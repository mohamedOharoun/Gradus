# Seguimiento del progreso — parte B (`sharedLogic`)

Índice común, decisiones 1–11 y acuerdos de proceso: `docs/tareas/seguimiento-progreso.md`.
Contrato (congelado): `backend/docs/api-progreso.md`. Rama: `dev/course-progress-tracking`.

## Objetivo

Dar a la app, en Kotlin puro y probado, todo lo que necesita para inscribirse, guardar lo dominado
con su fecha (por usuario), impedir estudiar sin inscripción y sincronizar con
`POST /api/progress/sync`. La parte C (Room, interfaz) solo tiene que implementar estos puertos y
llamar a estos casos de uso.

## Alcance

- **4.** Modelo (`Enrollment` y lo dominado con fecha), `EnrollmentStore` (+ en memoria),
  `studyAccess(enrolled)` → `Allowed | NotEnrolled`, `markMastered` exige inscripción, almacén en
  memoria por usuario.
- **5.** `ProgressGateway` + `HttpProgressGateway` (Ktor) + DTO + *mapper* + Fake; desenlaces
  `Synced | SessionExpired | Unreachable | ServerFailure`.
- **6.** `ProgressSync`: sube lo pendiente, fusiona la respuesta y marca lo subido.

## Fuera de alcance

- Room, `AppContainer`, ViewModels y pantallas (parte C); sesión persistente y WorkManager (parte D).
- Lo de «Fuera» del índice: darse de baja, registro de respuestas, SM-2, Inicio real…
- Arreglar en `app/` lo que rompa esta parte: se apunta en el Relevo y es de la parte C.

## Relevo

Se **reescribe** al final de cada sesión.

- **Estado (2026-11-25): parte B terminada** (tareas 4, 5 y 6 revisadas; 156 tests en
  `course.progress*`, todos en verde). Lo que hay y cómo usarlo, en `docs/arquitectura-progreso.md`
  («Cómo extenderlo» tiene la guía para C8). Relevo general reescrito.
- **Toca:** nada en la parte B. Si la parte C necesitara un cambio en `sharedLogic`, se reabre aquí
  con un paso nuevo (y el contrato sigue congelado). La batería completa se pasa al cierre (tarea 14).
- **Riesgos:**
  - `ExerciseViewModel.markMastered` lanza la excepción en la app hasta C9/C10 (B-2, aceptado).
  - El servidor redondea las fechas a microsegundos: nada debe emparejar lo enviado con lo recibido
    comparando fechas.
  - Gradle: `--rerun` a veces da `testAndroidHostTest NO-SOURCE` (no ejecuta nada). Orden fiable:
    `./gradlew :sharedLogic:compileAndroidHostTest --rerun :sharedLogic:testAndroidHostTest --rerun`;
    comprobar en los XML de `build/test-results/` que la hora es la de la ejecución.
- **Para quien use esto (parte C):** `courseId` en minúsculas (ver «Qué cuidar al mergear» de la
  referencia); sin sesión los `Flow` terminan; `ProgressSync` se crea con el **mismo** objeto que
  implementa los tres puertos del almacén.

## Lo que ya hay y sirve de modelo

- `course/progress/ProgressStore.kt` (puerto, **no cambia**: decisión 8) e
  `InMemoryProgressStore.kt` (un `MutableStateFlow` + `update` atómico), con
  `InMemoryProgressStoreTest`.
- `account/SessionStore.kt` (`current(): Session?`, suspend, sin `Flow`) y `account/Session.kt`
  (`token`, `account.id`; `toString` oculta el token).
- `course/ModuleState.kt`: `stateOf` → `MASTERED | FRONTIER | LOCKED` (funciones puras).
- HTTP: `account/http/GradusHttpClient.kt` (*factory method*, `expectSuccess = false`,
  `ignoreUnknownKeys`), `callServer` en `HttpAccountGateway.kt` (excepciones → desenlace),
  `course/http/HttpCourseGateway.kt` (estado primero, cuerpo después) y su test con `MockEngine`
  (`HttpCourseGatewayTest.kt`, ayudas en `commonTest/.../account/http/MockServer.kt`).
- Dependencias ya disponibles: Ktor, kotlinx-serialization, coroutines; en `commonTest`,
  `ktor-client-mock` y `kotlinx-coroutines-test`. Fechas: `kotlin.time` (biblioteca estándar).

## Decisiones de la parte B (aprobadas el 2026-11-25)

