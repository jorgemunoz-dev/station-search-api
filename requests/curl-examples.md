# Ejemplos con cURL

Los ejemplos asumen que la API está disponible en `http://localhost:8080`. Define primero estas
variables para poder copiar el resto de comandos directamente en una terminal:

```bash
export BASE_URL="http://localhost:8080"
export COUNTRY_CODE="ES"
export PRODUCT_TYPE="DIESEL_A"
```

Se usa `--get` junto con `--data-urlencode` para que cURL construya y codifique correctamente los
query parameters.

## Estaciones

### Buscar estaciones alrededor de unas coordenadas

```bash
curl --silent --show-error --get "${BASE_URL}/stations" \
  --header 'Accept: application/json' \
  --data-urlencode 'searchMode=RADIUS' \
  --data-urlencode 'lat=36.7213' \
  --data-urlencode 'lng=-4.4214' \
  --data-urlencode 'radiusMeters=10000' \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'sortBy=PRICE' \
  --data-urlencode 'page=0' \
  --data-urlencode 'size=20'
```

### Buscar estaciones dentro del mapa visible

```bash
curl --silent --show-error --get "${BASE_URL}/stations" \
  --header 'Accept: application/json' \
  --data-urlencode 'searchMode=VIEWPORT' \
  --data-urlencode 'north=36.7601' \
  --data-urlencode 'south=36.6802' \
  --data-urlencode 'east=-4.3501' \
  --data-urlencode 'west=-4.4907' \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'sortBy=PRICE'
```

## Búsqueda de localidades

```bash
curl --silent --show-error --get "${BASE_URL}/locations/search" \
  --header 'Accept: application/json' \
  --data-urlencode 'query=Málaga' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode 'limit=10'
```

## Descubrir áreas administrativas

Los clientes deben descubrir primero los IDs y reutilizarlos en las consultas estadísticas. No es
necesario conocer de antemano si un país usa provincias, estados, regiones o cualquier otro tipo.

### Áreas de primer nivel del país

```bash
curl --silent --show-error --get "${BASE_URL}/administrative-areas" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}"
```

Si tienes `jq`, puedes guardar el ID de un área devuelta por la API:

```bash
export AREA_ID="$(
  curl --silent --show-error --get "${BASE_URL}/administrative-areas" \
    --data-urlencode "countryCode=${COUNTRY_CODE}" |
  jq -r '.[0].id'
)"
```

### Hijos directos de un área

```bash
curl --silent --show-error --get "${BASE_URL}/administrative-areas" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "parentAreaId=${AREA_ID}"
```

### Filtrar por un tipo administrativo propio del país

```bash
curl --silent --show-error --get "${BASE_URL}/administrative-areas" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode 'areaType=PROVINCE'
```

### Ejemplo completo: obtener el `areaId` de Ardales (Málaga)

La jerarquía se recorre por IDs: primero se obtiene Málaga entre las áreas de primer nivel y después
se busca Ardales entre sus hijas. Este ejemplo requiere `jq`:

```bash
export MALAGA_AREA_ID="$(
  curl --silent --show-error --get "${BASE_URL}/administrative-areas" \
    --data-urlencode "countryCode=${COUNTRY_CODE}" \
    --data-urlencode 'areaType=PROVINCE' |
  jq --raw-output --exit-status '.[] | select(.name == "Málaga" or .name == "MÁLAGA") | .id' |
  head -n 1
)"

export ARDALES_AREA_ID="$(
  curl --silent --show-error --get "${BASE_URL}/administrative-areas" \
    --data-urlencode "countryCode=${COUNTRY_CODE}" \
    --data-urlencode "parentAreaId=${MALAGA_AREA_ID}" \
    --data-urlencode 'areaType=MUNICIPALITY' |
  jq --raw-output --exit-status '.[] | select(.name == "Ardales" or .name == "ARDALES") | .id' |
  head -n 1
)"

printf 'Málaga: %s\nArdales: %s\n' "${MALAGA_AREA_ID}" "${ARDALES_AREA_ID}"
```

Con el ID descubierto, esta petición devuelve las estadísticas actuales de Ardales:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/current" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "areaId=${ARDALES_AREA_ID}"
```

Y esta devuelve su histórico para un intervalo de fechas:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/history" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "areaId=${ARDALES_AREA_ID}" \
  --data-urlencode 'from=2026-09-01' \
  --data-urlencode 'to=2026-09-24'
```

El área solo aparecerá cuando exista al menos una estación clasificada en Ardales dentro de un
snapshot calculado. El catálogo actual se construye a partir de las áreas presentes en estaciones;
no es todavía un padrón completo de todos los municipios del país.

## Estadísticas actuales

### Nacionales

Omitir `areaId` significa consultar el país completo:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/current" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}"
```

### Para un área administrativa

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/current" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "areaId=${AREA_ID}"
```

## Histórico de estadísticas

### Nacional

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/history" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'from=2026-09-01' \
  --data-urlencode 'to=2026-09-23'
```

### Para un área administrativa

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/history" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "areaId=${AREA_ID}" \
  --data-urlencode 'from=2026-09-01' \
  --data-urlencode 'to=2026-09-23'
```

## Ranking de áreas administrativas

### Ranking de todas las áreas de primer nivel

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/administrative-areas" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}"
```

### Ranking de los hijos de un área

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/administrative-areas" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "parentAreaId=${AREA_ID}"
```

### Ranking de hijos filtrado por tipo

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/administrative-areas" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "parentAreaId=${AREA_ID}" \
  --data-urlencode 'areaType=MUNICIPALITY'
```

## Resumen y ahorro

### Resumen de precios de los últimos días

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/summary" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'days=7'
```

### Ahorro teórico de una estación

Sustituye el UUID por el `stationId` obtenido de una búsqueda o respuesta estadística:

```bash
export STATION_ID="00000000-0000-0000-0000-000000000000"

curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/savings" \
  --header 'Accept: application/json' \
  --data-urlencode "stationId=${STATION_ID}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'referencePrice=1.650' \
  --data-urlencode 'tankLiters=55'
```
