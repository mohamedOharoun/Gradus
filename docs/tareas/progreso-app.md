# Progreso en la app (partes C y D: tareas 7–13)

Parte C y D del seguimiento del progreso (índice común: `docs/tareas/seguimiento-progreso.md`).
Referencia viva de cómo funciona lo que ya hay: `docs/arquitectura-progreso.md`. Lo terminado de la parte
C, con su detalle: `docs/tareas/progreso-app-historico.md` (no hace falta leerlo para continuar). Rama:
`dev/course-progress-tracking`.

Estado: **parte C terminada (tareas 7–10, 2026-11-26). Parte D (11–13) planificada y aprobada
(2026-11-26); 11a, 11b y 11c-1 hechos; siguiente paso: 11c-2.**

## Relevo

Se **reescribe** al final de cada sesión; no se acumula.

- **Dónde estamos (2026-11-26):** parte C terminada, revisada, probada en el emulador por los usuarios
  y en commits. La app deja inscribirse (página de relleno desde Buscar), no deja estudiar sin
  inscripción, guarda el progreso en Room por usuario y lo sincroniza tras iniciar sesión, inscribirse y
  dominar (`ProgressSyncLauncher`, sin enseñar nada). **Parte D:** decisiones D-1 a D-5 tomadas, criterios
  de 11–13 escritos y pasos aprobados (abajo, en «Tareas»). **11a y 11b hechos y revisados**:
  `encodeSession` / `decodeSession`, `clearIfCurrent` (por token) y `SessionProgressSync` en `sharedLogic`;
  DataStore 1.3.0 y `DataStoreSessionStore` cifrado (Keystore, AES-256-GCM) en `app/.../data/session/`,
  **todavía sin conectar** a `AppContainer` (piezas en la arquitectura).
- **Paso que toca:** 11c-2 (`StartViewModel`), en el mismo chat que hizo 11c-1, con la propuesta ya
  aprobada (ver los sub-pasos de 11c en «Tareas»). Después, 11c-3 (navegación y emulador). 11c-1 ya
  conectó el almacén cifrado a `AppContainer` (`sessionStore` y `sessionUpdates`).
- **Lo que hay que saber para seguir:**
  - El orden es 11a → 11b → 11c → 13a → 13b → 12a → 12b (D-5). 13 va antes que 12 porque necesita 11.
  - `SessionProgressSync` (11a) es el único sitio que trata el 401 (borra la sesión con `clearIfCurrent`).
    En 11c la interfaz solo reacciona a «la sesión se ha borrado»; quien la borra de verdad es el `Worker`
    de 13b. El 401 de punta a punta se prueba en 13b.
  - En 13b, `ProgressSyncLauncher` y `ProgressSyncLauncherTest` se **borran** (permiso del 2026-11-26) y
    `AppContainer` da a los ViewModels el `WorkSyncRequester` (los ViewModels no cambian).
  - En 12b, `ProgressDatabase` pasa a la versión 2: en desarrollo, el destructivo borra lo local una vez.
  - Gradle: 11b añade DataStore y 13b WorkManager, aprobados (D-1). Nada más sin permiso.
- **Abierto (para después, no es de D):** inscribirse desde el camino (C-6); la página definitiva del
  curso; «Mis cursos» con los cursos inscritos (D-3: sin ella, la copia solo se usa cuando Android
  restaura el camino); «cerrar sesión» (D-2); en la tarea 14, congelar el esquema de Room (C-4).
- **Riesgos:**
  - La rama `dev/joel` toca `AppContainer`, `CourseViewModel`, `ScreenCourse`, `MainNavGraph`,
    `libs.versions.toml` y `build.gradle.kts`: cada paso los toca lo mínimo. Lo que hay que conservar al
    mergear está en la arquitectura («Qué cuidar al mergear»).
  - Desde 11c-1 la sesión está en disco, pero hasta 11c-3 la app sigue empezando siempre en Bienvenida
    (no mira la sesión guardada al abrir).
  - Un fallo del almacén (Room) dentro de la sincronización cierra la app (no se captura, a propósito).
  - **Hallazgo de 11c (probado en `DataStoreSessionStoreFailureTest`):** con un fallo del sistema al leer la
    sesión (`ProviderException` o `IOException`) que **no pasa**, `save` también lanza (`updateData` lee antes
    de escribir), así que «al iniciar sesión, `save` sustituye» solo vale con `CorruptionException` (fichero
    roto o clave perdida). `LoginViewModel` no captura ese fallo: iniciar sesión cerraría la app. Con un
    fallo pasajero, leer y guardar vuelven a funcionar con el mismo `DataStore`, y un fallo no borra la
    sesión guardada. Muy improbable (Keystore ocupado un instante); no se arregla en 11c.
    DataStore envuelve la `IOException` en otra `IOException` («Inoperable file…») con la original como causa.
  - No se ha encontrado qué `compileSdk` pide WorkManager 2.12.0 (tenemos 37); si no vale, Gradle lo
    dirá en 13b (`checkAarMetadata`).