- **B-1. Sin dependencias nuevas.** `ktor-client-mock` y `kotlinx-coroutines-test` ya están en
  `commonTest`. Fechas con `kotlin.time.Instant` y `Clock` (biblioteca estándar, estable en Kotlin 2.4
  según los usuarios; se confirma al compilar el primer paso). Se guardan como `Instant`, nunca como
  texto (comparar texto falla: `…00Z` queda detrás de `…00.500Z`). El `Clock` se inyecta.
- **B-2. Lo que se rompe en `app/`:**
  - `AppContainer.kt:32` (`InMemoryProgressStore()`) dejaría de compilar: **B4 cambia solo esa línea**
    para pasarle el `sessionStore` (`Clock.System` por defecto). Ojo: `dev/joel` también toca `AppContainer`.
  - `ExerciseViewModel.kt:48` (`markMastered`): compila, pero lanzará la excepción (no hay botón de
    inscribirse) y la app se cerrará al dominar un módulo **hasta C9/C10**. Se acepta; no se captura
    (es el invariante). La rama no sirve para enseñar la app hasta entonces.
  - `CourseViewModel.kt:114` (`mastered`): sin sesión ve el progreso vacío; la sesión se lee al empezar
    a escuchar.
- **B-3. Sin sesión:** leer devuelve vacío; escribir lanza `IllegalStateException` (como dominar sin
  inscripción: un fallo de programación). **Inscribirse dos veces:** sin error, se queda la primera fecha.
  Sin baja (decisión 7), comprobar la inscripción y escribir después es seguro.
- **B-4. El puerto de pendientes (lo pendiente, marcar lo subido y fusionar) va en la tarea 4**, en un
  paso propio, para que C7 diseñe Room una sola vez; la tarea 6 queda solo con el caso de uso.
- **B-5. Lo que el DTO cuida del contrato** (lo pide la tarea 5):
  - Los DTO de la petición **no tienen valores por defecto** (el `Json` del proyecto no los escribe y
    faltaría `mastered: []` → 400).
  - Primero el estado, después el cuerpo: 401 → `SessionExpired`; 400, 413 (`text/plain`) y 5xx →
    `ServerFailure` sin leer el cuerpo.
  - Fechas: se envían con `Instant.toString()` (UTC con `Z`); se aceptan con 0, 3 y 6 decimales.
  - `skipped`: `moduleId` ausente = inscripción entera; presente, tal cual (también `" "`).
  - Cuerpo `application/json` en UTF-8 sin BOM.
  - Emparejar lo subido **por clave** (curso, módulo), nunca por fecha (el servidor redondea a
    microsegundos); el `courseId` de `skipped` se compara sin distinguir mayúsculas.
- **B-6. Tests «no inscrito no abre ni frontera ni dominado»:** `studyAccess(enrolled)` no recibe el
  estado del módulo, y un módulo dominado sin inscripción no existe en la lógica. En B4 se prueban
  `NotEnrolled` y `Allowed`; la combinación con `ModuleState` la prueba **C9** (apuntado en el Relevo
  general).
- **B-7. Referencia viva: `docs/arquitectura-progreso.md`.** Cómo es y cómo se comporta el progreso
  **ahora** (no su historia): mapa de piezas y patrones, flujo, invariantes, contrato ↔ DTO, cómo
  extenderlo (p. ej. un tipo de ejercicio nuevo) y qué cuidar al mergear. Cada paso actualiza su
  sección; el orquestador la revisa con el código. C y D añaden las suyas.
- **B-8. Lo omitido por el servidor (`skipped`) sale de la cola y el móvil lo conserva** (contrato: «el
  móvil las quita de su cola de pendientes»; borrarlo sería una baja, decisión 7). Por tanto **todo lo
  enviado sale de la cola** tras un 200, por clave; `settle` no necesita `skipped`, y la comparación sin
  mayúsculas de B-5 ya no hace falta para la cola (el gateway sigue devolviendo `skipped`).
- **B-9. `skipped` es solo informativo** (por B-8 no cambia nada en el móvil): al leerlo se es tolerante.
  `"moduleId": null` (el contrato dice que no ocurre) se lee como la inscripción entera, igual que la
  clave ausente; no hace ilegible la respuesta. Lo obligatorio de `enrollments`, en cambio, es estricto.
- **B-10. `ProgressSync.sync` llama siempre al servidor**, también sin nada pendiente (envía
  `{"enrollments":[]}`, que el contrato admite «para solo leer»): si no, un móvil nuevo nunca recibiría
  el progreso de la cuenta. Sustituye al criterio del índice «nada pendiente no llama a la red». Si D13
  (WorkManager) quiere ahorrar llamadas, lo decide allí con `pending(...).isEmpty`.
