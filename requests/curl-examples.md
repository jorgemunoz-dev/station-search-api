# Ejemplos con cURL

```bash
export BASE_URL="http://localhost:8080"
export COUNTRY_CODE="ES"
export PRODUCT_TYPE="DIESEL_A"
```

## Buscar estaciones

```bash
curl --silent --show-error --get "${BASE_URL}/stations" \
  --header 'Accept: application/json' \
  --data-urlencode 'searchMode=RADIUS' \
  --data-urlencode 'lat=36.7213' \
  --data-urlencode 'lng=-4.4214' \
  --data-urlencode 'radiusMeters=10000' \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'sortBy=PRICE'
```

## Buscar una localidad y consultar sus estadísticas

`/locations/search` devuelve `normalizedLocalityName`, el valor que comparten `search_location` y
las estadísticas. No hay que obtener UUIDs ni alimentar otro catálogo.

### Ejemplo completo para Ardales

```bash
export LOCALITY="$({
  curl --silent --show-error --get "${BASE_URL}/locations/search" \
    --header 'Accept: application/json' \
    --data-urlencode "countryCode=${COUNTRY_CODE}" \
    --data-urlencode 'query=Ardales' \
    --data-urlencode 'limit=10'
} | jq --raw-output --exit-status \
    '.[] | select(.primaryText == "Ardales" or .primaryText == "ARDALES") | .normalizedLocalityName' |
    head -n 1)"

printf 'Localidad normalizada: %s\n' "${LOCALITY}"
```

Estadísticas actuales de Ardales:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/current" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "locality=${LOCALITY}"
```

Histórico de Ardales:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/history" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode "locality=${LOCALITY}" \
  --data-urlencode 'from=2026-09-01' \
  --data-urlencode 'to=2026-09-24'
```

También se puede enviar directamente `locality=Ardales`; el backend aplica la misma normalización.

## Estadísticas de un área administrativa

Los tres parámetros son posiciones neutrales por país. En España, si la fuente carga la provincia
en `admin_area_2`, las estadísticas actuales de Málaga se consultan así:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/current" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'adminArea2=Málaga'
```

Y su histórico así:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/history" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'adminArea2=Málaga' \
  --data-urlencode 'from=2026-09-01' \
  --data-urlencode 'to=2026-09-24'
```

Solo se puede seleccionar un scope cada vez: `locality`, `adminArea1`, `adminArea2` o `adminArea3`.

## Estadísticas del país completo

Omitir `locality` selecciona todo el país:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/current" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}"
```

## Ranking de localidades

Este endpoint siempre devuelve **una fila agregada por localidad**, no una fila por estación ni una
fila por área administrativa. Los parámetros `adminArea1`, `adminArea2` y `adminArea3` son filtros
acumulables sobre los metadatos de `search_location`; no convierten el área en la localidad buscada.

Todas las localidades del país:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/localities" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}"
```

Localidades filtradas por metadatos administrativos; para España `adminArea2` puede representar la
provincia, pero en otros países conserva el significado proporcionado por la fuente:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/localities" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'adminArea2=Málaga'
```

Por ejemplo, `adminArea2=Málaga&adminArea3=Málaga` puede devolver Torremolinos, Cuevas del Becerro
y La Cala del Moral si las tres localidades tienen esos valores administrativos en
`search_location`. Para consultar **la localidad exacta de Málaga**, se usa `locality` en el endpoint
de estadísticas actuales (o históricas), no `adminArea3`:

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/current" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'locality=Málaga'
```

El campo `stationCount` de esa respuesta es el número de estaciones incluidas en el agregado. El
objeto `cheapestStation` representa únicamente la estación más barata; no pretende listar todas las
estaciones de la localidad.

## Resumen y ahorro

```bash
curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/summary" \
  --header 'Accept: application/json' \
  --data-urlencode "countryCode=${COUNTRY_CODE}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'days=7'
```

```bash
export STATION_ID="00000000-0000-0000-0000-000000000000"

curl --silent --show-error --get "${BASE_URL}/statistics/fuel-prices/savings" \
  --header 'Accept: application/json' \
  --data-urlencode "stationId=${STATION_ID}" \
  --data-urlencode "productType=${PRODUCT_TYPE}" \
  --data-urlencode 'referencePrice=1.650' \
  --data-urlencode 'tankLiters=55'
```