- **Cómo comprobar (aprendido en C):**
  - La sincronización no se ve en pantalla. Para comprobar que llega al servidor: dominar, `adb shell pm
    clear es.ulpgc.pamn.dominio` (borra solo la base local) y volver a iniciar sesión: lo dominado
    vuelve del servidor. Escribir en la base de desarrollo necesita permiso de los usuarios cada vez.
  - **WorkManager (13b):** cerrar la app deslizándola en recientes, **no** con `am force-stop`: una app
    detenida a la fuerza no ejecuta trabajos hasta que se vuelve a abrir.
  - Tests instrumentados: con el emulador caído, `connectedDebugAndroidTest` acaba en BUILD SUCCESSFUL
    sin ejecutar nada. Mirar siempre `app/build/outputs/androidTest-results/connected/debug/*.xml`
    (un solo fichero, con espacios en el nombre: contar `testcase name` y mirar el `timestamp`).
  - Leer un XML de resultados con `read_file`, o con un `grep` que imprima `<`/`>`, se cuelga en esta
    máquina: usar la herramienta de búsqueda con `-o` y un patrón sin etiquetas.
  - La herramienta de shell corta las órdenes largas. Lo que funciona (2026-11-26): lanzar `./gradlew …
    --console=plain > /tmp/log 2>&1` con `runInBackground` de la herramienta y esperar con
    `interact_shell` (`wait`); el `setsid nohup … &` muere a veces sin escribir nada (con `< /dev/null …
    & disown` aguantó). Leer el log con la herramienta de búsqueda y comprobar en
    `sharedLogic/build/test-results/testAndroidHostTest/` el `timestamp` y que están las clases nuevas. `--rerun` solo afecta a la tarea que va
    justo delante. No leer ni buscar ficheros con `cat`, `grep` o `sed` en el shell: usar las
    herramientas del IDE.
  - Las previews no enseñan las barras del sistema ni el fondo real: las pantallas nuevas se miran en el
    emulador (lo que se escapó en 10b).
  - Nombres de test con comas: prohibidos en `sharedLogic` (iOS), aceptados en `app` (JVM). En
    `androidTest`, con `_` (minSdk 29 no admite espacios).

## Objetivo

Que el estudiante se inscriba en un curso desde la app, estudie solo si está inscrito, que lo inscrito y
lo dominado se guarde en el móvil (Room, por usuario) y se sincronice con el servidor (parte C, hecha);
y que funcione sin conexión y se sincronice solo (parte D).

## Alcance

- **C (hecha):** 7 Room por usuario · 8 sincronizar con la app abierta · 9 la puerta en la interfaz ·
  10 botón de inscribirse (y su página de relleno).
- **D:** 11 sesión guardada cifrada · 12 copia local del curso al inscribirse · 13 WorkManager.

## Fuera de alcance

Lo del índice: darse de baja; registro de respuestas y SM-2; Inicio real; reanudar a mitad de un
módulo; actualizar la copia local con versiones nuevas; comprobar el SHA-256. Además: cambiar el
contrato (congelado) o el comportamiento de los puertos de la parte B (si hiciera falta, se propone y
se reabre B con un paso nuevo); la página definitiva del curso (la de C10 es de relleno); inscribirse
desde el camino (C-6); «cerrar sesión» y una lista de cursos inscritos en «Mis cursos» (D-2, D-3).

## Lo que ya hay y sirve de modelo

- **Progreso en el móvil** (`app/.../data/progress/`): `RoomProgressStore` (los tres puertos),
  `ProgressModule` (*Facade*: el almacén y `ProgressSync` sobre el mismo objeto), `ProgressSyncLauncher`
  y `ProgressSyncRequester`. Cómo son y cómo se comportan: la arquitectura.
