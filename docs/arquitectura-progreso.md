# Progreso del estudiante — cómo está hecho y cómo se comporta

Documento de **referencia**: describe el estado **actual** (no la historia; esa está en
`docs/tareas/progreso-logica.md` y `progreso-app.md`). Léelo antes de mergear una rama que toque el
progreso o de añadir algo que lo use (p. ej. un tipo de ejercicio nuevo).

Cada paso que cambie una pieza actualiza su sección; el orquestador la revisa junto con el código.
Contrato con el servidor: `backend/docs/api-progreso.md`.

## Mapa de piezas

Puertos, implementaciones, casos de uso y el patrón de diseño de cada uno.

Paquete `course/progress/` (Kotlin puro, sin textos):

| Pieza | Qué es | Patrón |
|---|---|---|
| `StudyAccess` (`Allowed \| NotEnrolled`) | Si se puede estudiar un curso | Jerarquía sellada (tipo suma): el `when` es exhaustivo |
| `studyAccess(enrolled)` | Decide el acceso solo por la inscripción | Función pura |
| `ModuleTap` (`OpenPractice \| Locked(missing) \| NotEnrolled`) y `Course.tapModule(moduleId, mastered, access): ModuleTap?` | Qué pasa al tocar un módulo del camino: combina `StudyAccess` con `ModuleState`. Sin inscripción gana `NotEnrolled` (C-6); `Locked` lleva los prerrequisitos directos que faltan (`missingFor`); `null` si el módulo no existe | Jerarquía sellada (tipo suma) y función pura (extensión) |
| `Enrollment(courseId, enrolledAt)` | Una inscripción y su fecha | *Value object* (`data class`) |
| `MasteredModule(moduleId, masteredAt)` | Un módulo dominado y su fecha | *Value object* (`data class`) |
| `EnrollmentStore` | Puerto: `enrolled(courseId): Flow<Boolean>` y `suspend enroll(courseId)` | *Strategy* (puerto con implementaciones intercambiables) |
| `ProgressStore` | Puerto: `mastered(courseId): Flow<Set<String>>` y `suspend markMastered(courseId, moduleId)` | *Strategy* |
| `ProgressSnapshot(enrollments)` → `EnrollmentSnapshot(enrollment, mastered)` | Lo que se envía o se recibe al sincronizar, con la forma del contrato (lo dominado dentro de su inscripción). Las listas no tienen orden garantizado; `isEmpty` = nada que subir (`ProgressSync` llama igual, B-10) | *Value object* / DTO de dominio |
| `ProgressSyncStore` | Puerto de la sincronización: `suspend pending(userId)` y `suspend settle(userId, sent, remote)`; el usuario va por parámetro (decisión 11) | *Strategy* |
| `InMemoryProgressStore(sessions, clock)` | Implementa **los tres puertos** con un solo `MutableStateFlow`; la parte C pondrá Room en su lugar | *Interface Segregation* (un objeto, tres puertos pequeños); inyección de dependencias (`SessionStore`, `Clock`) |
| `UserProgress` → `CourseProgress` (`internal`) | El estado de un usuario: `courses: courseId → CourseProgress(enrollment, mastered, enrollmentPending, pendingModules)` | Valor inmutable con transformaciones puras (enfoque funcional) |
| `UserProgress.pending()` y `.settled(sent, remote)` (`internal`, `UserProgressSync.kt`) | Las transformaciones puras de la sincronización | Funciones puras (extensión) |
| `SyncResponse(remote, skipped)` | Un 200 de la sincronización en el modelo de la app: todo el progreso remoto (`ProgressSnapshot`) y lo omitido | *Value object* |
| `SkippedProgress` (`WholeEnrollment \| SingleModule`) | Lo que el servidor no guardó, con su `code` como texto. Solo informativo (B-8, B-9) | Jerarquía sellada (tipo suma) |
| `ProgressGateway` | Puerto: `suspend sync(session, pending): SyncOutcome`. La `Session` va por parámetro (decisión 11) | *Strategy* |
| `SyncOutcome` (`Synced(SyncResponse) \| SessionExpired \| Unreachable \| ServerFailure`) | Desenlace de la llamada, sin textos; nunca se lanza una excepción por red o servidor | Jerarquía sellada (tipo suma) |
| `ProgressGateways.http(baseUrl)` | La única forma de obtener el gateway real desde fuera de `sharedLogic`. `AppContainer` se lo pasa a `ProgressModule` (C8a) | *Factory method* |
| `FakeProgressGateway(answer)` y `SyncCall(userId, sent)` | Gateway sin red para los tests de `ProgressSync`: responde lo configurado y recuerda **el id del usuario** y lo enviado, nunca la `Session` (el token no queda guardado). `answer` es `suspend` (puede esperar a un `CompletableDeferred`: el test decide cuándo llega la respuesta) y la llamada se apunta antes de responder | *Test double* (fake) |
| `ProgressSync(store, gateway)` y `suspend sync(session): SyncOutcome` | Caso de uso de la sincronización: sube lo pendiente del usuario de la sesión, fusiona la respuesta y devuelve el desenlace del gateway tal cual | Caso de uso (*Interactor*) sobre dos puertos inyectados (*Strategy*) |
| `SessionProgressSync(sessions, sync)` y `suspend run(): SessionSyncResult` (11a) | Sincroniza con la sesión **guardada**: sin sesión devuelve `NoSession` sin llamar; si no, `Finished(outcome)` con el desenlace tal cual. Con `SessionExpired` borra la sesión con `clearIfCurrent(session)` (la que se leyó al empezar). Es **el único sitio que trata el 401**; la interfaz solo reacciona a «la sesión se ha borrado». Lo usará el `Worker` (13b) | Caso de uso que compone dos puertos |
| `SessionSyncResult` (`NoSession \| Finished(outcome)`) | Qué pasó al sincronizar con la sesión guardada | Jerarquía sellada (tipo suma) |
| `BackgroundSyncDecision` (`Done \| Retry \| GiveUp`), `MAX_SERVER_FAILURE_ATTEMPTS = 5` y `backgroundSyncDecision(result, attempt)` (13a) | Política de reintento del trabajo en segundo plano (sin acoplar a WorkManager): `NoSession` o `Synced` → `Done`; `Unreachable` → `Retry` siempre; `ServerFailure` → `Retry` en intentos `0..3` y `GiveUp` desde `4` (5.º intento, `runAttemptCount` 0-based); `SessionExpired` → `GiveUp`. `attempt < 0` lanza `IllegalArgumentException` | Jerarquía sellada (tipo suma) y función pura |

