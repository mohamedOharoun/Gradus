# Tarea — Progreso de los alumnos (backend, parte A)

Parte A del seguimiento del progreso. Índice común (decisiones, acuerdos, otras partes):
`docs/tareas/seguimiento-progreso.md`. Contrato con la app: `backend/docs/api-progreso.md`.

Rama: `dev/course-progress-tracking`. Proyecto: `backend/` (Maven, Java 25, Javalin, PostgreSQL en
Docker). Tests con JUnit Jupiter y repositorios en memoria: `./gradus.sh test` (`mvn test`) no necesita
la base de datos. Commits: `… (progress task N)`.

Estado: **tarea 1 hecha; toca la tarea 2** (2026-11-24). Lo terminado y sus decisiones:
`progreso-backend-historico.md` (no hace falta leerlo para continuar).

## Relevo

Lo que necesita quien coja la parte A (orquestador o revisor). **Se reescribe** al terminar cada
sesión; no se acumula.

- **Dónde estamos (2026-11-24):** tarea 1 hecha y revisada (653 tests en verde). El dominio y el caso de
  uso están en `domain/progress/` y `application/progress/`; faltan los adaptadores JDBC (tarea 2) y la
  ruta (tarea 3). El contrato sigue en **borrador**, con los puntos de la tarea 1 ya resueltos.
- **Al empezar:** leer `AGENTS.md`, el Relevo general y las Decisiones de
  `docs/tareas/seguimiento-progreso.md`, este documento y `backend/docs/api-progreso.md`. **No** hace
  falta el histórico, `backend/docs/especificacion-cursos-v1.md` (606 líneas) ni nada de la app.
- **Para la tarea 2 hay que saber:**
  - Los dos puertos que implementar: `application/progress/EnrollmentRepository` (`mergeAll`,
    `findAll`) y `CourseExistence` (`findExisting`). Su Javadoc es el contrato de cada método.
  - La semántica de `mergeAll` es la de `domain/progress/ProgressMerge` (unión, gana la fecha más
    antigua, nunca borra): el SQL debe hacer lo mismo de forma **atómica**. `SyncProgress` ya le pasa
    la lista sin repetidos y solo con cursos que existen; no lee antes de escribir.
  - **Sin permiso no se escribe en la base de desarrollo**: el criterio con `psql` necesita el OK de
    los usuarios antes de ejecutarse.
- **Abierto para la tarea 2:** si `JdbcCourseExistence` es una clase propia o lo implementa
  `JdbcCourseRepository` (recomendación previa: clase propia, para no tocar el repositorio de cursos);
  el orden de `findAll` (lo fija la tarea 3, pero el SQL necesita alguno determinista).
- **Riesgos:** la concurrencia de dos móviles que sincronizan a la vez (tarea 2); el contrato, que
  comparte la parte B (no se cambia tras congelarlo, en la tarea 3).
- **Qué mirar en la revisión de la tarea 2:** `LEAST` en los dos `ON CONFLICT`; inscripción antes que
  sus módulos y todo en una transacción (`Transactions`); `ON DELETE CASCADE` en `user_id`; que la
  migración sea la `006`; nada de leer → fusionar → guardar en Java.
- **Mejora pendiente (fuera de esta parte):** mover `AdjustableClock` (test, `application/course`, hoy
  `public`) a un paquete de test común.

## Objetivo

Guardar en PostgreSQL las inscripciones y los módulos dominados de cada usuario, y ofrecer
`POST /api/progress/sync` según el contrato.

## Lo que ya hay y sirve de modelo

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

## Diseño vigente

- **Hecho (tarea 1):** `domain/progress/` (records, `ProgressValidation`, `ProgressMerge`) y
  `application/progress/` (`SyncProgress`, `SyncResult` y los puertos). Detalle en el histórico.
- **Comprobar que el curso existe:** puerto `CourseExistence`; en la tarea 2, JDBC con
  `SELECT id FROM courses WHERE id = ANY(?)`. La clave ajena `course_id REFERENCES courses (id)` se
  mantiene como red de seguridad. Los cursos nunca se borran: no hay carrera entre comprobar y guardar.
- **Para la tarea 3:** `SkippedEntry.moduleId` es un `Optional<String>`, que Jackson no serializa bien
  sin un módulo extra: el DTO de respuesta lo convierte en un campo que se omite cuando está vacío.
  `SyncProgress` se crea una sola vez en `App.java` (por eso lee el reloj en cada llamada).
- **Patrones a nombrar:** puerto/adaptador (repositorios), función pura (validar y fusionar),
  segregación de interfaces (`CourseExistence`), *Decorator* (`RequireSession`, ya existe).

## Tareas

1. [x] **Dominio y caso de uso `SyncProgress`** (1a dominio, 1b caso de uso). Detalle, criterios y
   decisiones en `progreso-backend-historico.md`.
2. [ ] **Migración `006_create_enrollments.sql`, `JdbcEnrollmentRepository` y el adaptador JDBC de
   `CourseExistence`.**
   - `enrollments (user_id, course_id, enrolled_at)` con clave `(user_id, course_id)`.
   - `mastered_modules (user_id, course_id, module_id, mastered_at)` con clave
     `(user_id, course_id, module_id)` y clave ajena a su inscripción.
   - `user_id … REFERENCES users (id) ON DELETE CASCADE` (como `sessions`); `course_id` sin cascada
     (los cursos no se borran).
   - Fusión en SQL con `INSERT … ON CONFLICT … DO UPDATE SET … = LEAST(…)`, en una transacción; la
     misma semántica que `ProgressMerge`.
   - `CourseExistence` con `SELECT id FROM courses WHERE id = ANY(?)`.
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
- 2026-11-24: **tarea 1 hecha y revisada** (1a y 1b; 653 tests en verde). Detalle en el histórico.

## Siguiente paso

Tarea 2, con un orquestador nuevo: resolver con los usuarios el «Abierto para la tarea 2» del Relevo
y el permiso para `psql`, decidir si se parte en pasos (por ejemplo, 2a migración y SQL probado a mano,
2b adaptadores JDBC) y preparar el mensaje para quien implementa.