- **Composition root:** `app/.../data/AppContainer.kt` (`object` con `lazy`, `init(context)` desde
  `GradusApplication`, `appScope` para lo que no debe cancelarse con una pantalla).
- **Puertos de `sharedLogic`:** `SessionStore` (con `InMemorySessionStore` como referencia de
  comportamiento), `ProgressStore`, `EnrollmentStore`, `ProgressSyncStore`, `ProgressSync`,
  `SyncOutcome`, `CourseGateway` y `CachingCourseGateway`.
- **ViewModels con factory** (`viewModelFactory { initializer { … } }`) que reciben puertos y la
  `fun interface` por constructor: `CourseViewModel`, `ExerciseViewModel`, `EnrollmentViewModel`,
  `LoginViewModel`.
- **Tests de ViewModel** en `app/src/test` con `kotlinx-coroutines-test`: `EnrollmentViewModelTest`,
  `LoginViewModelTest`, `ProgressSyncLauncherTest` (respuestas controladas con `CompletableDeferred`).
- **Tests de Room** en `app/src/androidTest/.../data/progress/` con `RoomProgressRule` (base en memoria) y
  `listenTo` (un `Channel` para esperar valores de un `Flow` sin `delay`).
- **Componentes:** `PrimaryButton`, `EnrollButton`, `PlaceholderScreen`, `FormTopBar`. **Previews:**
  `@LightDarkPreviews`. **Pantallas a pantalla completa:** `Surface(background)` + `safeDrawingPadding`
  (como `ScreenLogin` y `ScreenCourseOverview`).

## Decisiones de C (2026-11-25; siguen vigentes)

- **C-1 Dependencias.** Aprobadas y añadidas: Room 2.8.5 (`room-runtime`, `room-compiler` con KSP),
  KSP 2.3.14 y `kotlinx-coroutines-test` en `androidTestImplementation`. Sin driver extra (SQLite del
  sistema). Room 3 se descartó por ahora. Las de D, en D-1.
- **C-2 Orden de C:** 9 → 10 → 7 → 8 (cumplido).
- **C-3 `Context`:** `GradusApplication` llama a `AppContainer.init(this)` (manifiesto:
  `android:name=".GradusApplication"`). En `Application` y no en `MainActivity` porque el `Worker` de
  D13 arranca sin Activity.
- **C-4 Room por usuario:** tablas `enrollments` y `mastered_modules` con columna `pending`, clave
  foránea al curso inscrito y fechas en nanosegundos (`InstantConverter`). En desarrollo, versión 1 y
  `fallbackToDestructiveMigration(dropAllTables = true)`; **en la tarea 14 se congela** (exportar el
  esquema y quitar el destructivo). `courseId` sin normalizar (comprobado; ver la arquitectura).
- **C-5 Sincronizar:** tras iniciar sesión, tras inscribirse y tras dominar, con
  `ProgressSyncRequester` → `ProgressSyncLauncher` (ámbito de la app; sin sesión no llama).
  «Al volver la conexión» pasa a D13. Ningún desenlace enseña nada en C; **D11 lleva a iniciar sesión
  con `SessionExpired`** (cómo, en D-2). Lo que escucha el progreso vive dentro del grafo `Main` (se crea
  tras iniciar sesión): un futuro «cerrar sesión» debe sacar de `Main`.
- **C-6 Botón y página:** página de relleno `ScreenCourseOverview` desde Buscar, con `EnrollButton`
  (reutilizable, no navega) e «Ir al camino» (siempre activo, se borra con la página). La ruta lleva
  solo `courseId`. Sin inscripción, tocar un módulo enseña «Inscríbete para empezar a estudiar».
  Inscribirse desde el camino, para después.

## Decisiones de D (2026-11-26)

- **D-1 Dependencias.** Aprobadas: `androidx.work:work-runtime` 2.12.0 (estable; `minSdk` 24),
  `androidx.work:work-testing` 2.12.0 (solo `androidTestImplementation`) y `androidx.datastore:datastore`
  1.3.0 (el tipado, no Preferences). Descartada `androidx.security:security-crypto` (obsoleta, sin más
  versiones). DataStore frente a hacerlo a mano: da escrituras atómicas, una detrás de otra, el `Flow`
  de cambios y el tratamiento del fichero roto.
