# Data import scripts

## GeoNames postal locations

`import_geonames.py` reads the 12-column GeoNames postal-code format from a
`.txt` file or directly from the country `.zip` file and imports it into
`search_location`.

Install the only runtime dependency and run the importer:

```bash
python -m pip install "psycopg[binary]>=3.1"

python scripts/import_geonames.py \
  --file ./ES.zip \
  --country ES \
  --database-url postgresql://postgres:postgres@localhost:5432/station_search \
  --replace-country
```

The generated UUID includes the country, postal code, normalized locality and
all available administrative levels. Consequently, equally named localities in
different administrative areas remain separate and repeated imports are
idempotent. `--replace-country` is recommended when refreshing a complete
country dataset because it also removes records that disappeared upstream.