Paquete `course/` y `course/http/` (copia local de cursos; Kotlin puro, desde 12a):

| Pieza | Qué es | Patrón |
|---|---|---|
| `encodeCourseVersion(course)` (`course/http/CourseDtoEncoder.kt`) | Inverso de `decodeCourseVersion`: serializa un `Course` al JSON de `GET /api/courses/{id}` (`CourseVersionDto` + `CourseContentDto` con `formatVersion = 1`). Genera ids estables `"o$index"` para opciones y ordena `MultipleChoice.correct`; comparte `internal val courseJson` de `CourseDtoMappers.kt` | *Mapper*, funciones puras |
| `CourseCopyStore` y `InMemoryCourseCopyStore` (`course/`) | Puerto local `save(courseId, json)` (sobrescribe) y `load(courseId): String?`, con implementación en memoria sobre `MutableStateFlow` para tests | *Strategy* (puerto hexagonal) |
| `OfflineCourseGateway(delegate, copies)` (`course/`) | Red primero: en `Found` guarda la copia bajo el `id` pedido y la devuelve; en `Unreachable` carga y decodifica la copia (si falta o es `Malformed`/`UnsupportedFormat` devuelve `Unreachable` sin borrarla). `NotFound`, `UnsupportedFormat`, `ServerFailure` y `search` pasan tal cual sin tocar `copies`; fallos del almacén se propagan | *Decorator* sobre `CourseGateway` |

Paquete `account/` (piezas de la sesión que usa el progreso; Kotlin puro, desde 11a):

| Pieza | Qué es | Patrón |
|---|---|---|
| `SessionStore.clearIfCurrent(session)` | Borra la sesión guardada solo si tiene **el mismo token** (un 401 invalida el token sea cual sea su `expiresAt`); con otra o sin ninguna, nada. El puerto no cambia. **Límite:** lee y borra en dos pasos (el puerto no tiene uno atómico); una sesión guardada justo entre los dos se borraría. Guardar mientras la petición viaja sí está protegido (probado) | Función de extensión |
| `encodeSession(session)` y `decodeSession(text): Session?` (`account/storage/`) | `Session` ↔ JSON para guardarla en disco. Lo ilegible (roto, vacío, no es objeto, falta un campo, `null` o tipo equivocado) da `null` y nunca lanza; el fallo no lleva texto. **El JSON lleva el token en claro**: se cifra en la app (11b) y no se registra nunca. Campos desconocidos, ignorados. Sin campo de versión: un formato viejo da `null` (volver a iniciar sesión) | *Mapper*, funciones puras |
| `StoredSessionDto` y `StoredAccountDto` (`internal`) | El JSON del disco, aparte del de la red (`SessionResponseDto`): si cambia el del servidor, lo guardado sigue leyéndose. El `toString` oculta el token | DTO |

Paquete `course/progress/http/` (`internal`, traduce entre el modelo y el JSON; sin HTTP):

| Pieza | Qué es | Patrón |
|---|---|---|
| `ProgressDtos.kt` (`SyncRequestDto`, `EnrollmentDto`, `MasteredDto`, `SyncResponseDto`, `SkippedDto`) | El JSON de `POST /api/progress/sync`, campo a campo | DTO |
| `encodeSyncRequest(snapshot)` | `ProgressSnapshot` → texto JSON de la petición | *Mapper*, función pura |
| `decodeSyncResponse(body)` | Texto JSON de un 200 → `SyncResponse?` (`null` = no se puede leer; no lanza) | *Mapper*, función pura |
| `HttpProgressGateway(client, baseUrl)` | Adaptador Ktor del puerto: `POST /api/progress/sync` con `Authorization: Bearer`. Solo HTTP: el JSON es de los mappers; las excepciones van por `callServer` | Adaptador (*Adapter*); `callServer` es una función de orden superior |

Paquete `app/.../data/progress/` (Room, solo Android; desde 7a–7c, conectado a `AppContainer` en 7d a través de `ProgressModule`):

| Pieza | Qué es | Patrón |
|---|---|---|
| `EnrollmentEntity` | Tabla `enrollments(user_id, course_id, enrolled_at, pending)`, clave primaria `(user_id, course_id)` | Entidad de persistencia (fila de tabla) |
| `MasteredModuleEntity` | Tabla `mastered_modules(user_id, course_id, module_id, mastered_at, pending)`, clave primaria `(user_id, course_id, module_id)` y clave foránea `(user_id, course_id)` → `enrollments` (sin `onDelete`: nunca se borra). La clave foránea es el principio de la clave primaria, así que el índice de la clave primaria la cubre (sin índice aparte ni avisos de Room) | Entidad de persistencia |
| `InstantConverter` | `Instant` ↔ `Long` en nanosegundos desde 1970: exacto y conserva el orden (`MIN` = la más antigua). Rango: 1677-09-21T00:12:43.145224192Z a 2262-04-11T23:47:16.854775807Z; fuera, `IllegalArgumentException` | *Adapter* (*TypeConverter*), funciones puras |
| `ProgressDao` | Insertar y leer (7a); `insert…IfAbsent` (`INSERT OR IGNORE`) y los `Flow` `watchEnrolled` / `watchMasteredIds` (7b); para sincronizar (7c), `pendingRows` y `settle`, dos métodos `@Transaction` con cuerpo que usan seis consultas (`enrollmentsToUpload`, `pendingModules`, `unqueue…`, `keepOlder…Date`). Nunca `DELETE` | *DAO* (*Data Access Object*); *Unit of Work* (`@Transaction`) |
| `ProgressRows(enrollments, modules)`, `ProgressSnapshot.toRows(userId)` y `ProgressRows.toSnapshot()` | Las filas de las dos tablas y su traducción a la forma del contrato. `toRows` pone el usuario y `pending = false`; `toSnapshot` mete cada módulo en la inscripción de su mismo usuario y curso (un módulo sin ella se queda fuera) | *Mapper*, funciones puras |
| `RoomProgressStore(dao, sessions, clock)` | Implementa **los tres puertos** (`ProgressStore`, `EnrollmentStore` y `ProgressSyncStore`) con Room, igual que `InMemoryProgressStore` | *Adapter* de los puertos; *Interface Segregation*; inyección de dependencias (`ProgressDao`, `SessionStore`, `Clock`) |
| `ProgressModule(database, sessions, gateway)` | Crea **un** `RoomProgressStore` y lo expone como `progressStore`, `enrollmentStore` y `syncStore`: los tres son el mismo objeto. `sync` es `ProgressSync(store, gateway)` con **ese mismo** objeto (C8a) y `sessionSync` es `SessionProgressSync(sessions, sync)` (13b-1): lo fusionado se ve por los otros puertos y el 401 borra la sesión. Recibe la base ya creada (y no el `Context`) para probarlo con una base en memoria | *Facade*; inyección de dependencias |
| `ProgressSyncRequester` | `fun interface` con `requestSync()` (no `suspend`): pedir sincronizar sin esperar al resultado. Es lo que reciben los ViewModels (8b); en sus tests basta una lambda | *Command* |
| `ProgressSyncWorker` (`CoroutineWorker`) (13b-1) | Ejecuta `sessionSync.run()` y traduce `backgroundSyncDecision(result, runAttemptCount)` a `Result` (`Done` → `success`, `Retry` → `retry`, `GiveUp` → `failure`). Constructor público de 2 argumentos usa `AppContainer.sessionProgressSync`; constructor `internal` + `ProgressSyncWorker.factory(sessionSync)` permiten probarlo sin `AppContainer`. No captura excepciones (errores de almacén pasan a `FAILED` en WorkManager; cancelación se propaga) | *Worker*; *Factory method* (`factory`) |
| `WorkSyncRequester(workManager)` (13b-1) | Implementa `ProgressSyncRequester` encolando trabajo único `"progress-sync"` (`ExistingWorkPolicy.REPLACE`, `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL` de 30 s) | *Command*; *Adapter* sobre WorkManager |
| `ProgressDatabase` y `create(context, name = DATABASE_NAME)` | Versión 2 (desde 12b-1: `enrollments`, `mastered_modules` y `course_copies`), `exportSchema = false`, `fallbackToDestructiveMigration(dropAllTables = true)` (C-4, D-3; se congela en la tarea 14). El fichero es `gradus-progress.db`; los tests pasan otro nombre | *Factory method* |

