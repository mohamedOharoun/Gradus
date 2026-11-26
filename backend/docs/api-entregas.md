# API de entregas con foto — contrato para la app

Las entregas de los ejercicios `photo` (fase 1: entregar, listar las propias y descargar su foto; la
revisión por el profesor es la fase 2). Diseño y decisiones en `docs/tareas/ejercicio-foto.md` (raíz del
repositorio); el formato del ejercicio, en `especificacion-cursos-v1.md` §2.5. Base y sesión, como en
`api-cursos.md`: `http://localhost:7070` y `Authorization: Bearer <token>`.

## Rutas

| Método y ruta | Sesión | Respuestas |
|---|---|---|
| `POST /api/courses/{id}/exercises/{exerciseId}/submissions` | sí | 201 entrega · 400 · 401 · 404 · 413 |
| `GET /api/submissions/mine?courseId=…` | sí | 200 las propias en ese curso · 400 · 401 |
| `GET /api/submissions/{id}/photo` | sí | 200 `image/jpeg` · 401 · 403 · 404 |

- El alumno es siempre el de la sesión: nunca va en la petición ni en la respuesta.
- Un `{id}` que no es un UUID da 404, como en las rutas de cursos. En la consulta es distinto:
  `courseId` es un parámetro obligatorio, así que si falta o no es un UUID da 400.
- Cada envío es una entrega nueva (no se sustituye la anterior); cuenta la última.

## Entregar

`multipart/form-data` con dos campos:

| Campo | Tipo | Qué es |
|---|---|---|
| `version` | texto | La versión del curso que estudia el alumno (entero ≥ 1). En esa versión el ejercicio tiene que existir y ser `photo` (en `steps`, no en `review`). |
| `photo` | **fichero** | La foto, JPEG de hasta **2 000 000 bytes**. Tiene que ir como fichero (con `filename` en su `Content-Disposition`): si llega como texto, el servidor no la ve como foto. |

- El servidor decide si es un JPEG por sus primeros bytes (`FF D8 FF`), no por el nombre ni el
  `Content-Type`.
- Límite de la petición entera: **2 MiB**. Si su `Content-Length` lo pasa, 413 sin leerla (y el servidor
  cierra la conexión a mitad del envío: el cliente puede verlo como un fallo de red). Por eso la app
  comprueba el tamaño antes de enviar (`PhotoSubmission.MAX_BYTES`) y reduce la foto a 2000 px de lado.
- Sin `Content-Length` (envío *chunked*), al pasar el límite el servidor corta la conexión.

**201** (la entrega, también cada elemento de «mis entregas»):

```json
{ "id": "a1b2…", "courseId": "0b6f…", "courseVersion": 3, "exerciseId": "repr-foto",
  "submittedAt": "2026-11-25T10:00:00.123456Z", "status": "PENDING", "photoBytes": 80902 }
```

`submittedAt` en ISO-8601 (UTC, con microsegundos). En la fase 1, `status` es siempre `PENDING`.

## Mis entregas

**200**: `{"submissions": [ …entregas… ]}`, la más reciente primero (a igual fecha, por `id` de mayor a
menor). Sin entregas, lista vacía (no 404). Un curso que no existe también da lista vacía.

## Descargar la foto

**200** con los bytes tal cual se guardaron, `Content-Type: image/jpeg`, `Cache-Control: private, no-store`
y `X-Content-Type-Options: nosniff`. Solo la descarga quien la subió: otra cuenta recibe 403.

## Códigos de error nuevos (para `ApiErrorCode` de la app)

| `code` | Estado | `field` | Cuándo |
|---|---|---|---|
| `INVALID_PHOTO` | 400 | `photo` | No es un JPEG |
| `PHOTO_TOO_LARGE` | 413 | `photo` | La foto pasa de 2 000 000 bytes, o la petición de 2 MiB |
| `EXERCISE_NOT_FOUND` | 404 | `exerciseId` | En esa versión no hay un ejercicio `photo` con ese id en `steps` |
| `SUBMISSION_NOT_FOUND` | 404 | `id` | No existe la entrega |
| `NOT_SUBMISSION_OWNER` | 403 | `id` | La entrega es de otra cuenta |

Se reutilizan:

| `code` | Estado | `field` | Cuándo |
|---|---|---|---|
| `REQUIRED` | 400 | `photo`, `version` o `courseId` | Falta el campo (o `photo` no llegó como fichero) |
| `INVALID_PARAMETER` | 400 | `version` o `courseId` | La versión no es un entero positivo, o `courseId` no es un UUID |
| `COURSE_NOT_FOUND` | 404 | `id` | No existe el curso o esa versión |
| `MALFORMED_BODY` | 400 | `body` | Multipart roto, o un campo de texto de más de 200 000 bytes (límite de Jetty) |
| `UNAUTHENTICATED` | 401 | `authorization` | Falta el token, no existe o ha caducado |

## Probarlo

- Yaak, carpeta «Entregas» (14 peticiones; antes, una vez por ordenador, la variable `fixturesDir`: ver
  `backend/yaak/README.md`).
- Base de datos: tabla `submissions` (`db/init/005_create_submissions.sql`); la foto va en la propia
  tabla (`photo BYTEA`) y se borra con la cuenta (`ON DELETE CASCADE`).
