# Publicación del sitemap mediante Nginx

El backend genera el sitemap dinámicamente desde `search_location` y lo expone en
`GET /seo/sitemap.xml`. Cuando supera 50.000 URLs, esa respuesta se convierte en un índice cuyos
documentos están disponibles en `GET /seo/sitemap-N.xml`.

Para publicarlo en el dominio canónico sin modificar el frontend, añade estas ubicaciones al
`server` de `www.gasoamigos.es` (sustituye `station-search-api:8080` si el upstream tiene otro
nombre):

```nginx
location = /sitemap.xml {
    proxy_pass http://station-search-api:8080/seo/sitemap.xml;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location ~ ^/sitemap-([1-9][0-9]*)\.xml$ {
    proxy_pass http://station-search-api:8080/seo/sitemap-$1.xml;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Tras validar y recargar Nginx, comprueba `https://www.gasoamigos.es/sitemap.xml`. El TTL de la
caché en memoria y de la respuesta HTTP es de una hora por defecto y puede cambiarse con
`SITEMAP_CACHE_TTL` usando una duración ISO-8601 (por ejemplo, `PT30M`). Cada instancia mantiene su
propia caché y se actualizará al vencer el TTL.