Paquete `app/.../data/course/` (copias locales de cursos en Room; solo Android, desde 12b-1):

| Pieza | Qué es | Patrón |
|---|---|---|
| `CourseCopyEntity` | Tabla `course_copies(course_id, json)`, clave primaria `course_id`. Sin `user_id` ni clave foránea a `enrollments`: las copias son globales al dispositivo | Entidad de persistencia |
| `CourseCopyDao` | `save(copy)` con `@Insert(onConflict = OnConflictStrategy.REPLACE)` (compatible con SQLite 3.22 / `minSdk` 29) y `loadJson(courseId): String?` | *DAO* |
| `RoomCourseCopyStore(dao)` | Implementa `CourseCopyStore` sobre `CourseCopyDao`. En `AppContainer`, `courseGateway` es `CachingCourseGateway(OfflineCourseGateway(http, RoomCourseCopyStore))` | *Adapter* del puerto |

Paquete `app/.../data/session/` (la sesión guardada cifrada; desde 11b, conectada a `AppContainer` en
11c-1):

| Pieza | Qué es | Patrón |
|---|---|---|
| `SessionCipher` | `encrypt(bytes)` y `decrypt(bytes)`; `decrypt` lanza `GeneralSecurityException` si no se puede descifrar (otra clave, byte cambiado, muy corto o sin clave). Los fallos del sistema (`ProviderException` de Keystore, `IOException` de `KeyStore.load`) salen tal cual | *Strategy* |
| `KeystoreSessionCipher(alias = DEFAULT_ALIAS)` | AES-256-GCM con una clave de Android Keystore (no sale del sistema). Formato: IV de 12 bytes + cifrado + etiqueta de 16. **Solo `encrypt` crea la clave**; buscar o crear va dentro de un cerrojo compartido por todas las instancias (generar con un alias existente sustituye la clave). La clave se busca en cada operación | Implementación de la *Strategy* |
| `EncryptedSessionSerializer(cipher)` | `Serializer<Session?>` de DataStore: `encodeSession` + cifrar al escribir, descifrar + `decodeSession` al leer. «Sin sesión» = fichero vacío. No se puede descifrar o no es una sesión → `CorruptionException` con texto fijo (nunca el token). Una `IOException` del disco **sale tal cual** (no borra una sesión buena) | *Adapter* |
| `DataStoreSessionStore(dataStore)` y `create(context, scope, fileName, cipher)` | Implementa `SessionStore` sin cambiar el puerto; además `session: Flow<Session?>` (cada cambio). Fichero `gradus-session.bin` en `noBackupFilesDir`; `ReplaceFileCorruptionHandler { null }`: fichero roto o clave perdida = sin sesión. **Un solo `DataStore` activo por fichero** (dos lanzan): la app crea uno; los tests cancelan el ámbito antes de abrir otro y usan alias propios, nunca `DEFAULT_ALIAS` | *Adapter* del puerto; *Factory method* |

En `app/.../ui/navigation/` (el arranque según la sesión guardada; 11c-2 y 11c-3):

| Pieza | Qué es | Patrón |
|---|---|---|
| `StartState` (`Loading \| SignedIn \| SignedOut`) | Qué sabe la app de la sesión guardada. Sin datos: el token no puede salir por aquí | Jerarquía sellada (tipo suma) |
| `StartViewModel(sessions: Flow<Session?>, progressSync: ProgressSyncRequester, retryDelay = 2 s)` | `state: StateFlow<StartState>` (`Eagerly`, empieza en `Loading`). Si el primer estado resuelto tras `Loading` es `SignedIn`, llama a `progressSync.requestSync()` una sola vez (13b-2); en `SignedOut → SignedIn` tras Login o tras un error pasajero de lectura no lo llama (no pisa a `LoginViewModel`). Si leer lanza `IOException` (incluida `CorruptionException`) o `ProviderException`: `SignedOut` sin borrar nada y vuelve a leer cada 2 s (un `Flow` que falla muere). Otro error no se traga. **Consecuencia:** un error pasajero dentro de `Main` lleva a Login aunque la sesión siga guardada | *Observer* (`StateFlow`); `retryWhen` |
| `StartNavigation.kt`: `startRoute()`, `mustLeaveMain(inMain)`, `isInMain()` y `NavController.leaveMain()` | `Loading` → sin ruta (solo el fondo); `SignedIn` → `Route.Main`; `SignedOut` → `Route.Welcome`. **Regla sobre el estado, no sobre el cambio:** `SignedOut` estando dentro del grafo `Main` → `leaveMain()`, que vacía la pila y deja `[Welcome, Login]` (como el flujo normal). Vale con la app abierta y cuando Android restaura la pila en `Main` sin sesión; con `Loading` no se sale | Funciones puras (extensiones) |
| `GradusNavHost(start, …)` y `MainActivity` | `MainActivity` crea `StartViewModel(AppContainer.sessionUpdates, AppContainer.progressSync)` (`by viewModels`) y pasa `state` con `collectAsStateWithLifecycle()` y `progressSync = AppContainer.progressSync`. `GradusNavHost` pinta un `Box` con el fondo del tema en `Loading`; la ruta inicial se fija **una vez** con el primer estado no `Loading` (si cambiara, el `NavHost` recrearía el grafo y vaciaría la pila); `rememberNavController()` restaura la pila aunque el `NavHost` llegue después. Al iniciar sesión, `enterHome()` espera a `start == SignedIn` (si no, un fotograma de desfase devolvería a Login) | *State hoisting*; la navegación solo en `GradusNavHost` |

