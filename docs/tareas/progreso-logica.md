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

- **Estado (2026-11-25):** 4a y 4b hechos y revisados. `InMemoryProgressStore(sessions, clock)` cumple
  `ProgressStore` y `EnrollmentStore` por usuario, con lo dominado anidado en su inscripción
  (`UserProgress` → `CourseProgress`). Detalle en `docs/arquitectura-progreso.md`.
- **Toca:** paso **4c** (puerto de pendientes; mensaje al implementador preparado por el orquestador).
  Cuando 4c esté hecho, B4 queda entera: reescribir el Relevo general (desbloquea la parte C).
- **Riesgos:**
  - `ExerciseViewModel.markMastered` lanza la excepción en la app hasta C9/C10 (B-2, aceptado).
  - El servidor redondea las fechas a microsegundos: nada debe emparejar lo enviado con lo recibido
    comparando fechas.
  - Gradle: `--rerun` a veces da `testAndroidHostTest NO-SOURCE` (no ejecuta nada). Orden fiable:
    `./gradlew :sharedLogic:compileAndroidHostTest --rerun :sharedLogic:testAndroidHostTest --rerun`;
    comprobar en los XML de `build/test-results/` que la hora es la de la ejecución.
- **Qué mirar en la revisión de 4c:** el `pending` de cada inscripción y de cada módulo no está en los
  *value objects* públicos (`Enrollment`, `MasteredModule`) sino en el estado interno; `settle` es una
  sola transformación pura dentro de `update`; todo lo enviado sale de la cola, y lo marcado durante la
  petición no; la fusión es unión con la fecha más antigua y avisa a quien escucha; el comentario del
  puerto dice lo que Room tiene que cumplir (en una sola transacción).
- **Abierto para 4c:** si `pending` sustituye a `progressOf` (se mantiene si los tests de fechas lo
  necesitan); si `settle` necesita `skipped` (ver el mensaje de 4c).

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

## Tareas

La casilla la marca el orquestador de la parte tras revisar. Los tests nombrados son el mínimo.

### [ ] 4. Modelo, puertos y `studyAccess`

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
- [ ] **4c. Puerto de pendientes** · Opus 5.5, esfuerzo medio · revisión a fondo (de su forma depende
  el esquema Room de C7).
  - `ProgressSnapshot`, `Skipped` (inscripción entera o módulo), `pending(userId)` y
    `settle(userId, sent, remote, skipped)` atómico; versión en memoria. El usuario va por parámetro
    (decisión 11); `ProgressStore` no cambia (decisión 8).
  - Tests mínimos: marcar lo subido por clave; lo dominado durante la petición sigue pendiente; en la
    fusión gana la fecha más antigua; lo omitido sale de la cola (inscripción entera o solo el módulo);
    `courseId` sin distinguir mayúsculas.

### [ ] 5. `ProgressGateway` HTTP

Criterios de aceptación:
- `ProgressGateway` (puerto) + `HttpProgressGateway` (Ktor) + DTO + *mapper* + `FakeProgressGateway`.
- La `Session` llega como parámetro (decisión 11) y va en `Authorization: Bearer <token>`.
- Desenlaces `Synced | SessionExpired | Unreachable | ServerFailure`.
- Tests mínimos con `MockEngine`: 200, 401, 400, sin conexión, 5xx, cuerpo ilegible; y lo del
  contrato: 413 en `text/plain`, fechas con 0, 3 y 6 decimales, `skipped` con y sin `moduleId`,
  `mastered: []` se envía, cuerpo JSON en UTF-8 sin BOM.

Pasos:

- [ ] **5a. DTO y *mapper*** · Sonnet 5.5, esfuerzo medio · revisión del contrato punto por punto (B-5).
  - Tests mínimos: `mastered: []` se envía; fechas con 0, 3 y 6 decimales; `skipped` con y sin
    `moduleId` (también `" "`); campos desconocidos ignorados; falta un campo de la respuesta → ilegible.
- [ ] **5b. `ProgressGateway`, `HttpProgressGateway` (con `callServer`) y `FakeProgressGateway`** ·
  Sonnet 5.5, esfuerzo medio · revisión del contrato y de que el token no salga en logs.
  - Tests mínimos: 200, 401, 400, 413 `text/plain`, 5xx, sin conexión, tiempo agotado, cuerpo
    ilegible; `POST /api/progress/sync` con `Bearer`, JSON UTF-8 sin BOM.

### [ ] 6. `ProgressSync`

Criterios de aceptación:
- Sube lo pendiente, fusiona la respuesta (unión) y marca lo subido; devuelve el desenlace del gateway.
- Tests mínimos: nada pendiente no llama a la red; un fallo de red deja lo pendiente; 401 no
  reintenta; lo dominado en otro móvil aparece; lo omitido por el servidor sale de la cola.

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

## Siguiente paso

Paso 4c: abrir un chat nuevo con el mensaje del orquestador (Opus 5.5, esfuerzo medio).
