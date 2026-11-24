# Tarea — Progreso de los alumnos (backend, parte A)

Parte A del seguimiento del progreso. Índice común (decisiones, acuerdos, otras partes):
`docs/tareas/seguimiento-progreso.md`. Contrato con la app: `backend/docs/api-progreso.md`.

Rama: `dev/course-progress-tracking`. Proyecto: `backend/` (Maven, Java 25, Javalin, PostgreSQL en
Docker). Tests con JUnit Jupiter y repositorios en memoria: `./gradus.sh test` (`mvn test`) no necesita
la base de datos. Commits: `… (progress task N)`.

Estado: **tarea 1 hecha; tarea 2 partida en 2a y 2b, con la 2a hecha; toca la 2b** (2026-11-24). Lo
terminado y sus decisiones: `progreso-backend-historico.md` (no hace falta leerlo para continuar).

## Relevo

Lo que necesita quien coja la parte A (orquestador o revisor). **Se reescribe** al terminar cada
sesión; no se acumula.

- **Dónde estamos (2026-11-24):** tarea 1 hecha y revisada (653 tests en verde). El dominio y el caso de
  uso están en `domain/progress/` y `application/progress/`; faltan los adaptadores JDBC (tarea 2) y la
  ruta (tarea 3). El contrato sigue en **borrador**, con los puntos de la tarea 1 ya resueltos. La
  tarea 2 ya tiene sus decisiones y su plan (abajo, en la tarea 2); la 2a (migración 006 y guion de
  `psql`) está hecha y revisada; falta la 2b (adaptadores JDBC), que copia las dos fusiones del guion.
- **Al empezar:** leer `AGENTS.md`, el Relevo general y las Decisiones de
  `docs/tareas/seguimiento-progreso.md`, este documento y `backend/docs/api-progreso.md`. **No** hace
  falta el histórico, `backend/docs/especificacion-cursos-v1.md` (606 líneas) ni nada de la app.
- **Para la tarea 2 hay que saber:**
  - Los dos puertos que implementar: `application/progress/EnrollmentRepository` (`mergeAll`,
    `findAll`) y `CourseExistence` (`findExisting`). Su Javadoc es el contrato de cada método.
  - La semántica de `mergeAll` es la de `domain/progress/ProgressMerge` (unión, gana la fecha más
    antigua, nunca borra): el SQL debe hacer lo mismo de forma **atómica**. `SyncProgress` ya le pasa
    la lista sin repetidos y solo con cursos que existen; no lee antes de escribir.
  - **Permiso para `psql` (2026-11-24): solo en una base de prueba temporal** (`gradus_progress_check`,
    en el mismo contenedor), que se crea, se usa y se borra. La base de desarrollo no se toca. Aplicar
    la 006 a la base de desarrollo es otro permiso (tarea 3).
- **Riesgos:** la concurrencia de dos móviles que sincronizan a la vez (tarea 2: escribir en orden fijo
  y en una transacción); el contrato, que comparte la parte B (no se cambia tras congelarlo, en la
  tarea 3).
- **Qué mirar en la revisión de la tarea 2:** `LEAST` en los dos `ON CONFLICT`; inscripción antes que
  sus módulos y todo en una transacción (`Transactions`); escritura **ordenada** (evita *deadlocks*);
  cadena de borrado usuario → inscripción → módulo; que la migración sea la `006`; nada de leer →
  fusionar → guardar en Java.
- **Mejora pendiente (fuera de esta parte):** mover `AdjustableClock` (test, `application/course`, hoy
  `public`) a un paquete de test común.

## Objetivo

Guardar en PostgreSQL las inscripciones y los módulos dominados de cada usuario, y ofrecer
`POST /api/progress/sync` según el contrato.

## Lo que ya hay y sirve de modelo

Para la **tarea 2** (SQL y JDBC):

- `db/init/004_create_courses.sql` (estilo de las migraciones; `gradus.sh` las aplica por nombre y
  anota cada una en `schema_migrations`); `db/init/003_create_sessions.sql` (`ON DELETE CASCADE`).
