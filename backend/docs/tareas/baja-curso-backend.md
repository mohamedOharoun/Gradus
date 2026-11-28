# Tarea — Baja de un curso (backend, parte A)

Parte A de la funcionalidad «Ficha del curso, inscripción y baja». Índice común (decisiones 1–8,
reparto en partes, acuerdos): `docs/tareas/ficha-e-inscripcion-curso.md`. Contrato con la app:
`backend/docs/api-progreso.md`.

Proyecto: `backend/` (Maven, Java 25, Javalin, PostgreSQL en Docker). Tests con JUnit Jupiter y
dobles en memoria (`./gradus.sh test` / `mvn test`, sin base de datos). Commits (los hacen los
usuarios): `… (course-detail task A<N>)`.

Estado: **A2 hecha y revisada — pendiente permiso de BD para A3** (2026-11-28).

## Relevo

Lo que necesita quien coja la parte A (orquestador o revisor). **Se reescribe** al terminar cada
sesión; no se acumula.

- **Dónde estamos (2026-11-28):** **A1 y A2 terminadas y revisadas** (968 tests en verde en
  `mvn test`, sin avisos ni dependencias nuevas). `CanonicalUuid` está compartido entre `web.course`
  y `web.progress`, `ProgressController.unenroll` está implementado y la ruta
  `DELETE /api/progress/enrollments/{courseId}` está registrada en `App.java` con `RequireSession`.
  Faltan **A3** (Yaak + comprobación real contra PostgreSQL) y **A4** (congelar `api-progreso.md` y
  pasar el relevo a Parte B).
- **Decisiones aprobadas para la Parte A:**
  - Ruta: `DELETE /api/progress/enrollments/{courseId}` con sesión (`RequireSession`).
  - `POST /api/progress/sync` no cambia (Decisión 3).
  - Respuestas: `204 No Content` (sin cuerpo, idempotente incluso si el usuario no estaba inscrito o
    el UUID no corresponde a un curso existente), `401 Unauthorized` (`UNAUTHENTICATED`, vía
    `RequireSession`) y **`404 Not Found`** (`field: "courseId"`, `code: "COURSE_NOT_FOUND"`) si
    `{courseId}` no es un UUID canónico (`8-4-4-4-12` hex), coherente con `web/course/PathIds` y sin
    añadir `ErrorCode` nuevos.
  - Sin migración SQL: `006_create_enrollments.sql` ya tiene `ON DELETE CASCADE` desde `enrollments`
    hacia `mastered_modules` y clave primaria `(user_id, course_id)`.
- **Siguiente:** lanzar al implementador de **A3** (nueva petición en `backend/yaak/workspace/` +
  batería `curl` contra PostgreSQL + verificación de los usuarios en Yaak).
- **Permiso aprobado para A3 (2026-11-28):** arrancar PostgreSQL local (`./gradus.sh db`) y el
  servidor en `localhost:7070` (parándolo al terminar), crear dos cuentas de prueba vía API
  (`unenroll-a@example.test` y `unenroll-b@example.test`) sobre cursos ya publicados, comprobar los
  escenarios con `curl` y `SELECT` de solo lectura (sin `UPDATE`/`DELETE` SQL manuales), y al
  terminar borrar ambas cuentas vía `DELETE /api/users/me` para dejar la base de desarrollo limpia.

## Objetivo

Añadir en el backend el endpoint autenticado `DELETE /api/progress/enrollments/{courseId}` que borra
la inscripción del usuario autenticado en un curso y, por `ON DELETE CASCADE`, todos sus módulos
dominados; cubrirlo con tests JUnit, añadir la petición en `backend/yaak/`, comprobarlo contra
PostgreSQL y congelar la ampliación en `backend/docs/api-progreso.md` para desbloquear la Parte B.

## Lo que ya hay y sirve de modelo

- `backend/db/init/006_create_enrollments.sql`: tablas `enrollments` y `mastered_modules` con
  `FOREIGN KEY (user_id, course_id) REFERENCES enrollments (user_id, course_id) ON DELETE CASCADE`.
- `application/progress/EnrollmentRepository.java` y `SyncProgress.java` (puerto y caso de uso).
- `infrastructure/persistence/JdbcEnrollmentRepository.java` y `Transactions.java` (*Execute Around*).
- `web/progress/ProgressController.java` y `web/RequireSession.java` (*Decorator*).
- `web/course/PathIds.java` (regex de UUID canónico y estilo de validación de parámetros de ruta).
- `App.java` (*composition root*).
- Tests de referencia: `application/progress/InMemoryEnrollmentRepository.java`,
  `application/progress/SyncProgressMergeTest.java`, `infrastructure/persistence/TransactionsTest.java`
  (dobles JDBC con `java.lang.reflect.Proxy`), `infrastructure/persistence/JdbcEmptyInputTest.java`,
  `web/course/PathIdsTest.java`.

