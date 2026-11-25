# Tarea — Progreso de los alumnos (backend, parte A)

Parte A del seguimiento del progreso. Índice común (decisiones, acuerdos, otras partes):
`docs/tareas/seguimiento-progreso.md`. Contrato con la app: `backend/docs/api-progreso.md`.

Rama: `dev/course-progress-tracking`. Proyecto: `backend/` (Maven, Java 25, Javalin, PostgreSQL en
Docker). Tests con JUnit Jupiter y repositorios en memoria: `./gradus.sh test` (`mvn test`) no necesita
la base de datos. Commits: `… (progress task N)`.

Estado: **parte A terminada (tareas 1–3); contrato congelado** (2026-11-25). Solo falta que los usuarios
repitan 200, 400 y 401 en Yaak para marcar la casilla de la tarea 3. Lo terminado y sus decisiones:
`progreso-backend-historico.md` (no hace falta leerlo para continuar).

## Relevo

Lo que necesita quien coja la parte A (orquestador o revisor). **Se reescribe** al terminar cada
sesión; no se acumula.

- **Dónde estamos (2026-11-25):** parte A terminada. `POST /api/progress/sync` está en `App.java` y
  funciona contra PostgreSQL: dominio y caso de uso (`SyncProgress`), migración `006`, adaptadores
  JDBC, lector estricto y DTO (`web/progress/`) y `ProgressController`. 764 tests en verde. Los 16
  escenarios reales de 3b dieron lo esperado y los adaptadores JDBC no fallaron. El contrato
  (`backend/docs/api-progreso.md`) está **congelado** y avisado en el Relevo general.
- **Pendiente de los usuarios:** repetir en Yaak «Sincronizar progreso (200)», el 400 (quitando
  `enrollments`) y el 401 (quitando `Authorization`). Escribe una inscripción y un módulo en
  `autora.cursos@example.com`. Con eso, el revisor marca la casilla de la tarea 3.
- **Base de desarrollo:** tiene la `006` (`schema_migrations` de la 001 a la 006; la 005 es de
  valoraciones, de otra rama). Datos de prueba de 3b, que se dejan a propósito: `progress-a@example.test`
  (2 inscripciones, 313 módulos) y `progress-b@example.test` (2 inscripciones, 4 módulos), sin
  sesiones abiertas. Para deshacer la 006, la receta de «Permisos de 3b» (solo si lo piden los usuarios).
- **Si alguien retoma la parte A:** solo para un cambio de contrato (protocolo del Relevo general) o un
  fallo que encuentre la parte B o la C. Leer `AGENTS.md`, el Relevo general, este Relevo, la tarea 3 y
  el contrato. **No** hace falta el histórico, `backend/docs/especificacion-cursos-v1.md` ni nada de
  la app.
- **Riesgos:** ninguno abierto en la parte A. El contrato no se cambia por cuenta propia.
- **Mejoras pendientes (fuera de esta parte):** mover `AdjustableClock` (test, `application/course`, hoy
  `public`) a un paquete de test común; unificar el decodificador UTF-8 de 3a con `Utf8Text`
  (`infrastructure/course`, privado de paquete) en un paquete común.

## Objetivo

Guardar en PostgreSQL las inscripciones y los módulos dominados de cada usuario, y ofrecer
`POST /api/progress/sync` según el contrato.

## Lo que ya hay y sirve de modelo

Para la **tarea 3** (web):

- `web/RequireSession.java` + `web/AuthenticatedHandler.java` (rutas con sesión; el usuario llega en
  `AuthenticationResult.Authenticated`).
- `web/course/CoursePublicationController.java` y sus respuestas; `web/ErrorResponse.java`.
- `infrastructure/course/JacksonCourseReader.java` (lector de bytes con Jackson: modelo para el lector
  de la petición) y `domain/progress/EnrollmentDraft.java` y `MasteredModuleDraft.java` (lo que el lector
  debe producir; sus constructores lanzan `NullPointerException` con `null`).