- **B-11. `ProgressSync` sin `Mutex`.** Dos `sync` a la vez no pierden ni deshacen nada: cada `settle`
  vacía la cola solo con su propio `sent` (por clave) y la fusión por fecha mínima no depende del orden;
  el coste es una llamada repetida, que el servidor fusiona. Sin `NonCancellable` en `settle`: si se
  cancela antes de escribir, lo pendiente se reenvía en la siguiente `sync` sin efectos. Si D13 quiere
  que no coincidan, lo resuelve WorkManager (`enqueueUniqueWork`). Para probarlo, la respuesta de
  `FakeProgressGateway` pasa a ser `suspend` (cambio de una palabra en el paso 5b).

## Tareas

La casilla la marca el orquestador de la parte tras revisar. Los tests nombrados son el mínimo.

### [x] 4. Modelo, puertos y `studyAccess`

Criterios de aceptación:
- `studyAccess(enrolled)` devuelve `Allowed | NotEnrolled` (sellado), sin textos.
- `EnrollmentStore` (+ en memoria) con fecha de inscripción; el almacén en memoria es **por usuario**
  (lee la sesión de `SessionStore`, decisión 8) y guarda la fecha de cada módulo dominado.
- `markMastered` sobre un curso no inscrito lanza excepción y no deja filas.
- Tests mínimos: no inscrito no abre ni un módulo frontera ni uno dominado; inscrito sin conexión sí;
  el progreso es por usuario; inscribirse dos veces; dominar sin inscripción lanza excepción y no deja
  filas. Los tests actuales de `InMemoryProgressStoreTest` siguen pasando (adaptados).
- `./gradlew :sharedLogic:testAndroidHostTest --rerun` en verde.

Pasos (cada uno: propuesta → OK → tests primero; actualiza su sección de la referencia):

- [x] **4a. Modelo y `studyAccess`** · Sonnet 5.5, esfuerzo bajo · revisión ligera.
  - `course/progress/`: `Enrollment` (`courseId`, `enrolledAt: Instant`) y `MasteredModule`
    (`moduleId`, `masteredAt: Instant`); `StudyAccess` sellado (`Allowed | NotEnrolled`) y
    `studyAccess(enrolled: Boolean)`.
  - Tests mínimos (`StudyAccessTest`): no inscrito → `NotEnrolled`; inscrito → `Allowed`.
- [x] **4b. `EnrollmentStore` y almacén en memoria por usuario** · Sonnet 5.5, esfuerzo alto ·
  revisión a fondo (atomicidad con `update`, nada escrito si falla, sesión leída dentro del `Flow`).
  - `EnrollmentStore` (`enrolled(courseId): Flow<Boolean>`, `suspend enroll(courseId)`).
    `InMemoryProgressStore(sessions: SessionStore, clock: Clock = Clock.System)` implementa los dos
    puertos sobre un solo `MutableStateFlow` por usuario. `markMastered` exige inscripción (B-3).
    Solo la línea 32 de `AppContainer.kt` (B-2). Se adaptan los 6 tests de `InMemoryProgressStoreTest`.
  - Tests mínimos: por usuario; inscribirse dos veces conserva la primera fecha; dominar sin
    inscripción lanza y no deja filas; sin sesión, leer da vacío y escribir lanza; la fecha sale del
    `Clock`; inscrito y sin gateway (sin conexión) → `Allowed`.
- [x] **4c. Puerto de pendientes** · Opus 5.5, esfuerzo medio · revisión a fondo (de su forma depende
  el esquema Room de C7).
  - Modelo público con la forma del contrato: `ProgressSnapshot(enrollments: List<EnrollmentSnapshot>)`,
    `EnrollmentSnapshot(enrollment, mastered: List<MasteredModule>)`. Puerto `ProgressSyncStore`:
    `pending(userId)` y `settle(userId, sent, remote)` atómico (sin `skipped`: B-8). Lo pendiente es
    estado interno de `CourseProgress` (`enrollmentPending`, `pendingModules` ⊆ `mastered.keys`), que en
    Room será una columna en cada tabla. `settle` = quitar de la cola lo de `sent` por clave y después
    fusionar `remote` (lo nuevo, no pendiente; lo que ya existía, fecha mínima y su marca intacta).
    Versión en memoria; `progressOf` se mantiene. El usuario va por parámetro (decisión 11).
  - Tests mínimos: los de la propuesta aprobada (`PendingProgressTest`, `SettleQueueTest`,
    `SettleMergeTest`), más: una fecha futura del móvil queda sustituida por la recortada del servidor;
    `isEmpty`; los tests no dependen del orden de las listas.

### [x] 5. `ProgressGateway` HTTP

