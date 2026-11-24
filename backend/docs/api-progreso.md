# API de progreso — contrato entre el backend y la app

Estado: **borrador** (2026-11-24). Lo confirma el orquestador de la parte A (backend) y queda
**congelado** al cerrar su tarea 3. A partir de ahí, cualquier cambio sigue el protocolo del Relevo
general de `docs/tareas/seguimiento-progreso.md`. Las dudas marcadas con **(abierto)** se resuelven en
la tarea indicada y se anotan aquí.

Mismas convenciones que `api-cursos.md`: base `http://localhost:7070`, sesión con
`Authorization: Bearer <token>`, errores con `{ "errors": [ { "field", "code", "message" } ] }`.

## Ruta

| Método y ruta | Sesión | Respuestas |
|---|---|---|
| `POST /api/progress/sync` | sí | 200 estado completo · 400 `MALFORMED_BODY` · 401 `UNAUTHENTICATED` |

Una sola ruta para subir y bajar: el móvil manda lo que tiene pendiente y recibe **todo** el progreso
del usuario en el servidor. Mandar una lista vacía sirve para solo leer.

## Cuerpo de la petición

```json
{ "enrollments": [
    { "courseId": "0b8f6d2e-…", "enrolledAt": "2026-11-24T10:00:00Z",
      "mastered": [ { "moduleId": "bfs", "masteredAt": "2026-11-24T10:20:00Z" } ] } ] }
```

- `courseId`: UUID del curso. `moduleId`: id de módulo del curso (texto, como en el JSON del curso).
- Fechas en ISO-8601 UTC (`Instant`), las del reloj del móvil.
- Los módulos dominados van **dentro** de su inscripción: no existe un módulo dominado sin inscripción.
- El usuario es siempre el de la sesión; nunca se lee del cuerpo.

## Respuesta 200

```json
{ "enrollments": [
    { "courseId": "0b8f6d2e-…", "enrolledAt": "2026-11-24T10:00:00Z",
      "mastered": [ { "moduleId": "bfs", "masteredAt": "2026-11-24T10:20:00Z" } ] } ],
  "skipped": [ { "courseId": "9a1c…", "code": "COURSE_NOT_FOUND" },
               { "courseId": "0b8f6d2e-…", "moduleId": " ", "code": "REQUIRED" } ] }
```

- `enrollments`: **todo** el progreso del usuario tras fusionar (no solo lo enviado).
- `skipped`: entradas enviadas que no se guardaron, con el motivo. El móvil las quita de su cola de
  pendientes (no las reintenta). El resto de la petición sí se guarda.
  - Orden: primero lo omitido al validar (reglas 4 con UUID inválido y 7) y después los cursos
    inexistentes (regla 4); dentro de cada grupo, en el orden de la petición. La app no depende del
    orden; solo sirve para que la respuesta sea determinista.
  - `courseId`: en lo omitido al validar, tal como llegó (texto, aunque no sea un UUID). En los cursos
    inexistentes, en forma canónica (UUID en minúsculas), y **una sola vez** aunque la petición lo
    repita (la regla 9 ya los ha fusionado). La app usa los ids que le da el servidor, que ya van en
    minúsculas.
  - Sin `moduleId`: se omitió la **inscripción entera** con sus módulos. Con `moduleId`: se omitió
    **solo ese módulo**; la inscripción y sus otros módulos sí se guardaron.

## Reglas

1. **Fusión = unión.** Lo enviado se añade a lo que ya hay; nunca se borra nada.
2. **Idempotente:** mandar lo mismo dos veces deja el mismo estado.
3. **Gana la fecha más antigua** (`enrolledAt` y cada `masteredAt`): es cuándo pasó por primera vez,
   desde cualquier móvil.
4. **Curso inexistente o `courseId` que no es un UUID** → la inscripción entera va a `skipped` con
   `COURSE_NOT_FOUND`; no bloquea lo demás.
5. **El `moduleId` no se comprueba** contra el contenido del curso: los ids son estables entre
   versiones, y uno que el autor haya quitado (`REMOVED_IDS`) simplemente deja de contar en la app.
6. **400 `MALFORMED_BODY`** solo si el JSON no tiene la forma de arriba (falta `enrollments` o un
   campo de una entrada, tipos equivocados, fecha que no es ISO-8601…). Los problemas de valor de una
   entrada concreta (reglas 4 y 7) van a `skipped`.
7. **`moduleId` vacío o solo espacios** → ese módulo va a `skipped` con `REQUIRED` (y su `moduleId`);
   el resto de la inscripción se guarda.
8. **Fechas en el futuro** (reloj del móvil adelantado) se recortan a la hora del servidor **antes** de
   fusionar. No hay regla para un `masteredAt` anterior a `enrolledAt`: se guardan tal cual.
9. **Repetidos en la misma petición** (inscripción o módulo) se fusionan con la regla 3, sin error.

## Abierto (se resuelve en la tarea indicada de `tareas/progreso-backend.md`)

- **Resuelto en la tarea 1 (2026-11-24):** UUID inválido y `moduleId` vacío (reglas 4, 6 y 7, y la
  forma de `skipped`), fechas futuras (regla 8) y repetidos en la misma petición (regla 9).
- **(abierto, tarea 3)** Límites: el cuerpo ya está limitado a 2 MiB por Javalin. ¿Hace falta un
  máximo de inscripciones o módulos por petición?
- **(abierto, tarea 3)** Orden de la respuesta (para que sea determinista y fácil de probar):
  propuesta, inscripciones por `enrolledAt` y después `courseId`; módulos por `moduleId`.
- **(abierto, tarea 3)** ¿Hace falta un código de error nuevo en `ErrorCode`? En principio no
  (`MALFORMED_BODY` y `UNAUTHENTICATED` ya existen; `COURSE_NOT_FOUND`, también).
