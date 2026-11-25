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
| `ProgressGateways.http(baseUrl)` | La única forma de obtener el gateway real desde fuera de `sharedLogic` (no está conectado a `AppContainer`: es C8) | *Factory method* |
| `FakeProgressGateway(answer)` y `SyncCall(userId, sent)` | Gateway sin red para los tests de `ProgressSync`: responde lo configurado y recuerda **el id del usuario** y lo enviado, nunca la `Session` (el token no queda guardado). `answer` es `suspend` (puede esperar a un `CompletableDeferred`: el test decide cuándo llega la respuesta) y la llamada se apunta antes de responder | *Test double* (fake) |
| `ProgressSync(store, gateway)` y `suspend sync(session): SyncOutcome` | Caso de uso de la sincronización: sube lo pendiente del usuario de la sesión, fusiona la respuesta y devuelve el desenlace del gateway tal cual | Caso de uso (*Interactor*) sobre dos puertos inyectados (*Strategy*) |

Paquete `course/progress/http/` (`internal`, traduce entre el modelo y el JSON; sin HTTP):

| Pieza | Qué es | Patrón |
|---|---|---|
| `ProgressDtos.kt` (`SyncRequestDto`, `EnrollmentDto`, `MasteredDto`, `SyncResponseDto`, `SkippedDto`) | El JSON de `POST /api/progress/sync`, campo a campo | DTO |
| `encodeSyncRequest(snapshot)` | `ProgressSnapshot` → texto JSON de la petición | *Mapper*, función pura |
| `decodeSyncResponse(body)` | Texto JSON de un 200 → `SyncResponse?` (`null` = no se puede leer; no lanza) | *Mapper*, función pura |
| `HttpProgressGateway(client, baseUrl)` | Adaptador Ktor del puerto: `POST /api/progress/sync` con `Authorization: Bearer`. Solo HTTP: el JSON es de los mappers; las excepciones van por `callServer` | Adaptador (*Adapter*); `callServer` es una función de orden superior |

Paquete `app/.../data/progress/` (Room, solo Android; desde 7a, **todavía sin conectar** a `AppContainer`: es 7d):

| Pieza | Qué es | Patrón |
|---|---|---|
| `EnrollmentEntity` | Tabla `enrollments(user_id, course_id, enrolled_at, pending)`, clave primaria `(user_id, course_id)` | Entidad de persistencia (fila de tabla) |
| `MasteredModuleEntity` | Tabla `mastered_modules(user_id, course_id, module_id, mastered_at, pending)`, clave primaria `(user_id, course_id, module_id)` y clave foránea `(user_id, course_id)` → `enrollments` (sin `onDelete`: nunca se borra). La clave foránea es el principio de la clave primaria, así que el índice de la clave primaria la cubre (sin índice aparte ni avisos de Room) | Entidad de persistencia |
| `InstantConverter` | `Instant` ↔ `Long` en nanosegundos desde 1970: exacto y conserva el orden (`MIN` = la más antigua). Rango: 1677-09-21T00:12:43.145224192Z a 2262-04-11T23:47:16.854775807Z; fuera, `IllegalArgumentException` | *Adapter* (*TypeConverter*), funciones puras |
| `ProgressDao` | Por ahora solo insertar y leer (`suspend`); las consultas de los puertos llegan en 7b y 7c | *DAO* (*Data Access Object*) |
| `ProgressDatabase` y `create(context, name = DATABASE_NAME)` | Versión 1, `exportSchema = false`, `fallbackToDestructiveMigration(dropAllTables = true)` (C-4; se congela en la tarea 14). El fichero es `gradus-progress.db`; los tests pasan otro nombre | *Factory method* |

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
- **Sincronizar desde la app (parte C; la D lo automatiza con WorkManager):**
  1. Crea `ProgressSync(store, gateway)` en `AppContainer` (C8) con el **mismo** objeto que implementa
     `ProgressStore` y `EnrollmentStore`, y el gateway de `ProgressGateways.http(baseUrl)`.
  2. Cuándo llamar a `sync(session)`: con la sesión actual (`SessionStore.current()`; si es `null`, no
     se sincroniza). Momentos razonables: al iniciar sesión o abrir la app (para traer lo de otros
     móviles, aunque no haya nada pendiente), tras inscribirse o dominar un módulo, y cuando el
     estudiante lo pida. Desde una corrutina del `ViewModel` (`viewModelScope`) o del `Worker`, nunca
     en el hilo principal bloqueándolo.
  3. Qué hacer con cada desenlace (los textos los decide la interfaz):
     - `Synced`: nada más; quien escucha `mastered` y `enrolled` ya ve lo fusionado. `skipped` es solo
       informativo (B-8): no hay que borrar ni reintentar nada.
     - `SessionExpired`: la sesión no vale. No reintentes con ella: lleva al estudiante a iniciar sesión
       (lo pendiente sigue guardado y se sube tras entrar).
     - `Unreachable`: sin conexión. Avisa si quieres y reintenta más tarde (WorkManager, parte D).
     - `ServerFailure`: fallo del servidor. Igual que `Unreachable`, pero no insistas en bucle.
  4. No captures excepciones alrededor de `sync` para «tapar» la red: no las lanza. Las del almacén son
     un fallo real. No hace falta evitar dos `sync` a la vez (B-11); si D quiere ahorrar llamadas, usa
     trabajo único de WorkManager o mira `pending(userId).isEmpty` (B-10).

## Qué cuidar al mergear

- **Gradle con Room y KSP (7a):** `gradle/libs.versions.toml` (`room`, `ksp`, `androidx-room-runtime`,
  `androidx-room-compiler` y el plugin `ksp`), el `build.gradle.kts` raíz (`alias(libs.plugins.ksp) apply false`)
  y `app/build.gradle.kts` (el plugin, `room-runtime`, `ksp(room-compiler)` y `kotlinx-coroutines-test`
  en `androidTestImplementation`). `dev/joel` también toca estos tres ficheros: al resolver conflictos,
  conserva las líneas de los dos lados.
- **`app/.../data/AppContainer.kt`, progreso (desde 9b):** un solo almacén con dos caras:
  `private val inMemoryProgress by lazy { InMemoryProgressStore(sessionStore) }` y, encima,
  `val progressStore: ProgressStore get() = inMemoryProgress` y
  `val enrollmentStore: EnrollmentStore get() = inMemoryProgress`. Tienen que ser **el mismo objeto**
  (si fueran dos, inscribirse no abriría nada). Si `dev/joel` también toca `AppContainer`, conserva
  las tres líneas y el `sessionStore` (declarado después, pero `lazy`: no importa el orden).
- **Factorías con un parámetro más (9b):** `CourseViewModel.factory(…, progress, enrollment)` y
  `ExerciseViewModel.factory(course, moduleId, progress, enrollment)`. Si `dev/joel` crea estos
  ViewModel en otro sitio, tiene que pasar `AppContainer.enrollmentStore`.
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
  curso aparecería dos veces tras sincronizar. La parte C debe comprobar que los ids del catálogo
  llegan en minúsculas (o normalizarlos al guardar).
