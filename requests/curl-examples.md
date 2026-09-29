# Ejemplos cURL

## Buscar estaciones por jerarquía administrativa

```bash
curl -G 'http://localhost:8080/stations' \
  --data-urlencode 'searchMode=LOCALITY' --data-urlencode 'countryCode=ES' \
  --data-urlencode 'adminArea1=Andalucia' --data-urlencode 'adminArea2=Málaga' \
  --data-urlencode 'adminArea3=Ardales' --data-urlencode 'productType=GASOLINE_95_E5' \
  --data-urlencode 'sortBy=PRICE' --data-urlencode 'page=0' --data-urlencode 'size=100'
```

`RADIUS` usa `lat`, `lng` y `radiusMeters`; `VIEWPORT` usa `north`, `south`, `east` y `west`.
Estos modos rechazan `countryCode` y cualquier `adminArea`.

## Estadísticas

País completo:

```bash
curl -G 'http://localhost:8080/statistics/fuel-prices/current' \
  --data-urlencode 'countryCode=ES' --data-urlencode 'productType=GASOLINE_95_E5'
```

Jerarquía completa e histórico:

```bash
curl -G 'http://localhost:8080/statistics/fuel-prices/history' \
  --data-urlencode 'countryCode=ES' --data-urlencode 'productType=GASOLINE_95_E5' \
  --data-urlencode 'adminArea1=Andalucia' --data-urlencode 'adminArea2=Málaga' \
  --data-urlencode 'adminArea3=Ardales' --data-urlencode 'from=2026-09-22' --data-urlencode 'to=2026-09-28'
```

Los niveles deben ser contiguos: país, nivel 1, niveles 1+2 o niveles 1+2+3.
