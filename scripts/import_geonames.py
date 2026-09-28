#!/usr/bin/env python3
"""Import a GeoNames postal-code dataset into ``search_location``."""

from __future__ import annotations

import argparse
import csv
import logging
import re
import sys
import unicodedata
import uuid
import zipfile
from contextlib import contextmanager
from dataclasses import asdict, dataclass
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Iterator, TextIO

import psycopg
from psycopg import Connection


LOGGER = logging.getLogger("geonames-importer")
SOURCE = "GEONAMES"
DEFAULT_BATCH_SIZE = 1_000
EXPECTED_COLUMN_COUNT = 12
UUID_NAMESPACE = uuid.UUID("68e38865-80b0-4cb1-bffe-08b25e06e499")

REQUIRED_COLUMNS = {
    "id",
    "country_code",
    "postal_code",
    "normalized_postal_code",
    "locality_name",
    "normalized_locality_name",
    "admin_area_1_name",
    "admin_area_1_code",
    "admin_area_2_name",
    "admin_area_2_code",
    "admin_area_3_name",
    "admin_area_3_code",
    "location",
    "accuracy",
    "source",
    "created_at",
    "updated_at",
}


@dataclass(frozen=True)
class SearchLocation:
    id: uuid.UUID
    country_code: str
    postal_code: str
    normalized_postal_code: str
    locality_name: str
    normalized_locality_name: str
    admin_area_1_name: str | None
    admin_area_1_code: str | None
    admin_area_2_name: str | None
    admin_area_2_code: str | None
    admin_area_3_name: str | None
    admin_area_3_code: str | None
    latitude: Decimal
    longitude: Decimal
    accuracy: int | None
    source: str = SOURCE


@dataclass
class ImportStatistics:
    total_rows: int = 0
    valid_rows: int = 0
    rejected_rows: int = 0


class InvalidGeoNamesRow(ValueError):
    """The GeoNames row cannot be imported."""


UPSERT_SQL = """
INSERT INTO search_location (
    id, country_code, postal_code, normalized_postal_code,
    locality_name, normalized_locality_name,
    admin_area_1_name, admin_area_1_code,
    admin_area_2_name, admin_area_2_code,
    admin_area_3_name, admin_area_3_code,
    location, accuracy, source
) VALUES (
    %(id)s, %(country_code)s, %(postal_code)s, %(normalized_postal_code)s,
    %(locality_name)s, %(normalized_locality_name)s,
    %(admin_area_1_name)s, %(admin_area_1_code)s,
    %(admin_area_2_name)s, %(admin_area_2_code)s,
    %(admin_area_3_name)s, %(admin_area_3_code)s,
    ST_SetSRID(ST_MakePoint(%(longitude)s, %(latitude)s), 4326)::geography,
    %(accuracy)s, %(source)s
)
ON CONFLICT (id) DO UPDATE SET
    country_code = EXCLUDED.country_code,
    postal_code = EXCLUDED.postal_code,
    normalized_postal_code = EXCLUDED.normalized_postal_code,
    locality_name = EXCLUDED.locality_name,
    normalized_locality_name = EXCLUDED.normalized_locality_name,
    admin_area_1_name = EXCLUDED.admin_area_1_name,
    admin_area_1_code = EXCLUDED.admin_area_1_code,
    admin_area_2_name = EXCLUDED.admin_area_2_name,
    admin_area_2_code = EXCLUDED.admin_area_2_code,
    admin_area_3_name = EXCLUDED.admin_area_3_name,
    admin_area_3_code = EXCLUDED.admin_area_3_code,
    location = EXCLUDED.location,
    accuracy = EXCLUDED.accuracy,
    source = EXCLUDED.source,
    updated_at = CURRENT_TIMESTAMP
"""

DELETE_COUNTRY_SQL = """
DELETE FROM search_location
WHERE country_code = %(country_code)s AND source = %(source)s
"""


def normalize_text(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value)
    unaccented = "".join(character for character in decomposed if not unicodedata.combining(character))
    cleaned = re.sub(r"[^\w]+", " ", unaccented.casefold(), flags=re.UNICODE)
    return re.sub(r"\s+", " ", cleaned).strip()