Tests: `EncryptedSessionSerializerTest` (JVM, con `FakeSessionCipher`); `KeystoreSessionCipherTest`,
`DataStoreSessionStoreTest`, `DataStoreSessionStoreFileTest` y `DataStoreSessionStoreFailureTest`
(instrumentados, con `SessionStoreRule`).

Fallos del sistema al leer la sesión (`ProviderException` de Keystore, `IOException`; DataStore envuelve
esta en otra `IOException` con la original como causa): salen de `current()` y del `Flow`, **no borran**
la sesión y, si son pasajeros, el mismo `DataStore` vuelve a leer y a guardar. Si no pasan, `save` también
lanza (`updateData` lee antes de escribir) y `LoginViewModel` no lo captura: iniciar sesión cerraría la
app. Muy improbable; conocido y aceptado (11c-1).

Las fechas son `kotlin.time.Instant` (estable, sin `@OptIn`), nunca texto. El `Clock` se inyecta
(`Clock.System` por defecto); los tests usan un `FakeClock`.

Cómo está hecho `InMemoryProgressStore`:

- **Estado:** un `MutableStateFlow<Map<String, UserProgress>>`; la clave es `session.account.id`.
- **Lo dominado está anidado en su inscripción** (`CourseProgress.mastered`), como en el contrato.
  El estado «dominados sin inscripción» no se puede representar.
- **Escribir** (`enroll`, `markMastered`): primero se lee la sesión y la hora (`current()` es
  `suspend`, y la lambda de `update` puede repetirse, así que debe ser pura); después, un
  `update` atómico aplica la transformación. Si la lambda lanza, el valor no cambia: no queda nada.
- **Leer** (`mastered`, `enrolled`): un `flow { … }` que lee la sesión **al empezar a escuchar**.
  Sin sesión emite el vacío (`emptySet()` / `false`) y termina. Con `distinctUntilChanged` no avisa
  de cambios en otros cursos.
- **Lo pendiente es estado interno**, no va en `Enrollment` ni en `MasteredModule`:
  `CourseProgress.enrollmentPending` y `CourseProgress.pendingModules` (ids, siempre ⊆
  `mastered.keys`). `enroll` y `markMastered` lo marcan; lo que llega del servidor entra sin marcar.
  En Room serán una columna `pending` en la tabla de inscripciones y otra en la de dominados.
- **`settle`** es `update { … settled(sent, remote) }`: una sola transformación pura, atómica.
  Como cambia el mismo `StateFlow`, quien escucha `mastered` o `enrolled` se entera de lo fusionado.
- `progressOf(userId)` es `internal` y de solo lectura: existe para que los tests vean todo el estado
  con sus fechas (los puertos no las exponen, y `pending` solo enseña lo pendiente). Se mantiene.

Cómo está hecho `RoomProgressStore` (7b y 7c):

- **Estado:** las tablas `enrollments` y `mastered_modules`; cada consulta filtra por
  `user_id = session.account.id`.
- **Escribir:** se lee la sesión (sin sesión, `IllegalStateException` como en memoria) y después se
  inserta con `INSERT OR IGNORE` y `pending = true`. Si la fila ya existía no se toca: se conservan la
  primera fecha y su `pending`.
- **`markMastered`** comprueba la inscripción (`dao.enrollment`) **antes** de insertar y lanza la
  `IllegalStateException` del puerto (con el `courseId`, nunca el token). La clave foránea es la segunda
  capa: `OR IGNORE` no ignora las violaciones de clave foránea, así que nunca queda un dominado suelto.
- **Sin transacción:** no hay baja (decisión 7), así que la inscripción comprobada sigue ahí al
  insertar; dos `INSERT OR IGNORE` de la misma fila a la vez dejan una sola.
- **Leer:** un `flow { … }` que lee la sesión **al empezar a escuchar** y luego emite el `Flow` de Room
  con ese `userId`. Sin sesión emite el vacío y termina sin abrir la consulta. Room vuelve a consultar
  con cualquier cambio en la tabla (otro curso, otro usuario): `distinctUntilChanged` no deja pasar el
  valor repetido.
- **`pending(userId)`** (no lee la sesión: decisión 11) = `dao.pendingRows(userId).toSnapshot()`. Dos
  lecturas en una transacción (las dos ven el mismo estado):
  - las inscripciones del usuario con `pending = 1` **o** con `EXISTS` algún módulo suyo con `pending = 1`;
  - los módulos del usuario con `pending = 1`.
  Así un módulo pendiente va dentro de su inscripción aunque ya estuviera subida, y solo con los pendientes.
- **`settle(userId, sent, remote)`** = `dao.settle(sent.toRows(userId), remote.toRows(userId))`, **una sola
  transacción** (`@Transaction` en un método con cuerpo del DAO; sin `room-ktx`), en este orden:
  1. por cada inscripción y cada módulo de `sent`, `UPDATE … SET pending = 0 WHERE` su clave (usuario,
     curso; usuario, curso, módulo): por clave, nunca por fecha; si no existe, no cambia nada;
  2. por cada inscripción de `remote` y **después** cada módulo (por la clave foránea), `INSERT OR IGNORE`
     con `pending = 0` y `UPDATE … SET fecha = MIN(fecha, :remota) WHERE` su clave (la más antigua; el
     `pending` de lo que ya estaba no cambia).
  Nunca `DELETE`. Idempotente (repetir cada sentencia da lo mismo). Si algo lanza a mitad, se deshace
  todo. Los `Flow` emiten lo fusionado porque Room avisa al terminar la transacción.