- `App.java` (*composition root*). Tests de la capa web: solo de los DTO (p. ej.
  `web/course/CourseResponsesTest.java`); la ruta se prueba con Yaak (en 3b, además, con `curl`).

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
3. [ ] **Ruta `POST /api/progress/sync`.** Partida en 3a, 3b y 3c (plan aprobado el 2026-11-24).
   ✅ Criterio de la tarea: tests del lector y del DTO (forma del JSON, fechas ISO, orden de la
   respuesta); con Yaak: 200, 400 con cuerpo malformado, 401 sin token; una petición de Yaak nueva
   guardada en `yaak/`; contrato congelado y avisado en el Relevo general.

   Decisiones (2026-11-24, con los usuarios):
   - **Límites:** ninguno propio; vale el de 2 MiB de Javalin (unos 30 000 módulos; una cuenta muy
     activa, unos 130 KB). Un máximo propio rechazaría la petición entera y bloquearía la cola
     (decisión 9). El contrato añade **413** (cuerpo > 2 MiB): lo da Javalin, sin cuerpo definido; la
     app lo trata como `ServerFailure`. El controlador **no** captura el 413.
   - **Orden de la respuesta:** el propuesto. Sale tal cual de `findAll`; el DTO no reordena. El de
     `moduleId` depende de la *collation* de la base de datos: el contrato dice que la app no depende
     del orden.
   - **Sin códigos de error nuevos** (`MALFORMED_BODY`, `UNAUTHENTICATED`, `COURSE_NOT_FOUND`, `REQUIRED`).
   - **Lector propio sobre bytes** (`ProgressRequestReader`; precedente: cursos), no `bodyAsClass`: el
     400 del contrato es estricto y Jackson por defecto es permisivo (campo ausente → `null`, número →
     texto); así cada caso de 400 es un test unitario. Reglas: campos desconocidos ignorados
     (*Tolerant Reader*; incluye `userId`); `mastered` obligatorio; fechas como texto con
     `Instant.parse` (un test fija el desfase `+01:00`); `courseId` que no es UUID y `moduleId` vacío
     pasan intactos al dominio (van a `skipped`). Sin dependencias nuevas.
   - **Al revisar la propuesta de 3a** (2026-11-24, con los usuarios): árbol (`readTree`) comprobado a
     mano, en una sola pasada `JsonNode → Optional<List<EnrollmentDraft>>` (sin forma intermedia con
     textos; ninguna excepción sale de su clase). **Clave repetida → 400**, también en un campo
     ignorado (`STRICT_DUPLICATE_DETECTION`, como en cursos). **Año fuera de 0001–9999 (UTC) → 400**:
     `Instant.parse` acepta años con signo o de más de 4 cifras y PostgreSQL no los guarda todos (sería
     un 500); alinea el contrato con RFC 3339. Decodificador UTF-8 propio con `REPORT` (`Utf8Text` es
     privado de su paquete y no se mueve); un test fija qué pasa con un BOM. Casos de `Malformed` con
     `@ParameterizedTest`.
   - **Permisos de 3b** (solo para 3b; fuera de ellos, preguntar):
     - *D1, migración:* aplicar la 006 a la base de desarrollo con `./gradus.sh up` o `db` (nunca
       `psql` a mano ni `down -v`). `SELECT filename FROM schema_migrations` antes (debe haber `005` y
       no `006`) y después. Para deshacer: `DROP TABLE mastered_modules, enrollments;` y
       `DELETE FROM schema_migrations WHERE filename = '006_create_enrollments.sql';` (solo si lo piden
       los usuarios).
     - *D2, datos de prueba:* solo a través de la API: dos cuentas nuevas, `progress-a@example.test` y
       `progress-b@example.test`, y su progreso, sobre cursos que ya existan (se leen con
       `GET /api/courses`; si no hay ninguno, parar y preguntar). Sin `UPDATE` ni `DELETE`; `SELECT` de
       solo lectura para comprobar filas. No se borra nada al terminar salvo que lo pidan los
       usuarios; se informa de lo creado.
     - *D3, quién ejecuta:* quien implementa lanza los escenarios con `curl`, guarda la petición en
       `backend/yaak/workspace/` (copiando una con sesión) y arranca el servidor en el puerto 7070 en
       segundo plano, parándolo después. Los usuarios repiten 200, 400 y 401 en Yaak al final.
   - Si una prueba real destapa un fallo en un adaptador JDBC (2b): parar, informar con la evidencia y
     proponer arreglo y test; no arreglarlo sin OK.

   3a. [x] **Lector de la petición y DTO de respuesta** (Sonnet, esfuerzo medio). Hecho en `web/progress/`:
   `ProgressRequestReader` (bytes → `ParsedRequest`: `StrictUtf8` → árbol → `SyncRequestJson`),
   `SyncRequestJson` (sustituye a `SyncRequestBody`), `IsoInstants`, `StrictUtf8`, `ParsedRequest` y
   `SyncResponse`. Plan original: `ParsedRequest` (sellado: `Parsed(drafts)` o
   `Malformed`), `ProgressRequestReader` (bytes → `ParsedRequest`) y `SyncResponse` (`from(SyncResult)`;
   fechas como texto con `Instant.toString()`), con `ProgressRequestReaderTest` y `SyncResponseTest`.
   Sin HTTP ni base de datos. Tests mínimos:
   - Lector, válido: cuerpo válido (borradores en el mismo orden, fechas parseadas); `enrollments: []`.
   - Lector, `Malformed`: cuerpo vacío, JSON roto, `null` o `[]` en la raíz; falta `enrollments`, o es
     `null`, no es una lista o tiene un elemento `null`; falta `courseId`, `enrolledAt` o `mastered`, o
     `mastered` tiene un elemento `null`; falta `moduleId` o `masteredAt`; número donde va texto; texto
     sobrante tras el JSON; fechas `"24/11/2026"`, `"2026-11-24"` y `""`.
   - Lector, reglas: `courseId` que no es UUID y `moduleId` vacío o en blanco pasan intactos;
     campos desconocidos (incluido `userId`) ignorados; fecha con desfase `+01:00` fijada por test.
   - Respuesta: nombres exactos de los campos; fechas ISO con `Z` y con fracción (`…00.123Z`);
     `moduleId` ausente (no `null`) en `skipped` de inscripción y presente en el de módulo; `code` con
     el nombre del `ErrorCode`; listas vacías como `[]`; se conserva el orden del `SyncResult`.
   - Revisión: a fondo. Ningún camino llega a un `NullPointerException`; sin dependencias nuevas; sin
     `bodyAsClass`; `mvn test` en verde (668 + los nuevos).
   - Commit: `add: progress sync request reader and response DTO (progress task 3a)`.

   3b. [x] **Controlador, cableado, Yaak y pruebas reales** (Sonnet, esfuerzo alto). `ProgressController`
   (sin lógica: lee los bytes, `switch` sobre `ParsedRequest`; usuario de `authenticated.user().id()`;
   `Malformed` → 400 `MALFORMED_BODY` en el campo `body`), cambio mínimo en `App.java` (los dos
   adaptadores reciben el `DataSource`; `SyncProgress` una sola vez; ruta con `requireSession.around`) y
   la petición nueva de Yaak. Es la **primera ejecución de los adaptadores JDBC contra PostgreSQL**.
   Comprobaciones mínimas, con `curl`:
   1. 401 sin token y con token inventado.
   2. 400 con JSON roto, con `{}` y con fecha mala.
   3. 200 con lista vacía (`{"enrollments":[],"skipped":[]}`).
   4. Dos cursos con módulos desordenados: la respuesta sale ordenada y las filas están en la base.
   5. La misma petición dos veces deja el mismo estado.
   6. Gana la fecha más antigua; una más reciente no cambia nada (`enrolledAt` y `masteredAt`).
   7. Curso inexistente y `courseId` que no es UUID van a `skipped`; el resto se guarda.
   8. `moduleId` en blanco va a `skipped` con `REQUIRED`; el resto se guarda.
   9. Fecha futura recortada a la hora del servidor.
   10. Repetidos en la misma petición, fusionados.
   11. Fracción de segundo de ida y vuelta (`timestamptz` guarda microsegundos).
   12. Aislamiento: la cuenta B no ve lo de A; un `userId` en el cuerpo se ignora.
   13. (Opcional) Dos peticiones a la vez con los mismos cursos en distinto orden: sin bloqueo.
   14. Cuerpo mayor de 2 MiB: 413.
   15. Fecha `"0001-01-01T00:00:00Z"` de ida y vuelta: sin 500, y anotar si vuelve cambiada.
   16. `moduleId` ausente (no `null`) en el `skipped` de inscripción con el mapper real de Javalin.
   - Revisión: a fondo. Controlador delgado; usuario siempre de la sesión; `App.java` con el cambio
     mínimo; salidas de `curl`; `schema_migrations` antes y después; lista de lo creado en la base; sin
     `UPDATE` ni `DELETE`.
   - Commit: `add: POST /api/progress/sync route and Yaak request (progress task 3b)`.

   3c. [x] **Congelar el contrato** (lo hace el orquestador). Volcar a `api-progreso.md` las decisiones
   de arriba (413, orden, sin códigos nuevos, campos desconocidos ignorados, clave repetida → 400,
   `mastered` obligatorio, fechas aceptadas con año 0001–9999, UTF-8 sin BOM), pasar su estado a «congelado», avisarlo en el Relevo general (afecta a la parte B,
   tarea 5), marcar la casilla de la tarea 3 y reescribir los Relevos.
   - Commit: `docs: freeze progress sync contract and close backend part (progress task 3c)`.

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
- 2026-11-24: **tarea 3 decidida y partida en 3a, 3b y 3c** con los usuarios (decisiones y permisos de
  3b, en la tarea 3). Contrato todavía en borrador; no se ha tocado código ni la base de desarrollo.