- `gradus.sh`, función `psql_in_container` (cómo se llama a `psql` dentro del contenedor).
- `infrastructure/persistence/JdbcCourseRepository.java`, `Transactions.java`, `Timestamps.java`;
  `JdbcSessionRepository.java` (más corto).

Para la **tarea 3** (web):

- `web/RequireSession.java` + `web/AuthenticatedHandler.java` (rutas con sesión; el usuario llega en
  `AuthenticationResult.Authenticated`).
- `web/course/CoursePublicationController.java` y sus respuestas; `web/ErrorResponse.java`.
- `App.java` (*composition root*). Tests de la capa web: solo de los DTO (p. ej.
  `web/course/CourseResponsesTest.java`); la ruta se prueba con Yaak.

## Diseño vigente

- **Hecho (tarea 1):** `domain/progress/` (records, `ProgressValidation`, `ProgressMerge`) y
  `application/progress/` (`SyncProgress`, `SyncResult` y los puertos). Detalle en el histórico.
- **Comprobar que el curso existe:** puerto `CourseExistence`; en la tarea 2, JDBC con
  `SELECT id FROM courses WHERE id = ANY(?)`. La clave ajena `course_id REFERENCES courses (id)` se
  mantiene como red de seguridad. Los cursos nunca se borran: no hay carrera entre comprobar y guardar.
- **Para la tarea 3:** `SkippedEntry.moduleId` es un `Optional<String>`, que Jackson no serializa bien
  sin un módulo extra: el DTO de respuesta lo convierte en un campo que se omite cuando está vacío.
  `SyncProgress` se crea una sola vez en `App.java` (por eso lee el reloj en cada llamada).
- **Patrones a nombrar:** puerto/adaptador (repositorios), función pura (validar, fusionar y el orden de
  escritura), segregación de interfaces (`CourseExistence`), *Decorator* (`RequireSession`, ya existe),
  *Execute Around* (`Transactions`, ya existe).

## Tareas

1. [x] **Dominio y caso de uso `SyncProgress`** (1a dominio, 1b caso de uso). Detalle, criterios y
   decisiones en `progreso-backend-historico.md`.
2. [ ] **Migración `006_create_enrollments.sql`, `JdbcEnrollmentRepository` y `JdbcCourseExistence`**
   (2a SQL, 2b JDBC).

   Decisiones (2026-11-24, con los usuarios):
   - **`JdbcCourseExistence`**, clase propia en `infrastructure/persistence/`: no toca
     `JdbcCourseRepository` (segregación de interfaces).
   - **Orden de `findAll`:** el que propone el contrato (inscripciones por `enrolled_at, course_id`;
     módulos por `module_id`), con dos consultas en `inSnapshot`. La tarea 3 solo lo confirma.
   - **Escritura en orden fijo:** inscripciones por `course_id`; módulos por `(course_id, module_id)`.
     Dos móviles que sincronizan los mismos cursos en distinto orden podrían bloquearse mutuamente
     (*deadlock*) por los bloqueos de fila de `ON CONFLICT`; con el mismo orden, uno espera al otro.
   - **`mastered_modules` cuelga solo de su inscripción:** `(user_id, course_id) REFERENCES enrollments
     ON DELETE CASCADE`, sin clave ajena directa a `users`. Borrar un usuario borra sus inscripciones y
     estas, sus módulos. Las claves primarias sirven de índice (por usuario y para la cascada).
   - **SQL probado en una base temporal** `gradus_progress_check` (ver el Relevo). El guion se guarda en
     `backend/db/checks/` como documentación; se puede quitar si estorba.

   2a. [x] **Migración y guion de comprobación.**
   - `backend/db/init/006_create_enrollments.sql`, con el estilo de la 004 (comentarios, `IF NOT
     EXISTS`, restricciones con nombre):
     - `enrollments (user_id, course_id, enrolled_at)`, clave `(user_id, course_id)`;
       `user_id … REFERENCES users (id) ON DELETE CASCADE`; `course_id REFERENCES courses (id)` sin
       cascada (los cursos no se borran).
     - `mastered_modules (user_id, course_id, module_id, mastered_at)`, clave
       `(user_id, course_id, module_id)` y la clave ajena a su inscripción de arriba.
     - `module_id` como `TEXT` (el dominio no limita su longitud) con un `CHECK` de no vacío como red
       de seguridad.
   - `backend/db/checks/006_enrollments_check.sql`: las sentencias de fusión
     (`INSERT … ON CONFLICT … DO UPDATE SET … = LEAST(…)`, las mismas que usará la 2b) y las
     comprobaciones, cada una con un comentario de qué espera.
   - ✅ Criterio, en `gradus_progress_check` (crearla, aplicar 001–004 y 006, guion, `DROP DATABASE`):
     la 006 aplicada dos veces sin error; inserción repetida sin cambios; gana la fecha más antigua
     (inscripción y módulo) y una más moderna no la sobrescribe; módulo sin inscripción rechazado;
     borrar un usuario borra inscripciones y módulos; dos sesiones a la vez (la segunda espera a la
     primera y queda la fecha más antigua); la base de desarrollo, sin cambios.
   - Modelo y esfuerzo: **Opus, esfuerzo alto** (el riesgo de la tarea está aquí y un error de
     concurrencia no lo detecta ningún test). Revisión: a fondo.

   2b. [ ] **Adaptadores JDBC.**
   - `JdbcCourseExistence` (`SELECT id FROM courses WHERE id = ANY(?)`, array de UUID) y
     `JdbcEnrollmentRepository`: `mergeAll` en `inTransaction`, inscripción antes que sus módulos,
     en el orden fijo, con el SQL de la 2a; `findAll` en `inSnapshot`, fechas con `Timestamps`.
   - El orden de escritura, como función pura con su test (fichero nuevo).
   - ✅ Criterio: tests del orden (cursos desordenados salen por `course_id`; módulos por
     `(course_id, module_id)`; no depende del orden de entrada); `./gradus.sh test` en verde; sin avisos
     nuevos. El cableado en `App.java` es de la tarea 3.
   - Modelo y esfuerzo: **Sonnet, esfuerzo medio** (el SQL llega probado). Revisión: a fondo en la
     transacción, el orden y la conversión de tipos.
