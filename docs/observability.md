# Observabilidad sin ruido

## Logs técnicos

- Una petición correcta no genera un log técnico por petición. Los 4xx esperados
  se cuentan como métricas, sin WARN ni stack trace.
- Una petición lenta genera WARN `station_search_slow`. Se registra como máximo
  un aviso por endpoint y minuto; `station_search_slow_total` cuenta todas.
- Los errores inesperados generan ERROR `unhandled_request_error`, con causa,
  endpoint, método, requestId y el contexto de trazas disponible. El filtro evita
  duplicar ese error. Un fallo fuera del controlador también se registra.
- La finalización normal de importaciones queda en DEBUG; sus métricas continúan
  disponibles. Los reintentos agotados de RabbitMQ siguen siendo ERROR.

El encabezado `X-Request-Id` permite localizar una petición. Si se proporciona,
solo se aceptan caracteres seguros y un máximo de 100; en caso contrario se
crea un UUID. Los ProblemDetail de errores inesperados incluyen ese identificador.

La probabilidad de muestreo de trazas continúa siendo 0,1 por defecto: no todos
los traceId de los logs tendrán una traza almacenada en Tempo. Los contadores de
métricas y los eventos de localidades no se muestrean.

## Localidades consultadas

`LocalitySearchAnalytics` emite un evento de negocio INFO `locality_consulted`
cuando se ejecuta correctamente una consulta `LOCALITY` de la primera página.
Está separado por logger y tipo de evento de los avisos técnicos.

Se excluyen el autocompletado, las siguientes páginas del listado, las consultas
por radio/mapa, el histórico y las estadísticas usadas durante SSR. Un cambio de
combustible, una recarga o un reintento correcto puede contar como otra consulta.
No son usuarios únicos, sesiones ni todas las visitas al HTML cacheado. El tráfico
automatizado que consulte la API también puede contar.

Los eventos contienen país, región, provincia, localidad normalizada, combustible
y cantidad de resultados de esa página. Los nombres se limitan a 100 caracteres.
No se registra texto libre del autocompletado, IP, coordenadas ni cuerpos de
petición. Las localidades son campos de JSON para Loki, nunca etiquetas de
Prometheus ni etiquetas indexadas de streams de Loki.

`station_search_locality_consultations_total` solo distingue resultados vacíos/no
vacíos. El ranking se calcula desde los eventos conservados en Loki, agrupando
la jerarquía completa para distinguir localidades con el mismo nombre. Comienza
cuando se despliega esta versión y no reconstruye búsquedas anteriores.

## Métricas de diagnóstico

Los histogramas HTTP están habilitados para que los paneles p95/p99 funcionen.
El filtro incluye estaciones, autocompletado y estadísticas, con endpoints y
nombres de filtros acotados: parámetros arbitrarios no crean nuevas series.
No se guardan valores de filtros en esas etiquetas.

RabbitMQ publica `station_rabbit_last_failure_timestamp_seconds` además del
contador de reintentos agotados. Permite detectar el primer fallo aunque el
contador aparezca por primera vez con valor uno, sin una muestra previa a cero.

Los dashboards, reglas y despliegue coordinado se encuentran en `station-infra`.