- **D-2 Sesión (11).** Se guarda la `Session` entera en JSON (traducción en `sharedLogic`), cifrada con
  AES-256-GCM y una clave de Android Keystore dentro del `Serializer` de DataStore (*Adapter*), en
  `noBackupFilesDir` (una copia de seguridad restaurada en otro móvil no tendría la clave).
  - Al abrir la app **no se mira `expiresAt`**: el servidor la alarga con cada uso (30 días deslizantes,
    tope de 90, `SessionPolicy`), así que la del móvil solo es una cota inferior. Con sesión guardada, a
    Inicio (también sin red); el servidor decide con un 401. Mientras se lee, solo el fondo (sin
    dependencia de *splash*).
  - Con un 401, `SessionProgressSync` borra la sesión con `clearIfCurrent` (solo si sigue siendo la misma:
    no echa a quien acaba de entrar). La app observa el `Flow` de la sesión y, si se borra estando en
    `Main`, va a iniciar sesión. El puerto `SessionStore` no cambia.
  - Sin «cerrar sesión» en D; para probar con otra cuenta, `pm clear`.
- **D-3 Copia del curso (12).** En Room (`course_copies` en `ProgressDatabase`, versión 2). **Red
  primero**; con `Unreachable`, la copia (con red, todo igual que hoy). `OfflineCourseGateway` guarda
  cada curso que descarga (sobrescribe); al inscribirse se descarga el curso. JSON con la forma de la
  API mediante `encodeCourseVersion` (inverso del mapper existente): no se toca `HttpCourseGateway` ni el
  puerto `CourseGateway`. **Sin entrada nueva sin red** (opción b): la copia se usa cuando Android
  restaura el camino o la práctica; una lista de inscritos en «Mis cursos», para después.
- **D-4 WorkManager (13).** Única forma de sincronizar: `WorkSyncRequester` implementa
  `ProgressSyncRequester` con un trabajo único `progress-sync` (`REPLACE`, `NetworkType.CONNECTED`,
  `EXPONENTIAL`). `ProgressSyncLauncher` y su test se borran (permiso dado). Reintentos
  (`backgroundSyncDecision`): sin sesión o `Synced`, terminado; `Unreachable`, reintentar siempre;
  `ServerFailure`, hasta 5 intentos; `SessionExpired`, rendirse. Con sesión guardada, se pide sincronizar
  al entrar en `Main`, no en `GradusApplication` (sustituiría al trabajo que ha arrancado el proceso).
- **D-5 Orden:** 11a → 11b → 11c → 13a → 13b → 12a → 12b.

## Tareas

La casilla la marca el orquestador tras revisar. Los tests nombrados son el **mínimo** (AGENTS §7).

### [x] Parte C: 7 Room · 8 sincronizar · 9 la puerta · 10 botón de inscribirse

Hechas y revisadas (2026-11-25/26). Pasos, modelos y detalle: `progreso-app-historico.md`.

### [ ] 11. Sesión guardada cifrada (parte D)

La sesión sobrevive a cerrar la app, cifrada en el móvil. Requisito de 13. *Acepta si:*

- Se guarda la `Session` entera (token, `expiresAt` y cuenta) en JSON, cifrada con AES-256-GCM y una
  clave de Android Keystore, con DataStore 1.3.0, en `noBackupFilesDir`. El fichero no contiene ni el
  token ni el correo en claro. La traducción `Session` ↔ JSON está en `sharedLogic` (test primero).
- La implementación de `SessionStore` no cambia el puerto: `save` sustituye, `current` da la guardada
  o `null`, `clear` la borra (sin sesión, no hace nada). Otro `DataStore` sobre el mismo fichero (como
  tras reiniciar la app) lee lo guardado.
- Un fichero roto o que no se puede descifrar equivale a «sin sesión»: no lanza, y después se puede
  guardar otra.
- Avisa de cada cambio con un `Flow<Session?>` (fuera del puerto).
- Al abrir la app: con sesión guardada, a Inicio, también sin red (no se mira `expiresAt`: el servidor
  la alarga con el uso; decide el 401); sin sesión, a Bienvenida. Mientras se lee, solo el fondo.
