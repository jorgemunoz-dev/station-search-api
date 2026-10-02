# GeoNames postal importer

The importer loads a GeoNames postal-code `.txt` or `.zip` into the canonical
`geographic_area` hierarchy and keeps `search_location` as its postal/spatial
projection. Run Liquibase before importing.

```bash
python3 scripts/import_geonames.py \
  --file ES.zip \
  --country ES \
  --country-name España \
  --database-url postgresql://user:password@localhost/database \
  --replace-country
```

`--replace-country` removes obsolete **GeoNames search rows** only after the new
file has been validated and imported. Canonical area IDs are never deleted or
recreated. Invalid source rows can be shown with `--log-rejections`.