def normalize_postal_code(value: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", value.strip().upper())


def clean_optional(value: str) -> str | None:
    value = value.strip()
    return value or None


def parse_decimal(value: str, field: str, row_number: int) -> Decimal:
    try:
        return Decimal(value.strip())
    except (InvalidOperation, AttributeError) as error:
        raise InvalidGeoNamesRow(f"Fila {row_number}: {field} inválido: {value!r}") from error


def parse_accuracy(value: str, row_number: int) -> int | None:
    if not value.strip():
        return None
    try:
        accuracy = int(value)
    except ValueError as error:
        raise InvalidGeoNamesRow(f"Fila {row_number}: accuracy inválido: {value!r}") from error
    if accuracy < 0 or accuracy > 32767:
        raise InvalidGeoNamesRow(f"Fila {row_number}: accuracy fuera del rango smallint")
    return accuracy


def stable_id(location: SearchLocation) -> uuid.UUID:
    # Administrative fields are part of the identity: homonymous localities must
    # remain distinct even if a source assigns them the same postal code.
    identity = "|".join(
        value or ""
        for value in (
            SOURCE,
            location.country_code,
            location.normalized_postal_code,
            location.normalized_locality_name,
            location.admin_area_1_code or normalize_text(location.admin_area_1_name or ""),
            location.admin_area_2_code or normalize_text(location.admin_area_2_name or ""),
            location.admin_area_3_code or normalize_text(location.admin_area_3_name or ""),
        )
    )
    return uuid.uuid5(UUID_NAMESPACE, identity)


def parse_row(columns: list[str], country_code: str, row_number: int) -> SearchLocation:
    if len(columns) != EXPECTED_COLUMN_COUNT:
        raise InvalidGeoNamesRow(
            f"Fila {row_number}: se esperaban {EXPECTED_COLUMN_COUNT} columnas y llegaron {len(columns)}"
        )

    (row_country, postal_code, locality_name, admin1_name, admin1_code,
     admin2_name, admin2_code, admin3_name, admin3_code,
     latitude_raw, longitude_raw, accuracy_raw) = columns

    row_country = row_country.strip().upper()
    postal_code = postal_code.strip()
    locality_name = locality_name.strip()
    if row_country != country_code:
        raise InvalidGeoNamesRow(f"Fila {row_number}: país {row_country!r}, se esperaba {country_code!r}")
    if not postal_code or not locality_name:
        raise InvalidGeoNamesRow(f"Fila {row_number}: código postal o localidad vacío")

    normalized_postal = normalize_postal_code(postal_code)
    normalized_locality = normalize_text(locality_name)
    if not normalized_postal or not normalized_locality:
        raise InvalidGeoNamesRow(f"Fila {row_number}: código postal o localidad normalizado vacío")

    latitude = parse_decimal(latitude_raw, "latitude", row_number)
    longitude = parse_decimal(longitude_raw, "longitude", row_number)
    if not Decimal("-90") <= latitude <= Decimal("90"):
        raise InvalidGeoNamesRow(f"Fila {row_number}: latitud fuera de rango: {latitude}")
    if not Decimal("-180") <= longitude <= Decimal("180"):
        raise InvalidGeoNamesRow(f"Fila {row_number}: longitud fuera de rango: {longitude}")

    location = SearchLocation(
        id=uuid.UUID(int=0),
        country_code=row_country,
        postal_code=postal_code,
        normalized_postal_code=normalized_postal,
        locality_name=locality_name,
        normalized_locality_name=normalized_locality,
        admin_area_1_name=clean_optional(admin1_name),
        admin_area_1_code=clean_optional(admin1_code),
        admin_area_2_name=clean_optional(admin2_name),
        admin_area_2_code=clean_optional(admin2_code),
        admin_area_3_name=clean_optional(admin3_name),
        admin_area_3_code=clean_optional(admin3_code),
        latitude=latitude,
        longitude=longitude,
        accuracy=parse_accuracy(accuracy_raw, row_number),
    )
    return SearchLocation(**{**asdict(location), "id": stable_id(location)})


@contextmanager
def open_dataset(path: Path) -> Iterator[TextIO]:
    if path.suffix.lower() != ".zip":
        with path.open("r", encoding="utf-8", newline="") as dataset:
            yield dataset
        return

    with zipfile.ZipFile(path) as archive:
        candidates = sorted(
            name for name in archive.namelist()
            if name.lower().endswith(".txt") and "readme" not in name.lower()
        )
        if not candidates:
            raise ValueError(f"No se encontró ningún .txt de datos dentro de {path}")
        import io
        with archive.open(candidates[0]) as binary, io.TextIOWrapper(binary, encoding="utf-8", newline="") as dataset:
            yield dataset


def validate_schema(connection: Connection) -> None:
    with connection.cursor() as cursor:
        cursor.execute(
            """SELECT column_name FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'search_location'"""
        )
        columns = {row[0] for row in cursor.fetchall()}
    missing = REQUIRED_COLUMNS - columns
    if missing:
        raise RuntimeError(f"Esquema search_location incompatible; faltan: {', '.join(sorted(missing))}")


def import_locations(connection: Connection, path: Path, country: str, batch_size: int, log_rejections: bool) -> ImportStatistics:
    statistics = ImportStatistics()
    batch: list[dict[str, object]] = []
    with open_dataset(path) as dataset:
        for row_number, columns in enumerate(csv.reader(dataset, delimiter="\t", quoting=csv.QUOTE_NONE), 1):
            statistics.total_rows += 1
            try:
                batch.append(asdict(parse_row(columns, country, row_number)))
                statistics.valid_rows += 1
            except InvalidGeoNamesRow as error:
                statistics.rejected_rows += 1
                if log_rejections:
                    LOGGER.warning("%s", error)
            if len(batch) >= batch_size:
                with connection.cursor() as cursor:
                    cursor.executemany(UPSERT_SQL, batch)
                batch.clear()
        if batch:
            with connection.cursor() as cursor:
                cursor.executemany(UPSERT_SQL, batch)
    return statistics


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Importa códigos postales de GeoNames en search_location.")
    parser.add_argument("--file", required=True, type=Path)
    parser.add_argument("--country", required=True)
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument("--replace-country", action="store_true")
    parser.add_argument("--log-rejections", action="store_true")
    parser.add_argument("--verbose", action="store_true")
    return parser.parse_args()


def main() -> int:
    options = arguments()
    logging.basicConfig(
        level=logging.DEBUG if options.verbose else logging.INFO,
        format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
    )
    country = options.country.strip().upper()
    if len(country) != 2 or not country.isalpha():
        LOGGER.error("El país debe ser un código ISO alpha-2")
        return 1
    if not options.file.is_file() or options.file.suffix.lower() not in {".zip", ".txt"}:
        LOGGER.error("--file debe apuntar a un fichero .zip o .txt")
        return 1
    if options.batch_size <= 0:
        LOGGER.error("--batch-size debe ser mayor que cero")
        return 1

    try:
        with psycopg.connect(options.database_url, autocommit=False) as connection:
            validate_schema(connection)
            if options.replace_country:
                with connection.cursor() as cursor:
                    cursor.execute(DELETE_COUNTRY_SQL, {"country_code": country, "source": SOURCE})
                    LOGGER.info("Filas anteriores eliminadas: %s", cursor.rowcount)
            statistics = import_locations(
                connection, options.file, country, options.batch_size, options.log_rejections
            )
            connection.commit()
        LOGGER.info(
            "Importación completada: total=%s, válidas=%s, rechazadas=%s",
            statistics.total_rows, statistics.valid_rows, statistics.rejected_rows,
        )
        return 0
    except (psycopg.Error, zipfile.BadZipFile, OSError, RuntimeError, ValueError):
        LOGGER.exception("No se pudo completar la importación")
        return 1


if __name__ == "__main__":
    sys.exit(main())