- 2026-11-24: propuesta de 3a aprobada con ajustes («Al revisar la propuesta de 3a», en la tarea 3).
- 2026-11-24: **3a hecha y revisada** (764 tests en verde tras quitar un test duplicado, sin avisos; sin
  base de datos ni HTTP). Un BOM
  UTF-8 al principio del cuerpo da `Malformed` (fijado por test; el lector de cursos sí lo quita): pasa
  al contrato en 3c.
- 2026-11-24: propuesta de 3b aprobada con ajustes (Yaak encadena el login de la autora; un login por
  cuenta y comando con `DELETE /api/sessions/current` al final; desempate por `course_id` en el escenario 15).
- 2026-11-24: **3b hecha y revisada** (el revisor confirmó los 764 tests, el diff y, con un `SELECT` de
  solo lectura, la 006 y 2 + 2 inscripciones con 313 + 4 módulos). `ProgressController` (sin lógica; no
  captura el 413), cambio mínimo en `App.java` y petición de Yaak «Sincronizar progreso (200)» en la carpeta
  nueva «Progreso · Sincronización». 764 tests en verde, sin avisos (no se añaden tests: la capa web solo se
  prueba con Yaak). Primera ejecución real de los adaptadores JDBC contra PostgreSQL: **sin fallos**. 006
  aplicada con `./gradus.sh db` (`schema_migrations`: 001–005 antes, 001–006 después). Los 16 escenarios
  con `curl` dieron lo esperado (datos de prueba en `progress-a@example.test` y `progress-b@example.test`,
  sin `UPDATE` ni `DELETE`; cero sesiones abiertas al terminar). Para 3c, anotar en el contrato: el 413 es
  `text/plain` con el cuerpo `Content Too Large`; PostgreSQL redondea la fracción a microsegundos
  (`.123456789Z` → `.123457Z`); el año `0001-01-01T00:00:00Z` vuelve igual.
- 2026-11-25: **3c hecha.** Contrato congelado, con lo decidido y lo comprobado en 3a y 3b (413 en
  `text/plain`, microsegundos, año 0001–9999, UTF-8 sin BOM, clave repetida, campos desconocidos,
  orden). Avisado en el Relevo general para la parte B (B5). Casilla de la tarea 3, pendiente de Yaak.

## Siguiente paso

Los usuarios repiten 200, 400 y 401 en Yaak; el revisor marca entonces la casilla de la tarea 3. La
parte A queda cerrada; lo siguiente es la parte B, con su propio orquestador.