- **Sin UPSERT** (`INSERT … ON CONFLICT DO UPDATE`): necesita SQLite 3.24 y Android 10 (`minSdk` 29) trae
  la 3.22. Tampoco `@Upsert` de Room: reescribe la fila entera, `pending` incluido.
- **Tests** (`app/src/androidTest/.../data/progress/`, base en memoria): `RoomEnrollmentTest`,
  `RoomMasteryTest`, `RoomMasteryRequiresEnrollmentTest`, `RoomNoSessionTest`, `RoomProgressPerUserTest`,
  `RoomProgressFlowTest` y `ProgressDaoInsertIfAbsentTest` (7b); `RoomPendingTest`, `RoomSettleQueueTest`,
  `RoomSettleMergeTest`, `RoomSettleFlowTest`, `RoomSettleAtomicityTest` y `RoomProgressSyncTest`
  (`ProgressSync` con Room y `FakeProgressGateway`) (7c); soporte en `RoomProgressTestSupport.kt`
  (`RoomProgressRule`, `TestClock`, `listenTo`) y `RoomSyncTestSupport.kt` (`snapshot`, `enrollment`,
  `assertSameSnapshot`, `allRows`). En la JVM, `ProgressRowsTest` (`app/src/test`). El todo o nada se
  prueba con una fecha remota de 2300 en el último módulo: `InstantConverter` lanza al guardarla, a mitad
  de `settle` (quitando el `@Transaction`, esos tests fallan). Los `Flow` se esperan con un `Channel` (`receive()`), sin
  `delay`; «no avisa» se prueba cambiando otra cosa, esperando a un segundo oyente de esa otra cosa
  (barrera) y comprobando que lo siguiente que llega es lo propio.

Cómo está hecho `ProgressSync`:

- Tres pasos y nada más: `sent = store.pending(userId)`, `gateway.sync(session, sent)` y, **solo si es
  `Synced`**, `store.settle(userId, sent, response.remote)`. Devuelve el desenlace con `also`.
- `userId = session.account.id`: la sesión llega por parámetro (decisión 11), no se lee de `SessionStore`.
- Sin `Mutex` ni `NonCancellable` (B-11): lo explican los invariantes de la sincronización.

## Flujo

Inscribirse → estudiar → dominar → sincronizar.

1. **Inscribirse:** `EnrollmentStore.enroll(courseId)`. Sin error si ya estaba inscrito (se queda la
   primera fecha). Sin sesión lanza `IllegalStateException`.
2. **Estudiar:** la interfaz lee `enrolled(courseId)` y decide con `studyAccess(enrolled)`.
   `NotEnrolled` → no se ofrece estudiar ni practicar. Funciona sin red: todo sale del almacén local.
3. **Dominar:** `ProgressStore.markMastered(courseId, moduleId)` guarda el módulo con la fecha del
   `Clock`. Dominar dos veces conserva la primera fecha. Sin inscripción o sin sesión lanza
   `IllegalStateException` y no escribe nada.
4. **Sincronizar:** `ProgressSync(store, gateway).sync(session)`, con la sesión actual por parámetro.
   1. `sent = pending(userId)`, con `userId = session.account.id`: cada inscripción pendiente y cada
      módulo pendiente, este dentro de su inscripción aunque ya estuviera subida (el contrato no admite
      módulos sueltos).
   2. `gateway.sync(session, sent)` **siempre**, también con `sent` vacío (B-10: envía
      `{"enrollments":[]}` y así llega lo dominado en otro móvil). Un solo intento. Mientras viaja, el
      estudiante puede seguir inscribiéndose y dominando.
   3. Tras un 200: `settle(userId, sent, remote)`, en una sola operación:
      - todo lo de `sent` sale de la cola **por clave** (curso; curso + módulo), nunca por fecha. Lo
        omitido por el servidor (`skipped`) también sale y se conserva en el móvil (B-8);
      - `remote` (todo el progreso del usuario en el servidor) se fusiona: unión, gana la fecha más
        antigua, lo nuevo no queda pendiente, nada local se borra.
      Lo hecho mientras la petición viajaba no está en `sent`: sigue pendiente.
   4. Si no hay 200 (`SessionExpired`, `Unreachable`, `ServerFailure`), no se llama a `settle`: el
      almacén no cambia y todo sigue pendiente.
   5. `sync` devuelve el desenlace del gateway tal cual; qué hacer con él lo decide quien llama (ver
      «Cómo extenderlo»).

## Invariantes

Reglas que nunca se rompen (p. ej. no hay módulo dominado sin inscripción).

- `studyAccess` solo mira la inscripción; no recibe el estado del módulo (B-6). La combinación con
  `ModuleState` ya no es cosa de la interfaz: la hace `Course.tapModule` (C9a), donde «sin
  inscripción» va antes que «bloqueado» (C-6).
- **No existe un módulo dominado sin inscripción.** Lo impone la estructura (`CourseProgress`
  contiene lo dominado) y `UserProgress.masteredIn`, que lanza `IllegalStateException` si el curso
  no está inscrito. El mensaje lleva el `courseId`, nunca el token.
- **Un fallo no deja nada escrito:** la comprobación va dentro de la lambda de `update`.
- **El progreso es por usuario** (clave: `account.id`). Cambiar de sesión no mezcla ni pierde datos.
- **Sin sesión:** leer da vacío/`false` y el `Flow` termina; escribir lanza `IllegalStateException`
  (fallo de programación, no un caso de usuario).
- **La primera fecha se conserva:** inscribirse o dominar otra vez no cambia ninguna fecha.
- **No hay baja** (decisión 7): por eso comprobar la inscripción y escribir después es seguro.
- **Un módulo pendiente está dominado:** `pendingModules ⊆ mastered.keys` (los tests lo comprueban
  donde se marca y donde se fusiona).
- **La cola se vacía por clave, nunca por fecha** (el servidor redondea a microsegundos). Lo que
  `sent` tenga y el móvil no, se ignora.
- **La fusión es unión y gana la fecha más antigua**, en los dos sentidos. Nunca borra nada local ni
  cambia la marca de pendiente de lo que ya estaba: lo pendiente que también llega del servidor sigue
  pendiente (reenviarlo es inofensivo).
