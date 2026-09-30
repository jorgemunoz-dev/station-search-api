# Resumen de endpoints públicos

La API expone siete operaciones de consulta. `countryCode` utiliza el código ISO 3166-1 alpha-2
(por ejemplo, `ES`) y `productType` identifica el combustible, como `DIESEL_A`.

## Estaciones y localidades

### `GET /stations`

Busca estaciones para mostrarlas en un mapa o listado. Tiene tres modos excluyentes:

- `RADIUS`: recibe `lat`, `lng` y `radiusMeters`; permite ordenar por precio o distancia.
- `VIEWPORT`: recibe `north`, `south`, `east` y `west`; devuelve las estaciones visibles en el mapa.
- `LOCALITY`: exige `countryCode`, `adminArea1`, `adminArea2` y `adminArea3`; compara nombres sin
  distinguir mayúsculas, tildes o separadores y solamente permite ordenar por precio.

Admite paginación y un filtro opcional por `productType`. Devuelve estaciones individuales; este es
el endpoint apropiado cuando se necesita una lista de estaciones, no un agregado estadístico.

En modo `LOCALITY`, la jerarquía también se resuelve desde `search_location` por país y código
postal normalizado cuando la estación todavía no contiene áreas administrativas.

### `GET /locations/search`

Busca localidades o códigos postales dentro del país obligatorio y devuelve coordenadas, nombres
de presentación, nombre normalizado y los tres niveles administrativos. Los clientes deben usar
estos niveles en la búsqueda exacta de estaciones y estadísticas.

## Estadísticas de combustible

### `GET /statistics/fuel-prices/current`

Devuelve el agregado más reciente. El alcance es el país sin áreas, `adminArea1`,
`adminArea1+adminArea2` o la jerarquía completa. Se rechazan huecos en la jerarquía.

### `GET /statistics/fuel-prices/history`

Devuelve una serie diaria entre `from` y `to` para el mismo alcance admitido por `current`. Incluye
media, mínimo, máximo, número de estaciones y variaciones absolutas y porcentuales respecto a uno,
siete y treinta días antes.

### `GET /statistics/fuel-prices/localities`

Devuelve un ranking con **un agregado por localidad**. Puede limitarse con una combinación de
`adminArea1`, `adminArea2` y `adminArea3`; esos parámetros solamente filtran el ranking y no
identifican una localidad. Por ejemplo, `adminArea2=Málaga` devuelve las localidades clasificadas
dentro de ese valor administrativo. Para una localidad exacta se usa su jerarquía completa en `current`.

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
| Ver las estaciones de una localidad exacta | `GET /stations?searchMode=LOCALITY&countryCode=...&adminArea1=...&adminArea2=...&adminArea3=...` |
| Autocompletar una localidad o código postal | `GET /locations/search` |
| Estadística nacional actual | `GET /statistics/fuel-prices/current` sin scope opcional |
| Estadística actual de Málaga capital | `GET /statistics/fuel-prices/current?adminArea1=Andalucia&adminArea2=Málaga&adminArea3=Málaga` |
| Estadística actual de un área administrativa | `GET /statistics/fuel-prices/current?adminArea1=...&adminArea2=...` |
| Evolución temporal de cualquiera de esos scopes | `GET /statistics/fuel-prices/history` |
| Comparar o clasificar localidades | `GET /statistics/fuel-prices/localities` |
| Mostrar un indicador nacional resumido | `GET /statistics/fuel-prices/summary` |
| Calcular ahorro para una estación y depósito | `GET /statistics/fuel-prices/savings` |