- Si la sesión se borra con la app en `Main`, se va a iniciar sesión y «atrás» no vuelve a `Main`.
- `SessionStore.clearIfCurrent(session)` borra solo si la guardada es esa misma sesión.
- `SessionProgressSync` (`sharedLogic`): sin sesión no llama al servidor; con `SessionExpired` borra la
  sesión si sigue siendo la misma que lo recibió; con los demás desenlaces no la toca. Lo dispara el
  `Worker` de 13 (el 401 de punta a punta se prueba allí).
- El token no sale en ningún log, `toString` ni mensaje de error.
- Si Android mata el proceso y restaura una pantalla de `Main`, hay sesión (el riesgo del Relevo).

Pasos:

- [x] **11a** (`sharedLogic`, test primero; Sonnet 5.5 · medio; revisado el 2026-11-26). Traducción `Session` ↔ JSON (lo ilegible
  da `null`, sin lanzar), `SessionStore.clearIfCurrent` y `SessionProgressSync` (`NoSession` |
  `Finished(outcome)`). Tests: ida y vuelta (con tildes y emojis); campos desconocidos; JSON roto, vacío
  o sin un campo → `null`; `clearIfCurrent` con la misma, con otra y sin sesión; `SessionProgressSync`
  sin sesión (no llama), `Synced` (no borra), `SessionExpired` (borra), `SessionExpired` tras guardar otra
  sesión mientras viajaba (no la borra), `Unreachable` y `ServerFailure` (no borra; sigue pendiente).
- [x] **11b** (app + Gradle: DataStore; Opus 5.5 · medio; revisado el 2026-11-26). `SessionCipher` + `KeystoreSessionCipher`,
  `EncryptedSessionSerializer`, `DataStoreSessionStore` con `create(context)`. Tests JVM del *serializer*
  con un cifrado falso; instrumentados del cifrado (ida y vuelta, IV aleatorio, byte cambiado → falla) y
  del almacén (guardar, sustituir, borrar, borrar sin sesión, otro `DataStore` sobre el mismo fichero
  tras cancelar el primero, fichero roto → `null` y luego guarda, sin token ni correo en claro, el `Flow`
  avisa).
- [ ] **11c** (app; Sonnet 5.5 · alto). `AppContainer` con el almacén nuevo; `StartViewModel` (`Loading` |
  `SignedIn` | `SignedOut` y el aviso de «sesión borrada»); `GradusNavHost` empieza según el estado.
  Tests: `StartViewModelTest` (`Loading` hasta el primer valor, con y sin sesión, con sesión → sin ella
  avisa una vez, sin sesión → con ella no avisa); `AppContainerTest` sigue en verde. Emulador: iniciar
  sesión, `am force-stop` y abrir → Inicio (también en modo avión); `pm clear` → Bienvenida; camino →
  fondo → `am kill` → volver: el camino con lo dominado. Partido en tres (aprobado el 2026-11-26):
  - [x] **11c-1** `AppContainer` con `DataStoreSessionStore` (`sessionStore` + `sessionUpdates`) y comentario
    de `SessionCipher` (revisado el 2026-11-26).
  - [ ] **11c-2** `StartState` + `StartViewModel` en `ui/navigation/`: error al leer (`IOException` o
    `ProviderException`) → `SignedOut` y reintento cada 2 s; el resto de errores no se traga.
  - [ ] **11c-3** `StartNavigation.kt` (funciones puras), `GradusNavHost` y `MainActivity`. Regla sobre el
    estado: `SignedOut` estando en `Main` → `leaveMain()` deja la pila `[Welcome, Login]` (sin tocar
    `enterHome` ni la flecha de Login). `Loading` = un `Box` con el fondo del tema (sin componente nuevo).
    Tests de restauración con `StateRestorationTester`, incluido girar tras iniciar sesión.

### [ ] 12. Copia local del curso al inscribirse (parte D)

Sin red, el camino y la práctica abren desde la copia del curso. *Acepta si:*

- `OfflineCourseGateway` (*Decorator* de `CourseGateway`, en `sharedLogic`) guarda cada curso que
  descarga (sobrescribe la copia anterior). Con `Unreachable`, devuelve la copia si la hay y se puede
  leer; si no, `Unreachable`. `NotFound`, `UnsupportedFormat`, `ServerFailure` y `search` pasan tal cual.