- **Fechas en el futuro:** si el reloj del móvil va adelantado, su fecha es más tardía que la que el
  servidor recorta a su hora (regla 8). Al fusionar gana la más antigua, así que el móvil se queda con
  la del servidor: los dos acaban con la misma fecha.
- **`settle` es idempotente:** dos veces con lo mismo deja el mismo estado.
- **`ProgressSync` solo llama a `settle` tras `Synced`, y con el `sent` que de verdad envió.** Con
  `SessionExpired`, `Unreachable` o `ServerFailure` el almacén queda igual (cola, fechas y nada nuevo).
- **Un solo intento por `sync`:** ni tras un 401 ni tras un fallo de red. Reintentar es cosa de quien
  llama (parte D).
- **`sync` nunca lanza por la red ni por el servidor** (llega como `SyncOutcome`). Si falla el almacén
  (p. ej. Room), su excepción sí sale; la cancelación también se propaga.
- **Dos `sync` a la vez no pierden ni deshacen nada** (B-11, sin `Mutex`): cada una solo saca de la
  cola **su** `sent`, por clave, y la fusión es unión con la fecha mínima, así que una respuesta antigua
  que llega tarde no borra lo nuevo. El coste es una llamada repetida.
- **Una cancelación antes de `settle` solo provoca un reenvío inofensivo:** `settle` es todo o nada,
  así que todo sigue pendiente; la siguiente `sync` lo reenvía y el servidor lo fusiona (fecha mínima).
- **Quien escucha conserva su usuario:** `mastered` y `enrolled` leen la sesión al empezar; si luego
  entra otro usuario, quien ya escuchaba no se entera de lo suyo. La interfaz debe volver a escuchar
  al cambiar de sesión.
- **Cualquier otra implementación de los puertos** (Room, en la parte C) tiene que cumplir todo lo
  anterior; los comentarios de `ProgressStore` y `EnrollmentStore` lo dicen.

## Contrato ↔ DTO

Cómo pasa cada campo y cada respuesta de `POST /api/progress/sync` al modelo de la app.

Código: `course/progress/http/` (`ProgressDtos.kt` y `ProgressDtoMappers.kt`). Solo traduce; el HTTP, el
estado y los desenlaces son del gateway (`HttpProgressGateway`, mismo paquete).

**Petición** (`encodeSyncRequest`):
- Todos los campos se escriben, también `"mastered":[]`. Los DTO **no tienen valores por defecto** y el
  `Json` lleva `encodeDefaults = true` (refuerzo: si alguien añade un valor por defecto, no deja de
  escribirse y el servidor no responde 400).
- Las fechas van con `Instant.toString()`: UTC con `Z`. Con nanosegundos, se escriben todos; el servidor
  redondea a microsegundos.
- Lo dominado va dentro de su inscripción (la forma de `ProgressSnapshot`).

**Respuesta 200** (`decodeSyncResponse`):

| Contrato | En la app |
|---|---|
| `enrollments[]` (`courseId`, `enrolledAt`, `mastered[]`) | `SyncResponse.remote` (`ProgressSnapshot`); `courseId` tal cual llega |
| Fechas con 0, 3 o 6 decimales | `Instant.parse` las acepta todas |
| `skipped[]` sin `moduleId` **o con `"moduleId": null`** | `SkippedProgress.WholeEnrollment(courseId, code)` |
| `skipped[]` con `moduleId` (también `""` y `" "`) | `SkippedProgress.SingleModule(courseId, moduleId, code)` |
| `code` | Texto: la app no depende de los códigos |
| Campos desconocidos | Se ignoran en todos los niveles |
| Falta un campo obligatorio, vale `null`, tipo equivocado o fecha inválida | `null` (no se puede leer) |
| JSON roto, cuerpo vacío o no JSON | `null` |

- Obligatorio es todo salvo `moduleId` en `skipped`. Estricto en `enrollments`; tolerante con lo
  desconocido.
- `"moduleId": null` se lee como la inscripción entera (B-9): `skipped` es solo informativo (B-8) y
  hacer ilegible la respuesta por un campo que nadie usa impediría `settle` y vaciar la cola.
- `decodeSyncResponse` captura `IllegalArgumentException` (de ella heredan `SerializationException` y
  el error de fecha): nunca sale una excepción hacia fuera.
- Quien use el resultado no debe emparejar lo enviado con lo recibido por fecha (el servidor redondea):
  se empareja por clave (`settle`).

**Estados → desenlaces** (`HttpProgressGateway`; mira primero el estado y solo lee el cuerpo en el 200):

| Respuesta o fallo | `SyncOutcome` |
|---|---|
| 200 con cuerpo legible | `Synced(SyncResponse)` |
| 200 con cuerpo ilegible (`decodeSyncResponse` da `null`) | `ServerFailure` |
| 401 (el cuerpo no se lee) | `SessionExpired` |
| 400 `MALFORMED_BODY` (error de la app) | `ServerFailure` |
| 413 en `text/plain` (`Content Too Large`) | `ServerFailure` |
| 5xx y cualquier otro estado (404…) | `ServerFailure` |
| Sin red, servidor apagado o tiempo agotado (`IOException`) | `Unreachable` |
| Otra excepción (no de red) | `ServerFailure` |
| Cancelación de la corrutina | se relanza (no es un fallo) |

- **Petición:** `POST {baseUrl}/api/progress/sync`, cabecera `Authorization: Bearer <token>`, cuerpo
  `TextContent(encodeSyncRequest(pending), ContentType.Application.Json)`. `TextContent` es un cuerpo
  ya hecho: `ContentNegotiation` no lo serializa otra vez. El tipo va sin `charset` y Ktor lo codifica
  en UTF-8 sin BOM, que es lo que pide el contrato (lo comprueban los tests con los bytes reales).
- **El token nunca sale de la cabecera:** no está en ningún `toString` (`Session` lo oculta), ni en
  `SyncOutcome` (no guarda la sesión), ni en lo que recuerda el Fake (`SyncCall` guarda solo el `userId`).
- Un 401 (`SessionExpired`) no se reintenta con la misma sesión; `Unreachable` y `ServerFailure` dejan
  todo pendiente (decisión de `ProgressSync`, tarea 6).

## Cómo extenderlo

