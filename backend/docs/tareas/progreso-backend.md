# Tarea — Progreso de los alumnos (backend, parte A)

Parte A del seguimiento del progreso. Índice común (decisiones, acuerdos, otras partes):
`docs/tareas/seguimiento-progreso.md`. Contrato con la app: `backend/docs/api-progreso.md`.

Rama: `dev/course-progress-tracking`. Proyecto: `backend/` (Maven, Java 25, Javalin, PostgreSQL en
Docker). Tests con JUnit Jupiter y repositorios en memoria: `./gradus.sh test` (`mvn test`) no necesita
la base de datos. Commits: `… (progress task N)`.

Estado: **tarea 1 en curso: 1a hecho, toca 1b** (2026-11-24).

## Relevo

Lo que necesita quien coja la parte A (orquestador o revisor). **Se reescribe** al terminar cada
sesión; no se acumula.

- **Dónde estamos (2026-11-24):** resueltos los «(abierto, tarea 1)» del contrato (reglas 4 y 6–9 de
  `api-progreso.md`) y cómo comprobar que un curso existe (puerto `CourseExistence`, ver «Ideas de
  diseño»). Tarea 1 partida en **1a** (dominio, **hecho y revisado**) y **1b** (caso de uso, sin
  empezar). Orden de `skipped` decidido: primero lo omitido al validar, después los cursos
  inexistentes, cada grupo en orden de petición (contrato, «Respuesta 200»). Mensaje de 1b entregado.
- **Al empezar:** leer `AGENTS.md`, el Relevo general y las Decisiones de
  `docs/tareas/seguimiento-progreso.md`, este documento y `backend/docs/api-progreso.md`. **No** hace
  falta `backend/docs/especificacion-cursos-v1.md` (606 líneas) ni nada de la app.
- **Riesgos:** la concurrencia de dos móviles que sincronizan a la vez (tarea 2: por eso el caso de
  uso delega la fusión en `mergeAll` y no hace leer → fusionar → guardar); el contrato, que comparte
  la parte B (no se cambia tras congelarlo).
- **Qué mirar en la revisión:** 1a, que validar y fusionar sean funciones puras sin estado ni
  infraestructura, records inmutables y un test por regla; 1b, que no haya leer → fusionar → guardar,
  que el `userId` llegue siempre por parámetro y que una lista vacía no escriba.

## Objetivo

Guardar en PostgreSQL las inscripciones y los módulos dominados de cada usuario, y ofrecer
`POST /api/progress/sync` según el contrato.

## Lo que ya hay y sirve de modelo

Para la **tarea 1** (dominio y caso de uso):

- `application/course/FindCourse.java` y `PublishCourse.java`: forma de un caso de uso (dependencias
  por constructor con `Objects.requireNonNull`, `Clock` inyectado, `switch` sobre resultados sellados).
- `application/course/CourseRepository.java`: forma de un puerto de salida.
- Tests: `src/test/.../application/course/InMemoryCourseRepository.java`, `FindCourseTest.java`,
  `AdjustableClock.java`; `src/test/.../application/TestUsers.java`.

Para la **tarea 2** (SQL y JDBC):

- `db/init/004_create_courses.sql` (estilo de las migraciones; `gradus.sh` las aplica por nombre y
  anota cada una en `schema_migrations`).
- `infrastructure/persistence/JdbcCourseRepository.java`, `Transactions.java`, `Timestamps.java`;
  `JdbcSessionRepository.java` (más corto).

Para la **tarea 3** (web):

- `web/RequireSession.java` + `web/AuthenticatedHandler.java` (rutas con sesión; el usuario llega en
  `AuthenticationResult.Authenticated`).
- `web/course/CoursePublicationController.java` y sus respuestas; `web/ErrorResponse.java`.
- `App.java` (*composition root*). Tests de la capa web: solo de los DTO (p. ej.
  `web/course/CourseResponsesTest.java`); la ruta se prueba con Yaak.

## Ideas de diseño (borrador; el orquestador lo afina con los usuarios)

- `domain/progress/`: `Enrollment(UUID courseId, Instant enrolledAt, …módulos dominados)` y
  `MasteredModule(String moduleId, Instant masteredAt)` (records inmutables). La **fusión** (unión,
  gana la fecha más antigua, repetidos dentro de la misma lista) es una **función pura** del dominio.