- La copia es JSON con la forma de `GET /api/courses/{id}` (`encodeCourseVersion`, inverso de
  `decodeCourseVersion`): leer lo escrito da el mismo `Course`, con todos los tipos de paso y ejercicio.
- Se guarda en Room: tabla `course_copies` en `ProgressDatabase`, versión 2 (en desarrollo, el
  destructivo borra una vez lo local; vuelve del servidor al sincronizar).
- Al inscribirse se descarga el curso (en el ámbito de la app), así hay copia aunque no se abra el camino.
- `AppContainer`: `CachingCourseGateway(OfflineCourseGateway(http, copias))`.
- Sin pantalla nueva (D-3): se prueba con el camino que Android restaura sin red tras `am kill`.

Pasos:

- [ ] **12a** (`sharedLogic`, test primero; Sonnet 5.5 · alto). `encodeCourseVersion`, `CourseCopyStore` +
  versión en memoria y `OfflineCourseGateway`. Tests: ida y vuelta de cada tipo de paso y ejercicio y del
  curso de ejemplo; `Found` guarda y otro `Found` sustituye; `Unreachable` con copia → `Found` igual; sin
  copia o ilegible → `Unreachable`; los demás desenlaces pasan y no usan la copia; `search` se delega.
- [ ] **12b** (app; Sonnet 5.5 · alto). Tabla y DAO, `RoomCourseCopyStore`, `ProgressDatabase` versión 2,
  la cadena en `AppContainer` y la descarga tras inscribirse (`EnrollmentViewModel` recibe una
  `fun interface`). Tests: instrumentados del almacén (guardar, leer, sobrescribir, desconocido → `null`);
  `EnrollmentViewModelTest` (pide descargar tras `enroll`; no si no actúa ni si `enroll` lanza).
  Emulador: inscribirse, abrir el camino, modo avión, fondo, `am kill`, volver: el camino sale de la copia
  y la práctica abre.

### [ ] 13. WorkManager (parte D)

Sincronización en segundo plano, también con la app cerrada. *Acepta si:*

- `backgroundSyncDecision` (función pura en `sharedLogic`): sin sesión o `Synced` → terminado;
  `Unreachable` → reintentar; `ServerFailure` → reintentar hasta el 5.º intento y después rendirse;
  `SessionExpired` → rendirse (la sesión ya está borrada).
- `ProgressSyncWorker` (`CoroutineWorker`) usa el `SessionProgressSync` de `AppContainer` (el mismo
  `ProgressSync` de `ProgressModule`) y traduce la decisión a `Result`.
- `WorkSyncRequester` implementa `ProgressSyncRequester`: trabajo único `progress-sync` con `REPLACE`,
  solo con red (`NetworkType.CONNECTED`) y espera creciente (`EXPONENTIAL`). Los ViewModels no cambian.
- `ProgressSyncLauncher` y su test se borran (permiso del 2026-11-26).
- Al abrir la app con sesión guardada se pide sincronizar al entrar en `Main` (no en
  `GradusApplication`: sustituiría al trabajo que ha arrancado el proceso).
- De punta a punta: dominar sin red y con la app cerrada, al volver la red sube solo; un 401 lleva a
  iniciar sesión.

Pasos:

- [ ] **13a** (`sharedLogic`, test primero; Sonnet 5.5 · bajo). `backgroundSyncDecision`. Tests: cada
  desenlace; `ServerFailure` en los intentos 0–3 reintenta y en el 4 (el 5.º) se rinde (`runAttemptCount`
  empieza en 0); `Unreachable` reintenta aunque sea el intento 100.
- [ ] **13b** (app + Gradle: WorkManager; Opus 5.5 · medio). `ProgressSyncWorker`, `WorkSyncRequester`,
  `AppContainer`, `MainActivity` y `MainNavGraph` con el requester nuevo, `StartViewModel` pide sincronizar
  al arrancar con sesión, y borrar `ProgressSyncLauncher` y su test. Tests: con `WorkManagerTestInitHelper`,
  un solo trabajo con red y espera creciente, dos peticiones dejan uno, sin red no corre y con las
  restricciones cumplidas sí; el `Worker` da el `Result` de cada decisión (quien implemente propone cómo
  probarlo sin `AppContainer`); `StartViewModel` pide sincronizar una vez con sesión y ninguna sin ella.
  Emulador (cerrar desde recientes, no `force-stop`): dominar en modo avión, cerrar, quitar el modo
  avión → sube solo (`dumpsys jobscheduler`; `pm clear` + iniciar sesión). 401: borrar la fila de la
  sesión en la base de desarrollo → abrir → iniciar sesión. **Las dos escriben en la base de desarrollo:
  pedir permiso.**