- **Un tipo de ejercicio nuevo:**
  1. Antes de ofrecerlo, lee `enrolled(courseId)` de `EnrollmentStore` y decide con
     `studyAccess(enrolled)`. Si da `NotEnrolled`, no lo abras (la interfaz pone el mensaje).
  2. Al dominar el módulo, llama a `ProgressStore.markMastered(courseId, moduleId)`.
  3. **No captures** la `IllegalStateException` de `markMastered`: es el invariante y avisa de un fallo
     de programación (se llegó a dominar sin comprobar `studyAccess`). Taparla escondería el fallo.
  4. Nunca escribas lo dominado por otro camino que no sea `markMastered`.
- **Inscribirse desde otra pantalla:** `EnrollmentViewModel(courseId, enrollmentStore)` (en
  `ui/screens/enrollment/`, con su `factory`) + `EnrollButton(state, onEnroll)` (en `ui/components/`).
  El ViewModel expone un `EnrollmentState` (`Unknown`, `NotEnrolled`, `Enrolling`, `Enrolled`) y el
  botón solo lo pinta: ninguno navega ni sabe en qué página están. No captures la
  `IllegalStateException` de `enroll` sin sesión (fallo de programación).
- **Otra forma de guardar el progreso (otra base de datos, otro servidor):** implementa
  `ProgressStore` y `EnrollmentStore` cumpliendo los invariantes de arriba (por usuario, nada escrito
  si falla, primera fecha, sin sesión). Para sincronizar, implementa también `ProgressSyncStore`:
  - Guarda lo pendiente como una columna (`pending`) en cada fila de inscripción y de módulo
    dominado, no en los *value objects*. `enroll` y `markMastered` ponen `pending = true` al insertar
    (y no lo tocan si la fila ya existía).
  - `pending(userId)`: las inscripciones con `pending` o con algún módulo `pending`, cada una con solo
    sus módulos `pending`.
  - `settle` en **una sola transacción** (`@Transaction`): poner `pending = false` por clave en lo de
    `sent` (sin mirar fechas; lo que no exista, se ignora), y después, por cada fila de `remote`,
    insertar con `pending = false` si no existe o, si existe, quedarse con la fecha mínima sin tocar
    `pending`. Nunca `DELETE`.
  - Los `Flow` de `mastered` y `enrolled` deben emitir tras `settle` (con Room, al leer de las mismas
    tablas, sale solo).
  - `ProgressSync` no cambia: se le pasa el almacén Room en lugar del de memoria.
- **Sincronizar desde la app (WorkManager, desde 13b):**
  1. `ProgressModule` crea `ProgressSync(store, gateway)` y `SessionProgressSync(sessions, sync)` sobre el
     **mismo** `RoomProgressStore` y el mismo `SessionStore`. `AppContainer` expone `sessionProgressSync`
     (para `ProgressSyncWorker`) y `progressSync: ProgressSyncRequester` (un `WorkSyncRequester` sobre
     `WorkManager.getInstance(requireContext())`).
     **Cómo se pide desde la app:** los ViewModels reciben `ProgressSyncRequester` por constructor (nunca
     `WorkSyncRequester` ni `AppContainer`) y llaman a `requestSync()` (sin `suspend`). `WorkSyncRequester`
     encola el trabajo único `"progress-sync"` (`REPLACE`, `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL`
     de 30 s), que ejecuta `ProgressSyncWorker` incluso con la app cerrada (deslizada desde Recientes).
     **Los cuatro momentos** (las fábricas de `MainNavGraph` y `MainActivity` → `GradusNavHost` pasan
     `AppContainer.progressSync`):
     - `StartViewModel`: una sola vez al arrancar si el primer estado resuelto tras `Loading` es `SignedIn`
       (nunca en `GradusApplication`, para no sustituir al trabajo que arrancó el proceso; ni en
       `SignedOut → SignedIn` tras Login, para no pisar a `LoginViewModel`).
     - `LoginViewModel`: justo después de `sessionStore.save` de un inicio de sesión correcto (trae lo
       dominado en otros móviles). Con un inicio fallido (credenciales, servidor, formulario no válido) no se pide.
     - `EnrollmentViewModel`: después de que `enroll` termine. Si el botón no actúa (`Unknown`,
       `Enrolling`, `Enrolled`) no se pide; si `enroll` lanza, tampoco.
     - `ExerciseViewModel`: después de `markMastered`, solo al aprobar la prueba de dominio (el mismo
       sitio donde se llama a `markMastered`), no en cada «Continuar»; si `markMastered` lanza, no.
  2. Qué hace `ProgressSyncWorker` con cada desenlace (mediante `SessionProgressSync` + `backgroundSyncDecision`):
     - `NoSession` o `Synced`: `Result.success()`; quien escucha `mastered` y `enrolled` ya ve lo fusionado.
       `skipped` es solo informativo (B-8): no hay que borrar ni reintentar nada.
     - `SessionExpired`: `SessionProgressSync` borra la sesión con `clearIfCurrent` y el worker devuelve
       `Result.failure()` (no reintenta). La interfaz observa `sessionUpdates` y, si está en `Main`, lleva a
       Iniciar sesión (`leaveMain`).
     - `Unreachable`: `Result.retry()` siempre (con espera exponencial y restricción `NetworkType.CONNECTED`).
     - `ServerFailure`: `Result.retry()` en los intentos `0..3` y `Result.failure()` en el 5.º (`attempt >= 4`).
  3. No captures excepciones alrededor de `sync` para «tapar» la red: no las lanza. Las del almacén (Room o
     lectura de sesión) salen de `doWork()` y WorkManager marca el trabajo como `FAILED` sin reintentar en bucle;
     la cancelación se propaga limpiamente y lo pendiente queda intacto.

## Qué cuidar al mergear

- **Gradle con Room y KSP (7a):** `gradle/libs.versions.toml` (`room`, `ksp`, `androidx-room-runtime`,
  `androidx-room-compiler` y el plugin `ksp`), el `build.gradle.kts` raíz (`alias(libs.plugins.ksp) apply false`)
  y `app/build.gradle.kts` (el plugin, `room-runtime`, `ksp(room-compiler)` y `kotlinx-coroutines-test`
  en `androidTestImplementation`). `dev/joel` también toca estos tres ficheros: al resolver conflictos,
  conserva las líneas de los dos lados.
- **Arranque según la sesión (11c-3):** `GradusNavHost` tiene un primer parámetro `start: StartState` y ya
  no empieza siempre en `Route.Welcome`; `MainActivity` crea el `StartViewModel`. Si otra rama toca
  `GradusNavHost` o `MainActivity`, conservar: el `Box` de `Loading` con su `return`, la ruta inicial con
  `remember(start == StartState.Loading)`, el `LaunchedEffect(leaveMain)` antes del `NavHost` y la condición
  `&& start == StartState.SignedIn` en la entrada de Login. Lo prueban `GradusNavHostStartTest` y
  `StartNavigationTest`.