Criterios de aceptación:
- `ProgressGateway` (puerto) + `HttpProgressGateway` (Ktor) + DTO + *mapper* + `FakeProgressGateway`.
- La `Session` llega como parámetro (decisión 11) y va en `Authorization: Bearer <token>`.
- Desenlaces `Synced | SessionExpired | Unreachable | ServerFailure`.
- Tests mínimos con `MockEngine`: 200, 401, 400, sin conexión, 5xx, cuerpo ilegible; y lo del
  contrato: 413 en `text/plain`, fechas con 0, 3 y 6 decimales, `skipped` con y sin `moduleId`,
  `mastered: []` se envía, cuerpo JSON en UTF-8 sin BOM.

Pasos:

- [x] **5a. DTO y *mapper*** · Sonnet 5.5, esfuerzo medio · revisión del contrato punto por punto (B-5).
  - Tests mínimos: `mastered: []` se envía; fechas con 0, 3 y 6 decimales; `skipped` con y sin
    `moduleId` (también `" "`); campos desconocidos ignorados; falta un campo de la respuesta → ilegible.
- [x] **5b. `ProgressGateway`, `HttpProgressGateway` (con `callServer`) y `FakeProgressGateway`** ·
  Sonnet 5.5, esfuerzo medio · revisión del contrato y de que el token no salga en logs.
  - Tests mínimos: 200, 401, 400, 413 `text/plain`, 5xx, sin conexión, tiempo agotado, cuerpo
    ilegible; `POST /api/progress/sync` con `Bearer`, JSON UTF-8 sin BOM.

### [x] 6. `ProgressSync`

Criterios de aceptación:
- Sube lo pendiente, fusiona la respuesta (unión) y marca lo subido; devuelve el desenlace del gateway.
- Tests mínimos: sin nada pendiente envía un snapshot vacío y trae lo del servidor (B-10); un fallo de
  red deja lo pendiente; 401 no reintenta; lo dominado en otro móvil aparece; lo omitido por el
  servidor sale de la cola.

Paso único · Opus 5.5, esfuerzo medio · revisión a fondo (corrutinas; dos sincronizaciones a la vez).
Tests añadidos a los mínimos: `ServerFailure` deja lo pendiente; un 200 sin cambios deja el estado igual.

## Progreso

- 2026-11-25: documento creado por el orquestador B; decisiones B-1 a B-7 y plan de las tareas 4–6
  aprobados. Creado el esqueleto de `docs/arquitectura-progreso.md`.
- 2026-11-25: **4a hecho y revisado.** Sin tests de los `data class` (no tienen lógica propia); un
  test fija que el `when` sobre `StudyAccess` es exhaustivo sin `else`.
- 2026-11-25: **4b hecho y revisado.** Fuera del plan, aprobado: `progressOf(userId)` `internal` para
  ver fechas en los tests; sin sesión, el `Flow` emite el vacío y **termina** (la interfaz debe volver a
  escuchar al cambiar de sesión). 48 tests de progreso en verde.
- 2026-11-25: **4c hecho y revisado; tarea 4 terminada.** `progressOf` se mantiene (lo usan los tests
  y `pending`). Lo pendiente que también llega del servidor sigue pendiente (reenviarlo es inofensivo).
  Tests de «quien escucha» con `backgroundScope` + `yield()` (sin `UnconfinedTestDispatcher`, que pide
  *opt-in*).

- 2026-11-25: **5a hecho y revisado.** `SkippedDto.moduleId` es `String?` (B-9). `encodeDefaults = true`
  es un refuerzo sin test propio (hoy ningún DTO de la petición tiene valor por defecto que lo ponga a
  prueba; lo vigila el test de `"mastered":[]`). Las fechas con nanosegundos se envían enteras.

- 2026-11-25: **5b hecho y revisado; tarea 5 terminada.** Fuera del plan, aprobado: el Fake recuerda
  `userId` y lo enviado (`SyncCall`), **no** la `Session`, para que el token no quede guardado.
  `ProgressGateways.http` no se conecta a `AppContainer` (C8).

- 2026-11-25: **6 hecho y revisado; parte B terminada.** `ProgressSync` (B-10, B-11) y `answer`
  `suspend` en el Fake. El orquestador corrigió el comentario de `ProgressSnapshot.isEmpty` (y su test
  y la referencia), que contradecía B-10. Recuento real de tests de progreso: 43 tras 4b, 71 tras 4c,
  106 tras 5a, 134 tras 5b y 156 tras 6 (las cifras anteriores de este documento estaban mal sumadas).

## Siguiente paso

Parte B terminada. Lo siguiente es de la parte C (C7 Room, C8 sincronizar), con su orquestador.
