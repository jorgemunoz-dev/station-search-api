# GeoNames importer

Install `psycopg` 3 and import a GeoNames 12-column postal file:

```bash
python -m pip install 'psycopg[binary]>=3.1'
./scripts/import_geonames.py ES.zip --dsn postgresql://postgres:postgres@localhost/station_search --country ES
```

`.txt` and single-member `.zip` files are accepted. `--batch-size` controls batches and
`--replace-country` atomically replaces one country. Validation, deletion and UUID-v5 UPSERTs run in
one transaction. Stable IDs include country, normalized postal/locality and all administrative levels.
