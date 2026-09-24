# Resumen de endpoints públicos

La API expone siete operaciones de consulta. `countryCode` utiliza el código ISO 3166-1 alpha-2
(por ejemplo, `ES`) y `productType` identifica el combustible, como `DIESEL_A`.

## Estaciones y localidades

### `GET /stations`

Busca estaciones para mostrarlas en un mapa o listado. Tiene dos modos excluyentes:

- `RADIUS`: recibe `lat`, `lng` y `radiusMeters`; permite ordenar por precio o distancia.
- `VIEWPORT`: recibe `north`, `south`, `east` y `west`; devuelve las estaciones visibles en el mapa.

Admite paginación y un filtro opcional por `productType`. Devuelve estaciones individuales; este es
el endpoint apropiado cuando se necesita una lista de estaciones, no un agregado estadístico.

### `GET /locations/search`

Busca localidades o códigos postales dentro del `countryCode` obligatorio. Se utiliza para el
autocompletado y para obtener coordenadas, `normalizedLocalityName` y el contexto
`adminArea1/2/3`. El `normalizedLocalityName` devuelto puede reutilizarse como `locality` en los
endpoints estadísticos.

## Estadísticas de combustible

### `GET /statistics/fuel-prices/current`

Devuelve el agregado más reciente de un combustible: media, mínimo, máximo, número de estaciones y
estaciones más barata y más cara. El alcance se selecciona así:

- sin `locality` ni `adminArea`: todo el país;
- `locality`: una ciudad o pueblo exactos dentro del país;
- exactamente uno de `adminArea1`, `adminArea2` o `adminArea3`: un área administrativa concreta.

`locality` y los tres parámetros administrativos son mutuamente excluyentes.

### `GET /statistics/fuel-prices/history`

Devuelve una serie diaria entre `from` y `to` para el mismo alcance admitido por `current`. Incluye
media, mínimo, máximo, número de estaciones y variaciones absolutas y porcentuales respecto a uno,
siete y treinta días antes.

### `GET /statistics/fuel-prices/localities`

Devuelve un ranking con **un agregado por localidad**. Puede limitarse con una combinación de
`adminArea1`, `adminArea2` y `adminArea3`; esos parámetros solamente filtran el ranking y no
identifican una localidad. Por ejemplo, `adminArea2=Málaga` devuelve las localidades clasificadas
dentro de ese valor administrativo. Para Málaga capital debe usarse `current?locality=Málaga`.

Cada elemento incluye `stationCount`, precios agregados, diferencia respecto a la media nacional,
posición en el ranking y la estación más barata de la localidad.

### `GET /statistics/fuel-prices/summary`

Devuelve un resumen nacional del producto para los últimos `days`: precio medio actual, precio
medio anterior, variación absoluta y porcentual, mínimo, máximo, estaciones analizadas y fecha de
actualización. Está pensado para tarjetas o indicadores generales, no para seleccionar localidades.

### `GET /statistics/fuel-prices/savings`

Calcula el ahorro teórico de repostar en una `stationId` frente a un `referencePrice`. Usa el precio
actual del `productType` y lo multiplica por `tankLiters` (55 litros por defecto). No modifica datos.

## Guía rápida de elección

| Necesidad | Endpoint |
| --- | --- |
| Ver estaciones individuales cercanas o visibles | `GET /stations` |
| Autocompletar una localidad o código postal | `GET /locations/search` |
| Estadística nacional actual | `GET /statistics/fuel-prices/current` sin scope opcional |
| Estadística actual de Málaga capital | `GET /statistics/fuel-prices/current?locality=Málaga` |
| Estadística actual de un área administrativa | `GET /statistics/fuel-prices/current?adminAreaN=...` |
| Evolución temporal de cualquiera de esos scopes | `GET /statistics/fuel-prices/history` |
| Comparar o clasificar localidades | `GET /statistics/fuel-prices/localities` |
| Mostrar un indicador nacional resumido | `GET /statistics/fuel-prices/summary` |
| Calcular ahorro para una estación y depósito | `GET /statistics/fuel-prices/savings` |