## Diseño acordado

- **Puerto `EnrollmentRepository`:** añadir `void delete(UUID userId, UUID courseId);`. Actualizar el
  Javadoc para distinguir que `mergeAll` nunca borra y `delete` elimina una inscripción y sus módulos
  dominados.
- **Caso de uso `UnenrollCourse`** (`application/progress/`): recibe `EnrollmentRepository` por
  constructor, valida argumentos no nulos (`Objects.requireNonNull`) y delega en
  `enrollments.delete(userId, courseId)`.
- **Persistencia `JdbcEnrollmentRepository.delete`:** ejecuta
  `DELETE FROM enrollments WHERE user_id = ? AND course_id = ?` dentro de `transactions.inTransaction`.
  PostgreSQL borra `mastered_modules` automáticamente por `ON DELETE CASCADE` en la misma operación.
- **Web (`web/` y `web/progress/`):** helper común de UUID en `web/` reutilizado por `web.course` y
  `web.progress`; `ProgressController.unenroll(ctx, authenticated)` lee `ctx.pathParam("courseId")`,
  responde `404` con `ErrorResponse.of("courseId", ErrorCode.COURSE_NOT_FOUND, ...)` si no es UUID
  canónico, y si es válido invoca `unenrollCourse.unenroll(authenticated.user().id(), courseId)` y
  responde `204 No Content`.
- **Cableado (`App.java`):** instanciar `JdbcEnrollmentRepository` una sola vez, pasarlo a
  `SyncProgress` y `UnenrollCourse`, y registrar
  `config.routes.delete("/api/progress/enrollments/{courseId}", requireSession.around(progress::unenroll))`.
- **Patrones a nombrar:** Puertos y adaptadores (`EnrollmentRepository` / `JdbcEnrollmentRepository`),
  Caso de uso (`UnenrollCourse`), *Execute Around* (`Transactions`), *Decorator* (`RequireSession`).

## Tareas

La casilla `[x]` la marca el orquestador/revisor tras verificar el resultado.

1. [x] **A1 · Caso de uso `UnenrollCourse` y persistencia `EnrollmentRepository.delete`**
   - **Modelo y esfuerzo mínimos recomendados:** Claude Sonnet (esfuerzo bajo-medio) o Gemini Flash
     (razonamiento medio).
   - **Ficheros:**
     - `application/progress/EnrollmentRepository.java`
     - `application/progress/UnenrollCourse.java` (nuevo)
     - `infrastructure/persistence/JdbcEnrollmentRepository.java`
     - Test: `application/progress/InMemoryEnrollmentRepository.java`,
       `application/progress/UnenrollCourseTest.java` (nuevo) y test unitario de
       `JdbcEnrollmentRepository.delete` (sin BD, con `Proxy` como en `TransactionsTest`).
   - **Criterios de aceptación:**
     - TDD (rojo → verde → refactor) en `UnenrollCourseTest`.
     - Casos de prueba mínimos de `UnenrollCourse`:
       1. Rechaza `EnrollmentRepository` nulo en el constructor y `userId`/`courseId` nulos en `unenroll`.
       2. Dar de baja de un curso inscrito elimina la inscripción y todos sus módulos dominados
          (`findAll` ya no los devuelve).
       3. Idempotente: dar de baja un curso no inscrito (o repetir la baja dos veces) no falla y deja
          el resto intacto.
       4. Aislamiento por curso: no borra otras inscripciones del mismo usuario.
       5. Aislamiento por usuario: no borra la inscripción de otro usuario al mismo curso.
       6. Permite volver a sincronizar/inscribirse después de la baja (sin residuos previos).
     - Test unitario de `JdbcEnrollmentRepository.delete`: comprueba sin BD (vía `DataSource`/`Connection`
       simulados con `Proxy`) que ejecuta `DELETE FROM enrollments WHERE user_id = ? AND course_id = ?`
       con los dos `UUID` en orden dentro de una transacción (`setAutoCommit(false)` → `executeUpdate`
       → `commit` → `close`) y hace `rollback` si `SQLException`, y que valida `userId`/`courseId` no nulos.
     - `mvn test` en verde sin avisos nuevos; sin tocar PostgreSQL ni capas web.
   - **Commit sugerido:** `add: UnenrollCourse use case and enrollment deletion in repository (course-detail task A1)`.