- `application/progress/`: caso de uso `SyncProgress` → `sync(UUID userId, List<Enrollment>)` devuelve
  el estado completo y lo omitido (`skipped`). Puerto `EnrollmentRepository` (por ejemplo,
  `mergeAll(userId, enrollments)` y `findAll(userId)`).
- **Comprobar que el curso existe (decidido 2026-11-24):** puerto propio
  `application/progress/CourseExistence` con `Set<UUID> findExisting(Set<UUID> courseIds)` (una sola
  consulta por petición). En la tarea 1, un *fake* de test; en la tarea 2, JDBC con
  `SELECT id FROM courses WHERE id = ANY(?)`. La clave ajena `course_id REFERENCES courses (id)` se
  mantiene como red de seguridad. No se toca `CourseRepository` (`findLatest` carga hasta 2 MiB). Los
  cursos nunca se borran, así que no hay carrera entre comprobar y guardar. Patrón: segregación de
  interfaces.
- **Validar antes de fusionar:** el dominio recibe la entrada en bruto (`courseId` como texto), la
  valida y recorta las fechas futuras con una función pura (`now` por parámetro), para que las reglas
  4 y 7–9 del contrato se prueben aquí y no en el DTO de la tarea 3.
- **Patrones a nombrar:** puerto/adaptador (repositorio), función pura (fusión), resultado sellado,
  *Decorator* (`RequireSession`, ya existe).

## Tareas

1. [ ] **Dominio y caso de uso `SyncProgress`.** Puntos abiertos resueltos y anotados en el contrato.
   - 1a. [x] **Dominio** (`domain/progress/`): records `Enrollment(UUID courseId, Instant enrolledAt,
     List<MasteredModule> mastered)` y `MasteredModule(String moduleId, Instant masteredAt)`
     (inmutables; `moduleId` vacío lanza excepción), entrada en bruto `EnrollmentDraft` (`courseId`
     como texto), `SkippedEntry(String courseId, String moduleId opcional, ErrorCode code)` y dos
     funciones puras: **validar** `(drafts, now)` → válidas con fechas recortadas + omitidas, y
     **fusionar** `(guardado, entrante)` → unión, gana la fecha más antigua (también quita repetidos).
     Los nombres exactos los propone quien implementa.
     - ✅ Criterio (tests mínimos). *Validar:* UUID inválido omite la inscripción entera con
       `COURSE_NOT_FOUND` aunque traiga módulos; `moduleId` vacío o solo espacios omite solo ese módulo
       con `REQUIRED` y conserva el resto; `enrolledAt` y `masteredAt` futuros se recortan a `now`;
       igual a `now` o pasada no cambia; lo omitido sale en el orden de la petición. *Fusionar:* unión
       de cursos distintos; unión de módulos del mismo curso; gana el `enrolledAt` más antiguo (en los
       dos sentidos); gana el `masteredAt` más antiguo; repetidos de una misma lista se fusionan;
       idempotente (`f(f(a,b),b) = f(a,b)`); nunca borra; entrante vacío devuelve lo guardado.
       *Records:* `moduleId` vacío lanza excepción; la lista no se puede modificar desde fuera.
     - Modelo y esfuerzo: **Sonnet, medio**. Revisión: normal.
   - 1b. [ ] **Caso de uso** (`application/progress/`): puertos `EnrollmentRepository`
     (`mergeAll(userId, enrollments)`, `findAll(userId)`) y `CourseExistence`; `SyncProgress` (puertos y
     `Clock` por constructor) con `sync(UUID userId, List<EnrollmentDraft>)` → `SyncResult(enrollments,
     skipped)`: validar → quitar repetidos → existencia (una consulta) → `mergeAll` → `findAll`. **No**
     lee → fusiona → guarda (lo hace el repositorio, atómico en la tarea 2). Tests con
     `InMemoryEnrollmentRepository` (usa la fusión de 1a) y un *fake* de `CourseExistence`.
     - ✅ Criterio (tests mínimos): devuelve **todo** lo guardado, no solo lo enviado; idempotente;
       lista vacía solo lee (ni `mergeAll` ni `findExisting`); curso inexistente va a `skipped` y lo
       demás se guarda; lo omitido por validar y por inexistencia se junta en `skipped`; dos usuarios
       no se ven; recorta fechas con el reloj inyectado; fusiona repetidos de la misma petición;
       consulta la existencia una sola vez con los ids distintos; no la consulta si no queda nada válido;
       no llama a `mergeAll` si no queda nada que guardar; orden de `skipped` (primero lo omitido al
       validar, después los cursos inexistentes, cada grupo en orden de petición).
     - Modelo y esfuerzo: **Sonnet, medio**. Revisión: normal.
