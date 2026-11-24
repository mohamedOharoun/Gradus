# Tarea — Progreso de los alumnos (backend, parte A)

Parte A del seguimiento del progreso. Índice común (decisiones, acuerdos, otras partes):
`docs/tareas/seguimiento-progreso.md`. Contrato con la app: `backend/docs/api-progreso.md`.

Rama: `dev/course-progress-tracking`. Proyecto: `backend/` (Maven, Java 25, Javalin, PostgreSQL en
Docker). Tests con JUnit Jupiter y repositorios en memoria: `./gradus.sh test` (`mvn test`) no necesita
la base de datos. Commits: `… (progress task N)`.

Estado: **tareas 1 y 2 hechas; toca la tarea 3** (2026-11-24). Lo terminado y sus decisiones:
`progreso-backend-historico.md` (no hace falta leerlo para continuar).

## Relevo

Lo que necesita quien coja la parte A (orquestador o revisor). **Se reescribe** al terminar cada
sesión; no se acumula.

- **Dónde estamos (2026-11-24):** tareas 1 y 2 hechas y revisadas (668 tests en verde). Hay dominio,
  caso de uso (`SyncProgress`), migración `006` y adaptadores JDBC (`JdbcEnrollmentRepository`,
  `JdbcCourseExistence`); falta la ruta (tarea 3). El contrato sigue en **borrador**.
- **Al empezar:** leer `AGENTS.md`, el Relevo general y las Decisiones de
  `docs/tareas/seguimiento-progreso.md`, este documento y `backend/docs/api-progreso.md`. **No** hace
  falta el histórico, `backend/docs/especificacion-cursos-v1.md` (606 líneas) ni nada de la app.
- **Para la tarea 3 hay que saber:**
  - Los adaptadores JDBC **no se han ejecutado nunca contra PostgreSQL** (el SQL sí, en la 2a; los
    tipos, `createArrayOf("uuid", …)` y los lotes, no). Las pruebas con Yaak son su primera prueba
    real: además del criterio, conviene mandar lo mismo dos veces, una fecha más antigua y un curso
    inexistente.
  - La `006` aún no está en la base de desarrollo, que ya tiene una `005_create_course_ratings.sql` de
    otra rama. Aplicarla (`./gradus.sh` la aplica al arrancar el servidor) necesita **permiso de los
    usuarios**; también cualquier escritura de Yaak (cuentas, cursos, progreso).
  - Cableado en `App.java`: `JdbcEnrollmentRepository` y `JdbcCourseExistence` reciben el `DataSource`;
    `SyncProgress` se crea una sola vez.
  - `findAll` ya devuelve el orden propuesto en el contrato (inscripciones por `enrolled_at, course_id`;
    módulos por `module_id`): la tarea 3 lo confirma al congelar el contrato.
- **Abierto para la tarea 3:** los tres «(abierto, tarea 3)» del contrato (límites por petición, orden
  de la respuesta, código de error nuevo).
- **Riesgos:** el contrato, que comparte la parte B (no se cambia tras congelarlo).
- **Qué mirar en la revisión de la tarea 3:** el usuario sale siempre de la sesión, nunca del cuerpo;
  400 solo para la forma del JSON (los problemas de valor van a `skipped`); `moduleId` se omite en
  `skipped` cuando está vacío; fechas ISO-8601 UTC; contrato congelado y avisado en el Relevo general.
- **Mejora pendiente (fuera de esta parte):** mover `AdjustableClock` (test, `application/course`, hoy
  `public`) a un paquete de test común.

## Objetivo

Guardar en PostgreSQL las inscripciones y los módulos dominados de cada usuario, y ofrecer
`POST /api/progress/sync` según el contrato.

## Lo que ya hay y sirve de modelo

Para la **tarea 3** (web):

- `web/RequireSession.java` + `web/AuthenticatedHandler.java` (rutas con sesión; el usuario llega en
  `AuthenticationResult.Authenticated`).
- `web/course/CoursePublicationController.java` y sus respuestas; `web/ErrorResponse.java`.
- `App.java` (*composition root*). Tests de la capa web: solo de los DTO (p. ej.
  `web/course/CourseResponsesTest.java`); la ruta se prueba con Yaak.

## Diseño vigente

- **Hecho (tarea 1):** `domain/progress/` (records, `ProgressValidation`, `ProgressMerge`) y
  `application/progress/` (`SyncProgress`, `SyncResult` y los puertos). Detalle en el histórico.
