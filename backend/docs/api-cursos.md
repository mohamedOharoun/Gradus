# API de cursos — contrato para la app

Resumen de lo que implementa el backend (detalle completo en `especificacion-cursos-v1.md` §4; formato
del curso en `src/main/resources/schemas/course-v1.schema.json`). Base en desarrollo:
`http://localhost:7070`, también desde el emulador: `gradus.sh up` hace `adb reverse tcp:7070 tcp:7070`.
No se usa `10.0.2.2`: con `targetSdk` 37 (Android 17), la red local exige el permiso
`ACCESS_LOCAL_NETWORK` (ver `docs/tareas/registro-cuentas-reales.md`).

## Rutas

| Método y ruta | Sesión | Respuestas |
|---|---|---|
| `POST /api/courses/validate` | sí | 200 informe (válido o no) · 401 · 413 |
| `POST /api/courses` | sí | 201 publicado · 400 · 401 · 413 · 422 |
| `POST /api/courses/{id}/versions/validate` | sí | 200 informe (válido o no) · 401 · 403 · 404 · 409 · 413 |
| `POST /api/courses/{id}/versions` | sí | 201 · 400 · 401 · 403 · 404 · 409 · 413 · 422 |
| `GET /api/users/me/courses` | sí | 200 cursos propios · 401 |
| `GET /api/courses?q=&tag=&language=&author=&published=&page=&size=` | no | 200 página · 400 |
| `GET /api/courses/{id}` | no | 200 última versión con contenido · 404 |
| `GET /api/courses/{id}/versions/{version}` | no | 200 versión con contenido · 404 |

- Sesión: cabecera `Authorization: Bearer <token>` (la del inicio de sesión). Sin ella, 401 `UNAUTHENTICATED`.
- Publicar: el cuerpo es el JSON del curso **tal cual** (no va dentro de otro objeto), hasta 2 MiB. El
  autor es siempre el usuario de la sesión.
- Un `{id}` que no es un UUID, o una `{version}` que no es un entero positivo, da 404.

## Cuerpos

**Informe** (200 de validar curso o nueva versión; 400, 413 y 422 de publicar):

```json
{ "valid": false,
  "errors":   [ { "code": "UNKNOWN_PREREQUISITE", "path": "/modules/1/requires/0", "message": "El módulo «bfs» requiere «colas»…" } ],
  "warnings": [ { "code": "DUPLICATE_TAG", "path": "/tags/1", "message": "…" } ] }
```

`valid` es verdadero si y solo si `errors` está vacío. `path` es un JSON Pointer dentro del curso
(`""` = la raíz). Los mensajes están en español y pensados para el autor; la app puede mostrarlos tal cual.
En `POST /api/courses/{id}/versions/validate` se comprueba primero que el curso exista (404) y que el
usuario de la sesión sea su autor (403); no guarda nada. Si el curso es válido e idéntico a la última
versión devuelve 409 `IDENTICAL_TO_LATEST`; si es válido y distinto, incluye el aviso `REMOVED_IDS`
comparando contra la última versión.

**Metadatos de una versión** (cada elemento de la búsqueda y `course` del 201):

```json
{ "id": "0b8f…", "version": 2, "author": { "id": "5c1d…", "name": "Ana" },
  "publishedAt": "2026-11-18T08:35:00Z", "formatVersion": 1,
  "title": "Introducción a los grafos", "description": "…", "language": "es",
  "tags": ["grafos", "algoritmos"], "moduleCount": 2, "sha256": "9c1e…" }
```

**201 al publicar**: `{ "course": { …metadatos… }, "warnings": [ …como en el informe… ] }`.
Al publicar una versión nueva puede venir el aviso `REMOVED_IDS` (ids de módulos, lecciones o ejercicios
que ya no existen: el progreso de los alumnos sobre ellos se pierde).

**Versión con contenido** (los dos GET de un curso): los metadatos más `"content"`, que es el JSON del
autor **sin modificar**:

```json
{ "id": "…", "version": 1, …resto de metadatos…, "content": { "formatVersion": 1, "title": "…", "modules": [ … ] } }
```

Para comprobar la descarga: el SHA-256 de los bytes de `content`, tal como vienen en la respuesta, es
`sha256`. Si se parsea y se vuelve a serializar, ya no coincide.

**Página de la búsqueda**: `{ "items": [ …metadatos… ], "page": 0, "size": 20, "total": 37 }`.

**Cursos propios** (`GET /api/users/me/courses`): `{ "courses": [ …metadatos… ] }` — últimas versiones de
los cursos publicados por el usuario de la sesión, ordenados por `publishedAt` descendente (desempate por
`id`), sin paginación; lista vacía si no tiene cursos publicados.

**Otros errores** (como en cuentas y sesiones): `{ "errors": [ { "field": "…", "code": "…", "message": "…" } ] }`.

## Búsqueda

- Solo últimas versiones, de la más reciente a la más antigua (desempate por id).
- `q`: subcadena en título o descripción. Sin distinguir mayúsculas ni espacios repetidos; `%` y `_` son literales.
- `tag`: repetible (`?tag=grafos&tag=algoritmos`). El curso debe tener **todas** las etiquetas pedidas.
- `language`: exacto (`es`, `en`, `es-ES`).
- `author`: subcadena del nombre del autor. Sin distinguir mayúsculas ni espacios repetidos en lo pedido;
  **no** quita tildes («angel» no encuentra a «Ángel»); `%` y `_` son literales.
- `published`: `month` (último mes) o `year` (último año), desde la misma hora de hace un mes o un año, en el
  calendario y en UTC, con el límite incluido. «Cualquier fecha» = no enviar el parámetro. Otro valor (`week`,
  `Month`, `any`…): 400 `INVALID_PARAMETER` en `published`.
- `q`, `tag`, `language`, `author` y `published` ausentes o en blanco no filtran; los presentes se combinan con Y.
- `page` desde 0 (por defecto 0); `size` entre 1 y 100 (por defecto 20). Si no son válidos: 400
  `INVALID_PARAMETER` en `page` y/o `size`.

## Códigos de error nuevos (para `ApiErrorCode` de la app)

| `code` | Estado | `field` | Cuándo |
|---|---|---|---|
| `COURSE_NOT_FOUND` | 404 | `id` o `version` | No existe el curso o la versión |
| `NOT_COURSE_AUTHOR` | 403 | `id` | Publica o valida una versión quien no es el autor |
| `IDENTICAL_TO_LATEST` | 409 | `body` | Al publicar o validar una versión, el contenido es igual al de la última versión |
| `INVALID_PARAMETER` | 400 | `page`, `size` o `published` | Parámetro de búsqueda no válido (si hay varios, todos, en ese orden) |

Los códigos de los problemas del curso (`errors`/`warnings` del informe) están en §3.3 de la
especificación; la app no necesita traducirlos.

## Formato del curso (resumen)

`formatVersion` (1), `title`, `description`, `language`, `tags`, `defaults.mastery {questions, minCorrect}`
y `modules[]`. Cada módulo: `id`, `title`, `requires[]` (ids de módulos), `mastery` opcional, `steps[]`
(lecciones `lesson` y ejercicios) y `review[]` (solo ejercicios, la reserva de la prueba de dominio).
Ejercicios: `singleChoice` (`options`, `answer`), `multipleChoice` (`options`, `correct[]`) y `matching`
(`pairs[] {left, right}`), todos con `prompt` y `explanation`. Los textos largos son Markdown. Los ids de
módulos, lecciones y ejercicios son únicos en el curso y estables entre versiones. Ejemplo pequeño
publicable: ver las peticiones de Yaak «Cursos · Publicación».