3. [ ] **Ruta `POST /api/progress/sync`.**
   - DTO de petición y respuesta, controlador con `requireSession.around`, cableado en `App.java`.
   - Resolver los «(abierto, tarea 3)» del contrato y **congelarlo** (cambiar su estado a «congelado»
     y avisarlo en el Relevo general).
   - Aplicar la 006 a la base de desarrollo (`gradus.sh`) **con permiso de los usuarios**.
   - ✅ Criterio: tests de los DTO (forma del JSON, fechas ISO, orden de la respuesta); con Yaak: 200,
     400 con cuerpo malformado, 401 sin token; una petición de Yaak nueva guardada en `yaak/`.
   - Modelo y esfuerzo sugeridos: **Sonnet, esfuerzo alto** (contrato compartido). Revisión: a fondo.

## Progreso

- 2026-11-24: documento creado desde la definición (`docs/tareas/seguimiento-progreso.md`).
- 2026-11-24: **tarea 1 hecha y revisada** (1a y 1b; 653 tests en verde). Detalle en el histórico.
- 2026-11-24: tarea 2 decidida con los usuarios (ver sus decisiones) y partida en 2a y 2b. Permiso para
  `psql` solo en la base temporal `gradus_progress_check`.
- 2026-11-24: **2a hecha y revisada.** `db/init/006_create_enrollments.sql` y
  `db/checks/006_enrollments_check.sql` (fusiones con `LEAST`, 15 comprobaciones con `ASSERT`, receta de
  concurrencia comentada). En `gradus_progress_check`: rojo sin la 006, 006 aplicada dos veces, guion
  en verde, dos sesiones (s2 espera con `Lock` y queda la fecha más antigua en inscripción y módulo),
  `DROP DATABASE`. Desarrollo sin cambios (mismo `md5` de `schema_migrations`; allí ya hay una `005`).

## Siguiente paso

Paso 2b (adaptadores JDBC), con un chat nuevo (Sonnet, esfuerzo medio): el mensaje lo prepara el
orquestador. Las sentencias de fusión se copian de `db/checks/006_enrollments_check.sql`.