- **Hecho (tarea 2):** `db/init/006_create_enrollments.sql` (y su guion `db/checks/`) y, en
  `infrastructure/persistence/`, `JdbcEnrollmentRepository` (fusión en SQL con `LEAST`, una
  transacción, escritura en el orden de `EnrollmentWriteOrder`), `EnrollmentRows` (filas → dominio) y
  `JdbcCourseExistence`. Con la entrada vacía, los dos adaptadores responden sin ir a la base de datos.
- **Comprobar que el curso existe:** puerto `CourseExistence` (`SELECT id FROM courses WHERE id =
  ANY(?)`). La clave ajena `course_id REFERENCES courses (id)` se mantiene como red de seguridad. Los
  cursos nunca se borran: no hay carrera entre comprobar y guardar.
- **Para la tarea 3:** `SkippedEntry.moduleId` es un `Optional<String>`, que Jackson no serializa bien
  sin un módulo extra: el DTO de respuesta lo convierte en un campo que se omite cuando está vacío.
  `SyncProgress` se crea una sola vez en `App.java` (por eso lee el reloj en cada llamada).
- **Patrones a nombrar:** puerto/adaptador (repositorios), función pura (validar, fusionar, el orden de
  escritura y el montaje de filas), segregación de interfaces (`CourseExistence`), *Decorator*
  (`RequireSession`, ya existe), *Execute Around* (`Transactions`, ya existe).

## Tareas

1. [x] **Dominio y caso de uso `SyncProgress`** (1a dominio, 1b caso de uso). Detalle, criterios y
   decisiones en `progreso-backend-historico.md`.
2. [x] **Migración `006_create_enrollments.sql`, `JdbcEnrollmentRepository` y `JdbcCourseExistence`**
   (2a SQL, 2b JDBC).

   Decisiones (2026-11-24, con los usuarios):
   - **`JdbcCourseExistence`**, clase propia en `infrastructure/persistence/`: no toca
     `JdbcCourseRepository` (segregación de interfaces).
   - **Orden de `findAll`:** el que propone el contrato (inscripciones por `enrolled_at, course_id`;
     módulos por `module_id`), con dos consultas en `inSnapshot`. La tarea 3 solo lo confirma.
   - **Escritura en orden fijo:** inscripciones por `course_id`; módulos por `(course_id, module_id)`.
     Dos móviles que sincronizan los mismos cursos en distinto orden podrían bloquearse mutuamente
     (*deadlock*) por los bloqueos de fila de `ON CONFLICT`; con el mismo orden, uno espera al otro.
     Se escribe en dos lotes (todas las inscripciones, luego todos los módulos): el orden sigue siendo
     el mismo en todas las transacciones. El orden de Java (`UUID.compareTo`, con signo) no es el de
     PostgreSQL; no importa, porque todas las escrituras pasan por `EnrollmentWriteOrder`.
   - **`mastered_modules` cuelga solo de su inscripción:** `(user_id, course_id) REFERENCES enrollments
     ON DELETE CASCADE`, sin clave ajena directa a `users`. Borrar un usuario borra sus inscripciones y
     estas, sus módulos. Las claves primarias sirven de índice (por usuario y para la cascada).
   - **SQL probado en una base temporal** `gradus_progress_check`, que se crea y se borra; la base de
     desarrollo no se toca. El guion se guarda en `backend/db/checks/` como documentación; se puede
     quitar si estorba.

   2a. [x] **Migración y guion de comprobación** (Opus, esfuerzo alto). 15 comprobaciones con `ASSERT`
   en `gradus_progress_check` y la receta de dos sesiones a la vez, comentada al final del guion.

   2b. [x] **Adaptadores JDBC** (Sonnet, esfuerzo medio). `JdbcCourseExistence`,
   `JdbcEnrollmentRepository` y las funciones puras `EnrollmentWriteOrder` y `EnrollmentRows`, con sus
   tests; `NoConnectionDataSource` (ayuda de test) comprueba que la entrada vacía no pide conexión.
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
- 2026-11-24: **2a hecha y revisada.** En `gradus_progress_check`: rojo sin la 006, 006 aplicada dos
  veces, guion en verde, dos sesiones (s2 espera con `Lock` y queda la fecha más antigua en inscripción
  y módulo), `DROP DATABASE`. Desarrollo sin cambios (mismo `md5` de `schema_migrations`; allí ya hay
  una `005`).
- 2026-11-24: **2b hecha y revisada** (668 tests en verde, sin avisos; sin base de datos). **Tarea 2
  hecha.** El adaptador entero se prueba con Yaak en la tarea 3.

## Siguiente paso

Tarea 3 (ruta y contrato), con el orquestador: resolver con los usuarios los «(abierto, tarea 3)» del
contrato y el permiso para aplicar la `006` y escribir con Yaak en la base de desarrollo; decidir si se
parte en pasos y preparar el mensaje para quien implementa.
