#!/usr/bin/env python3
"""Import GeoNames postal data into the canonical geographic catalogue."""
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
AREA_TYPES = ("COUNTRY", "ADMIN_AREA_1", "ADMIN_AREA_2", "ADMIN_AREA_3", "LOCALITY")
REQUIRED_COLUMNS = {
    "geographic_area": {"id", "country_code", "type", "name", "normalized_name", "parent_id", "source", "source_code"},
    "search_location": {"id", "country_code", "postal_code", "normalized_postal_code", "locality_id", "location", "accuracy", "source"},
}

@dataclass(frozen=True)
class GeoNamesPostalRow:
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

@dataclass
class ImportStatistics:
    total_rows: int = 0
    valid_rows: int = 0
    rejected_rows: int = 0
    areas_created: int = 0

class InvalidGeoNamesRow(ValueError):
    """The GeoNames row cannot be imported."""

UPSERT_LOCATION_SQL = """
INSERT INTO search_location (
    id, country_code, postal_code, normalized_postal_code, locality_id,
    location, accuracy, source
) VALUES (
    %(id)s, %(country_code)s, %(postal_code)s, %(normalized_postal_code)s,
    %(locality_id)s,
    ST_SetSRID(ST_MakePoint(%(longitude)s, %(latitude)s), 4326)::geography,
    %(accuracy)s, %(source)s
)
ON CONFLICT (id) DO UPDATE SET
    country_code=EXCLUDED.country_code, postal_code=EXCLUDED.postal_code,
    normalized_postal_code=EXCLUDED.normalized_postal_code,
    locality_id=EXCLUDED.locality_id, location=EXCLUDED.location,
    accuracy=EXCLUDED.accuracy, source=EXCLUDED.source,
    updated_at=CURRENT_TIMESTAMP
"""

def normalize_text(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value)
    unaccented = "".join(c for c in decomposed if not unicodedata.combining(c))
    cleaned = re.sub(r"[\W_]+", " ", unaccented.casefold(), flags=re.UNICODE)
    return re.sub(r"\s+", " ", cleaned).strip()

