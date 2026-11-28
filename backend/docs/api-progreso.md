# API de progreso — contrato entre el backend y la app

Estado: **congelado** (2026-11-24 para `POST /api/progress/sync`, 16 escenarios reales en PostgreSQL;
ampliado y congelado el **2026-11-28** con `DELETE /api/progress/enrollments/{courseId}`, 7 escenarios
reales en PostgreSQL en la Parte A de `docs/tareas/ficha-e-inscripcion-curso.md`). Cualquier cambio
posterior se propone primero a los usuarios y se avisa en el Relevo general del índice de tarea
correspondiente antes de aplicarlo.

Mismas convenciones que `api-cursos.md`: base `http://localhost:7070`, sesión con
`Authorization: Bearer <token>`, errores con `{ "errors": [ { "field", "code", "message" } ] }`.

## Rutas

| Método y ruta | Sesión | Respuestas |
|---|---|---|
| `POST /api/progress/sync` | sí | 200 estado completo · 400 `MALFORMED_BODY` · 401 `UNAUTHENTICATED` · 413 cuerpo > 2 MiB |
| `DELETE /api/progress/enrollments/{courseId}` | sí | 204 baja confirmada (sin cuerpo) · 401 `UNAUTHENTICATED` · 404 `COURSE_NOT_FOUND` |

### Sincronización (`POST /api/progress/sync`)

Una sola ruta para subir y bajar: el móvil manda lo que tiene pendiente y recibe **todo** el progreso
del usuario en el servidor. Mandar `{ "enrollments": [] }` sirve para solo leer.

- **400:** `{ "errors": [ { "field": "body", "code": "MALFORMED_BODY", "message": … } ] }`. Indica un
  error de la app (el cuerpo no tiene la forma de abajo); no se guarda nada.
- **401:** formato de errores con `field` `authorization`, y la cabecera `WWW-Authenticate: Bearer`.
- **413:** lo responde Javalin con `text/plain` y el cuerpo `Content Too Large`, **no** con el formato de
  errores JSON. No debería ocurrir: el límite es de 2 MiB y una cuenta muy activa (unos 2 000 módulos)
  ocupa unos 130 KB. No hay otro límite de inscripciones ni de módulos por petición.
- La app trata cualquier estado que no sea 200 ni 401 (400, 413, 5xx) como fallo del servidor.

## Cuerpo de la petición

```json
{ "enrollments": [
    { "courseId": "0b8f6d2e-…", "enrolledAt": "2026-11-24T10:00:00Z",
      "mastered": [ { "moduleId": "bfs", "masteredAt": "2026-11-24T10:20:00Z" } ] } ] }
```

- `courseId`: UUID del curso. `moduleId`: id de módulo del curso (texto, como en el JSON del curso).
- **Todos los campos son obligatorios**, también `mastered` (puede ser `[]`). Los módulos dominados van
  **dentro** de su inscripción: no existe un módulo dominado sin inscripción.
- **Fechas** (`enrolledAt`, `masteredAt`): texto ISO-8601, las del reloj del móvil.
  - La app manda UTC con `Z` (`2026-11-24T10:00:00Z`). Se acepta también un desfase (`+01:00`), que se
    convierte al instante UTC equivalente.
  - Año entre 0001 y 9999, contado en UTC. Sin espacios alrededor.
  - Se admite fracción de segundo; se guarda con precisión de **microsegundos**, redondeando
    (`.123456789Z` vuelve como `.123457Z`).
- **Campos desconocidos se ignoran** en todos los niveles. El usuario es siempre el de la sesión: un
  `userId` en el cuerpo no se lee.
- **Codificación:** UTF-8 válido y **sin BOM**.

## Respuesta 200

```json
{ "enrollments": [
    { "courseId": "0b8f6d2e-…", "enrolledAt": "2026-11-24T10:00:00Z",
      "mastered": [ { "moduleId": "bfs", "masteredAt": "2026-11-24T10:20:00Z" } ] } ],
  "skipped": [ { "courseId": "9a1c…", "code": "COURSE_NOT_FOUND" },
               { "courseId": "0b8f6d2e-…", "moduleId": " ", "code": "REQUIRED" } ] }
```

- `enrollments`: **todo** el progreso del usuario tras fusionar (no solo lo enviado).
  - `courseId` en forma canónica (UUID en minúsculas).
  - Fechas en ISO-8601 UTC con `Z`: sin fracción si es cero; si la hay, con 3 o 6 decimales
    (`.500Z`, `.123457Z`). La app debe aceptar los tres formatos.
  - **Orden:** inscripciones por `enrolledAt` y después `courseId`; módulos por `moduleId`. Es el orden
    de PostgreSQL: `courseId` se compara como bytes sin signo (no coincide con `UUID.compareTo` de
    Java) y `moduleId` según la *collation* de la base de datos. Es determinista para una misma base,
    pero **la app no depende del orden**.
- `skipped`: entradas enviadas que no se guardaron, con el motivo. El móvil las quita de su cola de
  pendientes (no las reintenta). El resto de la petición sí se guarda.
  - Orden: primero lo omitido al validar (reglas 4 con UUID inválido y 7) y después los cursos
    inexistentes (regla 4); dentro de cada grupo, en el orden de la petición. La app no depende del
    orden; solo sirve para que la respuesta sea determinista.
  - `courseId`: en lo omitido al validar, tal como llegó (texto, aunque no sea un UUID). En los cursos
    inexistentes, en forma canónica (UUID en minúsculas), y **una sola vez** aunque la petición lo
    repita (la regla 9 ya los ha fusionado). La app usa los ids que le da el servidor, que ya van en
    minúsculas.
  - Sin `moduleId` (la clave no aparece, no vale `null`): se omitió la **inscripción entera** con sus
    módulos. Con `moduleId` (tal como llegó): se omitió **solo ese módulo**; la inscripción y sus otros
    módulos sí se guardaron.