- **Gradle con DataStore (11b) y WorkManager (13b-1):** `datastore = "1.3.0"` (`androidx-datastore`), `work = "2.12.0"` (`androidx-work-runtime` y `androidx-work-testing`) en `gradle/libs.versions.toml`, e `implementation(libs.androidx.datastore)`, `implementation(libs.androidx.work.runtime)` y `androidTestImplementation(libs.androidx.work.testing)` en `app/build.gradle.kts`.
- **`app/.../data/AppContainer.kt`, progreso y copias (desde 7d; WorkManager 13b; copias 12b-1):**
  `private var appContext: Context?`, `private fun requireContext()` y `fun init(context)` (solo cuenta la
  primera llamada). `private val progressDatabase by lazy { ProgressDatabase.create(requireContext()) }` es
  la **única** instancia de Room, compartida entre `progress` (`ProgressModule(progressDatabase, sessionStore,
  ProgressGateways.http(BASE_URL))`) y `courseGateway` (`CachingCourseGateway(OfflineCourseGateway(
  CourseGateways.http(BASE_URL), RoomCourseCopyStore(progressDatabase.courseCopyDao())))`) sin acoplar
  `courseGateway` a `sessionStore`. Encima de `progress`: `val progressStore: ProgressStore`,
  `val enrollmentStore: EnrollmentStore` y `val sessionProgressSync: SessionProgressSync` (tienen que salir
  del **mismo** `ProgressModule`; lo comprueba `AppContainerTest`). Desde 13b-2, `val progressSync:
  ProgressSyncRequester by lazy { WorkSyncRequester(WorkManager.getInstance(requireContext())) }`. La sesión
  (desde 11c-1): `private val sessions by lazy { DataStoreSessionStore.create(requireContext()) }` (**un solo
  objeto**: DataStore no admite dos activos sobre el mismo fichero), `val sessionStore: SessionStore get() =
  sessions` y `val sessionUpdates: Flow<Session?> get() = sessions.session` (solo para el arranque). Si
  `dev/joel` también toca `AppContainer`, conserva `init`, `requireContext`, `progressDatabase`,
  `courseGateway` (con `OfflineCourseGateway`), `progress`, `progressStore`, `enrollmentStore`,
  `sessionProgressSync`, `progressSync`, `sessions`, `sessionStore` y `sessionUpdates`. Todo ello (salvo
  `accountGateway`) necesita `init` (lo comprueban `AppContainerInitTest` y `AppContainerTest`).
- **`GradusApplication` y el manifiesto (7d):** `GradusApplication : Application` llama a
  `AppContainer.init(this)` en `onCreate`, y `AndroidManifest.xml` lleva `android:name=".GradusApplication"`
  en `<application>` (una sola línea añadida). Sin ella, usar el progreso o la sesión lanza
  `IllegalStateException` («AppContainer.init(context) must be called first»). El catálogo y la cuenta no
  necesitan `Context`. Crear el `RoomDatabase` no abre el fichero (lo hace la primera consulta), por eso los
  tests instrumentados pueden pedir `AppContainer.progressStore` sin tocar los datos.
- **Fábricas con parámetros nuevos (9b, 8b y 13b-2):** `CourseViewModel.factory(…, progress, enrollment)`,
  `ExerciseViewModel.factory(course, moduleId, progress, enrollment, progressSync, random)`,
  `EnrollmentViewModel.factory(courseId, enrollment, progressSync)`,
  `LoginViewModel.factory(accountGateway, sessionStore, progressSync)` y
  `StartViewModel.factory(sessions, progressSync)`. `GradusNavHost` recibe `progressSync` que rellena
  `MainActivity` con `AppContainer.progressSync`. Si `dev/joel` crea estos ViewModel o llama a
  `GradusNavHost` en otro sitio, tiene que pasar `AppContainer.enrollmentStore` y `AppContainer.progressSync`
  (si no, no se sincroniza nada y no hay error que lo avise).
- **`ExerciseViewModel` ya no se alcanza sin inscripción (9b):** el camino no abre la práctica sin
  inscripción (`tapModule`), y la entrada de `Route.Exercise` en `MainNavGraph` vuelve atrás según
  `exerciseViewModel.access` antes de componer `ScreenExercise`. Por eso `markMastered` ya no lanza en
  uso normal (y no se captura: si lanzara, sería un fallo de programación). Al resolver conflictos en
  ese bloque, conserva el `when (exerciseViewModel.access)`. Desde 10b se puede inscribir desde la
  página de relleno del curso.
- **Buscar abre `Route.CourseOverview` (10b):** en `MainNavGraph`, la pestaña Buscar navega con
  `Route.CourseOverview(id)` (no `Route.Course(id)`); desde esa página, «Ir al camino» navega a
  `Route.Course(id)`. Si `dev/joel` toca la navegación de Buscar, que conserve esa ruta (si no, nadie
  llega a inscribirse). El `EnrollmentViewModel` se crea dentro de la entrada `composable<Route.CourseOverview>`.
- **`CourseViewModel` (`mastered` y `enrolled`):** escucha los dos con `combine`; sin sesión ve el
  progreso vacío y sin inscripción; la sesión se lee al empezar a escuchar. `CourseUiState.enrolled`
  vale `false` por defecto (la preview de `ScreenCourse` no lo pasa).
- **`courseId` en mayúsculas:** `settle` compara el `courseId` tal cual, y el servidor lo devuelve en
  minúsculas (forma canónica). Si los ids de curso que usa el móvil vinieran en mayúsculas, el mismo
  curso aparecería dos veces tras sincronizar. **Comprobado en C (2026-11-25):** el móvil no inventa
  ids. La página de inscripción usa el `id` de la búsqueda (`CourseSummaryDto.id`) y el camino y la
  práctica el de `GET /api/courses/{id}` (`CourseVersionDto.id` → `Course.id`); los dos son el UUID
  que crea el servidor, que lo escribe en minúsculas. Que son el mismo id lo probó el 10b en el
  emulador (inscribirse en la página abre la práctica en el camino). Por eso no se normaliza. **Si
  algún día un id llega de otro sitio** (un enlace, un id escrito a mano), normalízalo antes de usarlo
  con los puertos. El curso de ejemplo (`estructuras-de-datos`) también va en minúsculas.