## Progreso

- 2026-11-25/26: parte C terminada (detalle en `progreso-app-historico.md`).
- 2026-11-26: parte D planificada: decisiones D-1 a D-5, criterios de 11–13 y pasos aprobados por los
  usuarios.
- 2026-11-26: **11a hecho y revisado** (`sharedLogic`, 25 tests nuevos; 771 en total, en verde). Nuevo: `account/storage/`
  (`StoredSessionDto`, `encodeSession`, `decodeSession`: lo ilegible da `null`, el token no sale en ningún
  `toString`; el texto de `encodeSession` sí lleva el token en claro, para cifrarlo en 11b),
  `account/SessionStoreExtensions.kt` (`clearIfCurrent`) y `course/progress/SessionProgressSync.kt` +
  `SessionSyncResult.kt`. Decisiones: `clearIfCurrent` compara **solo el token** (un 401 invalida el token
  sea cual sea su `expiresAt`); el DTO del disco es aparte del de la red; sin campo `version`. Límite
  conocido: `clearIfCurrent` lee y borra en dos pasos (el puerto no tiene uno atómico), así que una
  sesión guardada justo entre los dos se borraría; el caso realista (guardar mientras la petición viaja)
  está probado.
- 2026-11-26: **11b hecho y revisado** (orquestador: 9 JVM + 23 instrumentados en verde en esta pasada).
  DataStore 1.3.0 añadido. Nuevo en `app/.../data/session/`:
  `SessionCipher`, `KeystoreSessionCipher`, `EncryptedSessionSerializer` y `DataStoreSessionStore`
  (`create(context, scope = IO + SupervisorJob, fileName, cipher)`, `session: Flow<Session?>`). Todavía no
  está en `AppContainer` (11c). Lo que hay que saber: `DataStore<Session?>` sin envoltorio; «sin sesión»
  es un fichero vacío; descifrar o leer mal da `CorruptionException` → `null`, pero una IOException del
  disco **sale hacia fuera** (no borra la sesión): 11c decide qué hace la interfaz con ella. Solo
  `encrypt` crea la clave, buscar o crear va dentro de un cerrojo, y sin clave lo guardado se lee
  como `null`. Los tests usan alias propios, nunca `DEFAULT_ALIAS`. Tests: 9 JVM + 23 instrumentados, en
  verde. Revisión: el comentario de `SessionCipher` promete «nunca otra excepción», pero Keystore puede
  lanzar `ProviderException` (y `KeyStore.load`, `IOException`); salen tal cual, como la `IOException`
  del disco (no borran la sesión). Se corrige el comentario en 11c, que decide qué hace el arranque si
  leer la sesión lanza. El test de hilos no se ha visto fallar sin el cerrojo (una carrera no siempre
  ocurre): documenta, no demuestra.
- 2026-11-26: **11c-1 hecho y revisado** (orquestador: JVM 4 + 11 y 33 instrumentados en verde en esta pasada). `AppContainer`: `sessionStore` es ya
  el `DataStoreSessionStore` (un solo objeto, necesita `init`) y `sessionUpdates: Flow<Session?>` sale del
  mismo; comentario de `SessionCipher` corregido. Cambiado `AppContainerInitTest` (la sesión ya necesita
  `init`). Tests nuevos: 2 JVM del serializer (fallos del sistema salen tal cual), 2 de `AppContainerInitTest`,
  3 de `AppContainerTest` y `DataStoreSessionStoreFailureTest` (6). Verde: JVM de `app` y 136 instrumentados
  del paquete `data`. Queda 11c-2 (`StartViewModel`, en `ui/navigation/`) y 11c-3 (navegación y emulador).
  Nota: Gradle desde el sandbox del agente no tiene `JAVA_HOME` ni `~/.gradle` escribible; se lanza con
  `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` fuera del sandbox.

## Siguiente paso

11c-2 (`StartViewModel`): propuesta ya aprobada; tests primero.