## Reglas

1. **Fusión = unión.** Lo enviado se añade a lo que ya hay; nunca se borra nada.
2. **Idempotente:** mandar lo mismo dos veces deja el mismo estado (y la misma respuesta).
3. **Gana la fecha más antigua** (`enrolledAt` y cada `masteredAt`): es cuándo pasó por primera vez,
   desde cualquier móvil.
4. **Curso inexistente o `courseId` que no es un UUID** → la inscripción entera va a `skipped` con
   `COURSE_NOT_FOUND`; no bloquea lo demás.
5. **El `moduleId` no se comprueba** contra el contenido del curso: los ids son estables entre
   versiones, y uno que el autor haya quitado (`REMOVED_IDS`) simplemente deja de contar en la app.
6. **400 `MALFORMED_BODY`** solo si el cuerpo no tiene la forma de arriba: cuerpo vacío o JSON roto;
   texto sobrante tras el JSON; una clave repetida en cualquier objeto (también en un campo ignorado);
   falta `enrollments` o un campo de una entrada, o vale `null`; tipos equivocados (un número donde va
   texto, un objeto donde va una lista…); una fecha que no cumple lo de «Cuerpo de la petición»;
   UTF-8 inválido o con BOM. Los problemas de valor de una entrada concreta (reglas 4 y 7) van a
   `skipped`.
7. **`moduleId` vacío o solo espacios** → ese módulo va a `skipped` con `REQUIRED` (y su `moduleId`);
   el resto de la inscripción se guarda.
8. **Fechas en el futuro** (reloj del móvil adelantado) se recortan a la hora del servidor **antes** de
   fusionar. No hay regla para un `masteredAt` anterior a `enrolledAt`: se guardan tal cual.
9. **Repetidos en la misma petición** (inscripción o módulo) se fusionan con la regla 3, sin error.

## Baja de un curso (`DELETE /api/progress/enrollments/{courseId}`)

Ruta independiente para darse de baja explícitamente de un curso; `POST /api/progress/sync` mantiene
intacta su semántica de unión sin borrados.

- **Petición:** cabecera obligatoria `Authorization: Bearer <token>`. Sin cuerpo (si se enviara
  alguno, el servidor no lo lee). `{courseId}` debe ser un UUID en forma canónica
  (`8-4-4-4-12` dígitos hexadecimales, mayúsculas o minúsculas; formas abreviadas como `1-2-3-4-5`
  se rechazan).
- **`204 No Content`** (cuerpo vacío): la inscripción del usuario autenticado en `{courseId}` y
  todos sus módulos dominados (`mastered_modules`, borrados en cascada por PostgreSQL) han quedado
  eliminados.
- **`401 Unauthorized`:** mismo formato de errores que `sync` (`field: "authorization"`,
  `code: "UNAUTHENTICATED"` y cabecera `WWW-Authenticate: Bearer`).
- **`404 Not Found`:** `{ "errors": [ { "field": "courseId", "code": "COURSE_NOT_FOUND", "message": … } ] }`
  cuando `{courseId}` **no es un UUID canónico**.
- La app trata cualquier estado que no sea `204` ni `401` (`404`, `5xx`, …) como fallo del servidor.

### Reglas de la baja

1. **Idempotente:** si el usuario ya no estaba inscrito en `{courseId}` —o si el UUID válido no
   corresponde a ningún curso existente— responde igualmente `204 No Content` y no modifica nada.
2. **Aislamiento estricto por usuario y curso:** el usuario es siempre el de la sesión. Solo borra la
   fila `(user_id, course_id)` de ese usuario y ese curso; nunca afecta a otros cursos del mismo
   usuario ni a las inscripciones de otros usuarios en ese curso.
3. **Alcance del borrado:** borra únicamente `enrollments` y, por `ON DELETE CASCADE`, sus
   `mastered_modules`. No borra entregas con foto (`submissions`), cursos ni usuarios.
4. **Reinscripción limpia:** tras la baja, volver a sincronizar ese curso con
   `POST /api/progress/sync` empieza desde cero, sin módulos dominados ni fechas anteriores.

## Decisiones (antes «abierto»)

- **Tarea 1 (2026-11-24):** UUID inválido y `moduleId` vacío (reglas 4, 6 y 7, y la forma de
  `skipped`), fechas futuras (regla 8) y repetidos en la misma petición (regla 9).
- **Tarea 3 (2026-11-24):**
  - Límites: ninguno propio; basta el de 2 MiB (413). Un máximo que rechazara la petición entera
    bloquearía la cola de pendientes de la app.
  - Orden de la respuesta: el de «Respuesta 200»; la app no depende de él.
  - Sin códigos de error nuevos: `MALFORMED_BODY`, `UNAUTHENTICATED`, `COURSE_NOT_FOUND` y `REQUIRED`
    ya existían.
  - Lectura estricta del cuerpo (regla 6): campos desconocidos ignorados, `mastered` obligatorio, año
    0001–9999, clave repetida y BOM rechazados.
- **Parte A · Baja de curso (2026-11-28):**
  - `POST /api/progress/sync` no cambia; la baja se añade como `DELETE /api/progress/enrollments/{courseId}`
    en línea (ver `docs/tareas/ficha-e-inscripcion-curso.md`, decisiones 2, 3 y 5).
  - `{courseId}` fuera del formato UUID canónico (`8-4-4-4-12` hex) responde `404` con
    `field: "courseId"` y `code: "COURSE_NOT_FOUND"` (coherente con las rutas de cursos y sin
    códigos de error nuevos).
  - Un UUID canónico válido no inscrito o de un curso inexistente responde `204` (idempotente).