2. [ ] **Migración `006_create_enrollments.sql` y `JdbcEnrollmentRepository`.**
   - `enrollments (user_id, course_id, enrolled_at)` con clave `(user_id, course_id)`.
   - `mastered_modules (user_id, course_id, module_id, mastered_at)` con clave
     `(user_id, course_id, module_id)` y clave ajena a su inscripción.
   - `user_id … REFERENCES users (id) ON DELETE CASCADE` (como `sessions`); `course_id` sin cascada
     (los cursos no se borran).
   - Fusión en SQL con `INSERT … ON CONFLICT … DO UPDATE SET … = LEAST(…)`, en una transacción.
   - ✅ Criterio: SQL probado a mano con `psql` **con permiso de los usuarios** (inserción repetida, fecha
     más antigua gana, borrar un usuario borra su progreso, dos sesiones de `psql` a la vez).
   - Modelo y esfuerzo sugeridos: **Sonnet, esfuerzo alto** (concurrencia y transacciones). Revisión:
     a fondo.
3. [ ] **Ruta `POST /api/progress/sync`.**
   - DTO de petición y respuesta, controlador con `requireSession.around`, cableado en `App.java`.
   - Resolver los «(abierto, tarea 3)» del contrato y **congelarlo** (cambiar su estado a «congelado»
     y avisarlo en el Relevo general).
   - ✅ Criterio: tests de los DTO (forma del JSON, fechas ISO, orden de la respuesta); con Yaak: 200,
     400 con cuerpo malformado, 401 sin token; una petición de Yaak nueva guardada en `yaak/`.
   - Modelo y esfuerzo sugeridos: **Sonnet, esfuerzo alto** (contrato compartido). Revisión: a fondo.

## Progreso

- 2026-11-24: documento creado desde la definición (`docs/tareas/seguimiento-progreso.md`).
- 2026-11-24: tarea 1 planificada con los usuarios: reglas 4 y 6–9 del contrato, puerto
  `CourseExistence`, pasos 1a y 1b. Mensaje de 1a entregado.
- 2026-11-24: **1a hecho y revisado** (624 tests en verde). Decisiones de 1a aprobadas por los usuarios:
  - Tipo extra `MasteredModuleDraft(moduleId, masteredAt)`: la entrada en bruto necesita su propio
    tipo porque `MasteredModule` no admite un `moduleId` vacío. `EnrollmentDraft` lo usa en su lista.
  - `SkippedEntry(String courseId, Optional<String> moduleId, ErrorCode code)`: `moduleId` vacío =
    inscripción entera omitida; con valor = solo ese módulo. Fábricas `SkippedEntry.enrollment(...)` y
    `SkippedEntry.module(...)`.
  - Funciones puras: `ProgressValidation.validate(drafts, now)` → `ValidatedProgress(valid, skipped)` y
    `ProgressMerge.merge(stored, incoming)`. `merge(List.of(), valid)` quita repetidos (lo usará 1b).
  - Orden de `merge`: primera aparición (guardado primero, luego lo nuevo; módulos igual). El orden de
    la respuesta HTTP lo decide la tarea 3.
  - `validate` no recorta espacios del `moduleId` válido: se guarda tal cual.
  - Decisión **fuera del plan**, aceptada en la revisión: un `courseId` solo es UUID si es el formato
    canónico de 36 caracteres (`UUID.fromString` acepta atajos como `1-1-1-1-1`; se rechazan con
    `COURSE_NOT_FOUND`; mayúsculas válidas). `validate` no quita repetidos: eso lo hace
    `merge(List.of(), valid)` en 1b.
- 2026-11-24: orden de `skipped` relajado con los usuarios (ver contrato); así `validate` de 1a no
  cambia. Mensaje de 1b entregado.
- Nota para la tarea 3 (revisor): `SkippedEntry.moduleId` es un `Optional<String>` y Jackson no lo
  serializa bien sin un módulo extra; el DTO de respuesta debe convertirlo en un campo que se omite
  cuando está vacío.

## Siguiente paso

Paso **1b**: quien implementa propone ficheros, forma y tests y espera el OK; después, el orquestador
revisa y marca 1b y la tarea 1.