2. [x] **A2 · Validación de UUID compartida, `ProgressController.unenroll` y cableado en `App.java`**
   - **Modelo y esfuerzo mínimos recomendados:** Claude Sonnet (esfuerzo medio) o Gemini Pro
     (razonamiento medio).
   - **Ficheros:**
     - Helper de UUID compartido en `web/` + ajuste mínimo en `web/course/PathIds.java` (permiso ya
       concedido).
     - `web/progress/ProgressController.java`
     - `App.java`
     - Tests JUnit del helper de UUID, `PathIdsTest` intacto y tests del controlador/ruta (204,
       idempotencia, 404 `COURSE_NOT_FOUND` con UUID inválido, 401 vía `RequireSession`, sin añadir
       dependencias).
   - **Criterios de aceptación:**
     - Controlador delgado (sin lógica de negocio) que toma siempre `authenticated.user().id()`.
     - `POST /api/progress/sync` intacto.
     - Validación estricta de UUID canónico (acepta mayúsculas/minúsculas; rechaza vacío, formato corto
       `1-2-3-4-5`, espacios, caracteres no hex).
     - `App.java` instancia `JdbcEnrollmentRepository` una sola vez y registra
       `DELETE /api/progress/enrollments/{courseId}` con `requireSession.around(...)`.
     - `mvn test` en verde sin avisos ni nuevas dependencias.
   - **Commit sugerido:** `add: DELETE /api/progress/enrollments/{courseId} route and controller (course-detail task A2)`.

3. [ ] **A3 · Petición en Yaak y comprobación real contra PostgreSQL**
   - **Modelo y esfuerzo mínimos recomendados:** Claude Sonnet (esfuerzo bajo-medio) o Gemini Flash
     (razonamiento medio).
   - **Ficheros:** nueva petición en `backend/yaak/workspace/` (carpeta de Progreso).
   - **Permiso previo requerido:** antes de escribir en la base de desarrollo, pedir confirmación a los
     usuarios sobre las cuentas/BD a usar (por ejemplo, cuentas de prueba vía API sin tocar cuentas
     reales).
   - **Criterios de aceptación:**
     - Comprobaciones reales con `curl` contra PostgreSQL:
       1. `401` sin token y con token inválido.
       2. `404` (`COURSE_NOT_FOUND`) con `{courseId}` no UUID (p. ej. `not-a-uuid`, `1-2-3-4-5`).
       3. `204` al dar de baja un curso inscrito con módulos dominados: desaparece de
          `POST /api/progress/sync` (`{"enrollments":[]}`) y de las tablas `enrollments` y
          `mastered_modules` (`ON DELETE CASCADE`, comprobado con `SELECT` de solo lectura).
       4. `204` idempotente al repetir el mismo `DELETE` y con un UUID válido no inscrito/inexistente.
       5. Aislamiento: otro curso del mismo usuario y la inscripción de otro usuario al mismo curso
          permanecen intactos.
     - Comprobado por los usuarios desde Yaak.
   - **Commit sugerido:** `add: Yaak request for course unenrollment (course-detail task A3)`.

4. [ ] **A4 · Actualizar y congelar `backend/docs/api-progreso.md` y pasar relevo a Parte B**
   - **Modelo y esfuerzo mínimos recomendados:** Orquestador A — Claude Sonnet (esfuerzo bajo) o Gemini
     Flash (razonamiento bajo).
   - **Ficheros:**
     - `backend/docs/api-progreso.md`
     - `backend/docs/tareas/baja-curso-backend.md`
     - `docs/tareas/ficha-e-inscripcion-curso.md` (actualizar **Relevo general**)
   - **Criterios de aceptación:**
     - Ampliación de `DELETE /api/progress/enrollments/{courseId}` documentada y marcada como congelada.
     - Todas las casillas de Parte A marcadas y Relevo general listo para el orquestador B.
   - **Commit sugerido:** `docs: freeze unenroll contract and close backend part (course-detail task A4)`.

## Progreso

- 2026-11-28: documento creado tras aprobar el plan, el `404 COURSE_NOT_FOUND` para `{courseId}` no
  UUID y el permiso para compartir la validación de UUID en `web/`.
- 2026-11-28: **A1 hecha y revisada.** Añadido `EnrollmentRepository.delete`, creado `UnenrollCourse`
  con TDD, implementado `JdbcEnrollmentRepository.delete` en transacción y cubierto con 14 tests
  nuevos (`UnenrollCourseTest` y `JdbcEnrollmentRepositoryDeleteTest`). 946 tests en verde en
  `mvn test`, sin avisos ni cambios fuera de alcance.
- 2026-11-28: **A2 hecha y revisada.** Creado `CanonicalUuid` en `web/` (delegado desde `PathIds`),
  añadido `ProgressController.unenroll` y registrada `DELETE /api/progress/enrollments/{courseId}` en
  `App.java` con `RequireSession`. 22 tests nuevos (`CanonicalUuidTest` y `ProgressControllerTest`),
  968 tests en verde en `mvn test`, sin dependencias nuevas ni avisos.

## Siguiente paso

Confirmar permisos para PostgreSQL local y ejecutar **A3** (petición en `backend/yaak/workspace/` y
comprobación real contra PostgreSQL).