def normalize_postal_code(value: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", value.strip().upper())

def clean_optional(value: str) -> str | None:
    return value.strip() or None

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
    if not 0 <= accuracy <= 10:
        raise InvalidGeoNamesRow(f"Fila {row_number}: accuracy fuera del rango GeoNames 0..10")
    return accuracy

def stable_id(location: GeoNamesPostalRow) -> uuid.UUID:
    identity = "|".join(value or "" for value in (
        SOURCE, location.country_code, location.normalized_postal_code,
        location.normalized_locality_name,
        location.admin_area_1_code or normalize_text(location.admin_area_1_name or ""),
        location.admin_area_2_code or normalize_text(location.admin_area_2_name or ""),
        location.admin_area_3_code or normalize_text(location.admin_area_3_name or ""),
    ))
    return uuid.uuid5(UUID_NAMESPACE, identity)

def parse_row(columns: list[str], country_code: str, row_number: int) -> GeoNamesPostalRow:
    if len(columns) != EXPECTED_COLUMN_COUNT:
        raise InvalidGeoNamesRow(f"Fila {row_number}: se esperaban 12 columnas y llegaron {len(columns)}")
    (row_country, postal_code, locality_name, a1_name, a1_code, a2_name,
     a2_code, a3_name, a3_code, latitude_raw, longitude_raw, accuracy_raw) = columns
    row_country, postal_code, locality_name = row_country.strip().upper(), postal_code.strip(), locality_name.strip()
    if row_country != country_code:
        raise InvalidGeoNamesRow(f"Fila {row_number}: país {row_country!r}, se esperaba {country_code!r}")
    normalized_postal, normalized_locality = normalize_postal_code(postal_code), normalize_text(locality_name)
    if not normalized_postal or not normalized_locality:
        raise InvalidGeoNamesRow(f"Fila {row_number}: código postal o localidad vacío")
    latitude, longitude = parse_decimal(latitude_raw, "latitude", row_number), parse_decimal(longitude_raw, "longitude", row_number)
    if not Decimal("-90") <= latitude <= Decimal("90") or not Decimal("-180") <= longitude <= Decimal("180"):
        raise InvalidGeoNamesRow(f"Fila {row_number}: coordenadas fuera de rango")
    row = GeoNamesPostalRow(uuid.UUID(int=0), row_country, postal_code, normalized_postal,
        locality_name, normalized_locality, clean_optional(a1_name), clean_optional(a1_code),
        clean_optional(a2_name), clean_optional(a2_code), clean_optional(a3_name),
        clean_optional(a3_code), latitude, longitude, parse_accuracy(accuracy_raw, row_number))
    return GeoNamesPostalRow(**{**asdict(row), "id": stable_id(row)})

@contextmanager
def open_dataset(path: Path) -> Iterator[TextIO]:
    if path.suffix.lower() != ".zip":
        with path.open("r", encoding="utf-8", newline="") as dataset:
            yield dataset
        return
    with zipfile.ZipFile(path) as archive:
        candidates = [name for name in archive.namelist() if name.lower().endswith(".txt") and "readme" not in name.lower()]
        if len(candidates) != 1:
            raise ValueError(f"Se esperaba un único .txt de datos dentro de {path}; encontrados: {len(candidates)}")
        import io
        with archive.open(candidates[0]) as binary, io.TextIOWrapper(binary, encoding="utf-8", newline="") as dataset:
            yield dataset

def validate_schema(connection: Connection) -> None:
    with connection.cursor() as cursor:
        for table, required in REQUIRED_COLUMNS.items():
            cursor.execute("SELECT column_name FROM information_schema.columns WHERE table_schema=current_schema() AND table_name=%s", (table,))
            missing = required - {row[0] for row in cursor.fetchall()}
            if missing:
                raise RuntimeError(f"Esquema {table} incompatible; faltan: {', '.join(sorted(missing))}")

def resolve_area(cursor, cache: dict, country: str, area_type: str, name: str, parent_id: int | None,
                 source: str, source_code: str | None, statistics: ImportStatistics) -> int:
    normalized = normalize_text(name)
    key = (country, area_type, parent_id, source_code, normalized)
    if key in cache:
        return cache[key]
    if source_code:
        cursor.execute("""SELECT id, normalized_name FROM geographic_area
            WHERE source=%s AND country_code=%s AND type=%s AND parent_id IS NOT DISTINCT FROM %s AND source_code=%s""",
            (source, country, area_type, parent_id, source_code))
        found = cursor.fetchone()
        if found:
            cursor.execute("UPDATE geographic_area SET name=%s, normalized_name=%s, active=true, updated_at=CURRENT_TIMESTAMP WHERE id=%s",
                           (name, normalized, found[0]))
            cache[key] = found[0]
            return found[0]
    cursor.execute("""SELECT id FROM geographic_area WHERE country_code=%s AND type=%s
        AND parent_id IS NOT DISTINCT FROM %s AND normalized_name=%s""", (country, area_type, parent_id, normalized))
    found = cursor.fetchone()
    if found:
        if source_code:
            cursor.execute("UPDATE geographic_area SET source_code=COALESCE(source_code,%s), active=true, updated_at=CURRENT_TIMESTAMP WHERE id=%s", (source_code, found[0]))
        cache[key] = found[0]
        return found[0]
    cursor.execute("""INSERT INTO geographic_area(country_code,type,name,normalized_name,parent_id,source,source_code)
        VALUES(%s,%s,%s,%s,%s,%s,%s) RETURNING id""", (country, area_type, name, normalized, parent_id, source, source_code))
    area_id = cursor.fetchone()[0]
    statistics.areas_created += 1
    cache[key] = area_id
    return area_id

def import_locations(connection: Connection, path: Path, country: str, country_name: str,
                     batch_size: int, log_rejections: bool, replace_country: bool) -> ImportStatistics:
    statistics, cache, batch, imported_ids = ImportStatistics(), {}, [], []
    with connection.cursor() as cursor:
        country_id = resolve_area(cursor, cache, country, "COUNTRY", country_name, None, "ISO_3166", country, statistics)
        with open_dataset(path) as dataset:
            for row_number, columns in enumerate(csv.reader(dataset, delimiter="\t", quoting=csv.QUOTE_NONE), 1):
                statistics.total_rows += 1
                try:
                    row = parse_row(columns, country, row_number)
                    parent = country_id
                    for area_type, name, code in (
                        ("ADMIN_AREA_1", row.admin_area_1_name, row.admin_area_1_code),
                        ("ADMIN_AREA_2", row.admin_area_2_name, row.admin_area_2_code),
                        ("ADMIN_AREA_3", row.admin_area_3_name, row.admin_area_3_code),
                    ):
                        if not name:
                            continue
                        parent = resolve_area(cursor, cache, country, area_type, name, parent, SOURCE, code, statistics)
                    locality_id = resolve_area(cursor, cache, country, "LOCALITY", row.locality_name, parent, SOURCE, None, statistics)
                    values = asdict(row) | {"locality_id": locality_id, "source": SOURCE}
                    batch.append(values)
                    imported_ids.append(row.id)
                    statistics.valid_rows += 1
                except InvalidGeoNamesRow as error:
                    statistics.rejected_rows += 1
                    if log_rejections:
                        LOGGER.warning("%s", error)
                if len(batch) >= batch_size:
                    cursor.executemany(UPSERT_LOCATION_SQL, batch)
                    batch.clear()
            if batch:
                cursor.executemany(UPSERT_LOCATION_SQL, batch)
        if replace_country:
            cursor.execute("CREATE TEMP TABLE imported_search_location(id uuid PRIMARY KEY) ON COMMIT DROP")
            cursor.executemany("INSERT INTO imported_search_location(id) VALUES(%s)", [(value,) for value in imported_ids])
            cursor.execute("""DELETE FROM search_location sl WHERE sl.country_code=%s AND sl.source=%s
                AND NOT EXISTS (SELECT 1 FROM imported_search_location imported WHERE imported.id=sl.id)""", (country, SOURCE))
            LOGGER.info("Filas GeoNames obsoletas eliminadas: %s", cursor.rowcount)
    return statistics

def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Importa GeoNames en geographic_area y search_location.")
    parser.add_argument("--file", required=True, type=Path)
    parser.add_argument("--country", required=True)
    parser.add_argument("--country-name", required=True)
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument("--replace-country", action="store_true")
    parser.add_argument("--log-rejections", action="store_true")
    parser.add_argument("--verbose", action="store_true")
    return parser.parse_args()

def main() -> int:
    options = arguments()
    logging.basicConfig(level=logging.DEBUG if options.verbose else logging.INFO,
                        format="%(asctime)s | %(levelname)s | %(name)s | %(message)s")
    country = options.country.strip().upper()
    if not re.fullmatch(r"[A-Z]{2}", country) or not options.country_name.strip() or options.batch_size <= 0:
        LOGGER.error("País, nombre de país o batch size inválidos")
        return 1
    if not options.file.is_file() or options.file.suffix.lower() not in {".zip", ".txt"}:
        LOGGER.error("--file debe apuntar a un fichero .zip o .txt")
        return 1
    try:
        with psycopg.connect(options.database_url, autocommit=False) as connection:
            validate_schema(connection)
            statistics = import_locations(connection, options.file, country, options.country_name.strip(),
                                          options.batch_size, options.log_rejections, options.replace_country)
            connection.commit()
        LOGGER.info("Importación completada: total=%s, válidas=%s, rechazadas=%s, áreas creadas=%s",
                    statistics.total_rows, statistics.valid_rows, statistics.rejected_rows, statistics.areas_created)
        return 0
    except (psycopg.Error, zipfile.BadZipFile, OSError, RuntimeError, ValueError):
        LOGGER.exception("No se pudo completar la importación")
        return 1

if __name__ == "__main__":
    sys.exit(main())
