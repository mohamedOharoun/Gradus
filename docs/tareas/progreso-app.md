# Progreso en la app (partes C y D: tareas 7–13)

Parte C y D del seguimiento del progreso (índice común: `docs/tareas/seguimiento-progreso.md`).
Referencia viva de cómo funciona lo que ya hay: `docs/arquitectura-progreso.md`. Lo terminado, con su
detalle (la parte C y los pasos de D ya revisados): `docs/tareas/progreso-app-historico.md` (no hace falta
leerlo para continuar). Rama: `dev/course-progress-tracking`.

Estado: **parte C terminada (tareas 7–10). Parte D: tarea 11 terminada; tarea 13 hecha y revisada en código; paso 12a y subpaso 12b-1 terminados y revisados (2026-11-26). Siguiente paso: 12b-2 (último subpaso de código de D).**

## Relevo

Se **reescribe** al final de cada sesión; no se acumula.

- **Dónde estamos (2026-11-26):** parte C terminada y en commits. **Parte D:** decisiones D-1 a D-5,
  criterios de 11–13 y pasos aprobados (abajo, en «Tareas»). **Tarea 11 terminada y en commits.**
  **Tarea 13 y paso 12a hechos, revisados y en commits.** **Subpaso 12b-1 terminado y revisado** (a falta
  del commit de los usuarios): tabla `course_copies` (`CourseCopyEntity`, `CourseCopyDao`,
  `RoomCourseCopyStore` en `app/.../data/course/`), `ProgressDatabase` v2 y cadena
  `CachingCourseGateway(OfflineCourseGateway(http, RoomCourseCopyStore))` en `AppContainer` compartiendo
  una única instancia `progressDatabase` (sin acoplar `courseGateway` a `sessionStore`).
- **Paso que toca:** **12b-2** (propuesta ya aprobada; mismo chat o Gemini Neon · alto):
  `CourseCopyRequester`, `CourseCopyLauncher` sobre `appScope` (relanza `CancellationException`, captura
  `Exception` como precarga *best-effort*), `EnrollmentViewModel`, cableado quirúrgico en `MainNavGraph`,
  tests (`CourseCopyLauncherTest`, `EnrollmentViewModelTest`, `AppContainerInitTest`, `AppContainerTest`) y
  pasos de prueba en el emulador.
- **Pruebas de emulador al cierre de D (tras 12b):** las dos de la tarea 13 (modo avión cerrando desde
  Recientes + 401 borrando la fila de sesión) y la de 12b (inscribirse, abrir camino, modo avión, fondo,
  `am kill`, volver → el camino sale de la copia y la práctica abre). Todas escriben en la base de
  desarrollo: las ejecutan los usuarios.
- **Lo que hay que saber para seguir:**
  - El orden es 11a → 11b → 11c → 13a → 13b → 12a → 12b (D-5). Queda solo la tarea **12** (12a y 12b).
  - `SessionProgressSync` (11a) es el único sitio que trata el 401 (borra la sesión con `clearIfCurrent`);
    lo ejecuta `ProgressSyncWorker` (13b) y la interfaz reacciona a «no hay sesión estando en `Main`» (11c-3).
  - Al iniciar sesión, `GradusNavHost` espera a `start == SignedIn` antes de `enterHome()` (11c-3) y
    `StartViewModel` solo pide sincronizar si el primer estado tras `Loading` es `SignedIn` (13b-2): si se
    toca el flujo de Login, conservar ambos.
  - En 12b, `ProgressDatabase` pasa a la versión 2 (tabla `course_copies`): en desarrollo, el destructivo
    borra lo local una vez; volverá del servidor al sincronizar. También volverá `appScope` a `AppContainer`
    (quitado en 13b-2 por no tener uso) para lanzar la descarga de la copia del curso tras inscribirse.
  - Gradle: DataStore 1.3.0 (11b) y WorkManager 2.12.0 + `work-testing` 2.12.0 (13b-1) ya están añadidos.
    12a y 12b no necesitan dependencias nuevas.
- **Abierto (para después, no es de D):** inscribirse desde el camino (C-6); la página definitiva del
  curso; «Mis cursos» con los cursos inscritos (D-3: sin ella, la copia solo se usa cuando Android
  restaura el camino); «cerrar sesión» (D-2); en la tarea 14, congelar el esquema de Room (C-4).
- **Riesgos:**
  - La rama `dev/joel` toca `AppContainer`, `CourseViewModel`, `ScreenCourse`, `MainNavGraph`,
    `libs.versions.toml` y `build.gradle.kts`: cada paso los toca lo mínimo. Lo que hay que conservar al
    mergear está en la arquitectura («Qué cuidar al mergear»).
  - Un error pasajero al leer la sesión con la app ya en `Main` lleva a Login aunque la sesión siga
    guardada (`StartViewModel`, 11c-2): conocido y aceptado; muy raro (DataStore lee el fichero una vez).
  - Un fallo del almacén (Room o lectura de sesión) dentro de `ProgressSyncWorker` se propaga sin
    capturarse y WorkManager marca el trabajo como `FAILED` (sin reintentar en bucle y sin cerrar la app).
  - Con un fallo del sistema al leer la sesión que **no pasa** (`ProviderException` de Keystore o
    `IOException`), `save` también lanza y `LoginViewModel` no lo captura: iniciar sesión cerraría la app.
    Muy improbable; conocido y aceptado (probado en `DataStoreSessionStoreFailureTest`).
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
    `sharedLogic/build/test-results/testAndroidHostTest/` (o `app/build/test-results/testDebugUnitTest/`) el
    `timestamp` y que están las clases nuevas. `--rerun` solo afecta a la tarea que va justo delante.
    Desde el *sandbox* del implementador, Gradle necesita `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` y
    lanzarse fuera del *sandbox*. Instrumentados de un paso:
    `-Pandroid.testInstrumentationRunnerArguments.class=<clase>,<clase>`.
  - **Base de desarrollo:** iniciar sesión en el emulador crea una sesión en el servidor y dominar sube
    progreso: sin permiso, esas comprobaciones las hacen los usuarios (el implementador las deja escritas). No leer ni buscar ficheros con `cat`, `grep` o `sed` en el shell: usar las
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

### [x] 11. Sesión guardada cifrada (parte D)

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
- [x] **11c** (app; Sonnet 5.5 · alto; revisado el 2026-11-26). `AppContainer` con el almacén nuevo; `StartViewModel` (`Loading` |
  `SignedIn` | `SignedOut` y el aviso de «sesión borrada»); `GradusNavHost` empieza según el estado.
  Tests: `StartViewModelTest` (`Loading` hasta el primer valor, con y sin sesión, con sesión → sin ella
  avisa una vez, sin sesión → con ella no avisa); `AppContainerTest` sigue en verde. Emulador: iniciar
  sesión, `am force-stop` y abrir → Inicio (también en modo avión); `pm clear` → Bienvenida; camino →
  fondo → `am kill` → volver: el camino con lo dominado. Partido en tres (aprobado el 2026-11-26):
  - [x] **11c-1** `AppContainer` con `DataStoreSessionStore` (`sessionStore` + `sessionUpdates`) y comentario
    de `SessionCipher` (revisado el 2026-11-26).
  - [x] **11c-2** (revisado el 2026-11-26) `StartState` + `StartViewModel` en `ui/navigation/`: error al leer (`IOException` o
    `ProviderException`) → `SignedOut` y reintento cada 2 s; el resto de errores no se traga.
  - [x] **11c-3** (revisado el 2026-11-26) `StartNavigation.kt` (funciones puras), `GradusNavHost` y `MainActivity`. Regla sobre el
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

- [x] **12a** (`sharedLogic`, test primero; Gemini Neon · alto; revisado el 2026-11-26). `encodeCourseVersion`, `CourseCopyStore` +
  versión en memoria y `OfflineCourseGateway`. Tests: ida y vuelta de cada tipo de paso y ejercicio y del
  curso de ejemplo; `Found` guarda y otro `Found` sustituye; `Unreachable` con copia → `Found` igual; sin
  copia o ilegible → `Unreachable`; los demás desenlaces pasan y no usan la copia; `search` se delega.
- [ ] **12b** (app; Sonnet 5.5 · alto). Tabla y DAO, `RoomCourseCopyStore`, `ProgressDatabase` versión 2,
  la cadena en `AppContainer` y la descarga tras inscribirse (`EnrollmentViewModel` recibe una
  `fun interface`). Tests: instrumentados del almacén (guardar, leer, sobrescribir, desconocido → `null`);
  `EnrollmentViewModelTest` (pide descargar tras `enroll`; no si no actúa ni si `enroll` lanza).
  Emulador: inscribirse, abrir el camino, modo avión, fondo, `am kill`, volver: el camino sale de la copia
  y la práctica abre. Partido en dos (aprobado el 2026-11-26):
  - [x] **12b-1** (revisado el 2026-11-26) `CourseCopyEntity`, `CourseCopyDao` y `RoomCourseCopyStore` en `data/course/`,
    `ProgressDatabase` versión 2, base única y cadena `CachingCourseGateway(OfflineCourseGateway(…))` en
    `AppContainer`, y tests (`RoomCourseCopyStoreTest`, `AppContainerInitTest`, `AppContainerTest`).
  - [ ] **12b-2** `CourseCopyRequester`, `CourseCopyLauncher` (sobre `appScope`, captura *best-effort* de
    `Exception` relanzando `CancellationException`), `EnrollmentViewModel`, `MainNavGraph`, y tests
    (`CourseCopyLauncherTest`, `EnrollmentViewModelTest`, `AppContainerInitTest`, `AppContainerTest`).

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

- [x] **13a** (`sharedLogic`, test primero; Gemini Hydrogen · bajo; revisado el 2026-11-26). `backgroundSyncDecision`. Tests: cada
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
  pedir permiso.** Partido en dos (aprobado el 2026-11-26):
  - [x] **13b-1** (revisado el 2026-11-26) Gradle (WorkManager 2.12.0 y `work-testing` 2.12.0), `ProgressModule.sessionSync`,
    `ProgressSyncWorker` (con `factory`), `WorkSyncRequester` y tests instrumentados (`ProgressSyncWorkerTest`
    y `WorkSyncRequesterTest`).
  - [x] **13b-2** (revisado el 2026-11-26) `AppContainer` (`sessionProgressSync` + `progressSync`, sin `appScope` sin usar),
    `StartViewModel` (pide sincronizar solo si el primer estado resuelto es `SignedIn`), `MainActivity`,
    `MainNavGraph`, comentario de `ProgressSyncRequester`, borrado de `ProgressSyncLauncher` y
    `ProgressSyncLauncherTest`, y tests (`StartViewModelTest`, `AppContainerInitTest`, `AppContainerTest`).

## Progreso

- 2026-11-25/26: parte C terminada (detalle en `progreso-app-historico.md`).
- 2026-11-26: parte D planificada: decisiones D-1 a D-5, criterios de 11–13 y pasos aprobados por los
  usuarios.
- 2026-11-26: **tarea 11 terminada** (11a, 11b, 11c-1, 11c-2 y 11c-3; detalle de cada paso, decisiones de
  revisión y modelos en `progreso-app-historico.md`). Última pasada del orquestador (11c-3): JVM de `app`
  300/300 (22 clases) e instrumentados `GradusNavHostStartTest` + `AppContainerTest` 14/14. Probada
  parcialmente en el emulador por los usuarios sin fallos; se reconfirmará de punta a punta con modo avión
  y `am kill` en 13b/12b.
- 2026-11-26: **13a revisado y aprobado**: `BackgroundSyncDecision` (`Done` | `Retry` | `GiveUp`),
  `MAX_SERVER_FAILURE_ATTEMPTS = 5` y `backgroundSyncDecision(result, attempt)` en `sharedLogic` (con
  `require(attempt >= 0)`). Revisión del orquestador: `:sharedLogic:testAndroidHostTest --rerun` en verde
  (61 clases, 781 tests, 0 fallos; `BackgroundSyncDecisionTest` 10/10).
- 2026-11-26: **13b-1 revisado y aprobado:** WorkManager `2.12.0` (`work-runtime` + `work-testing`,
  `checkAarMetadata` en verde con `compileSdk 37`, sin avisos nuevos ni deprecación en
  `ExistingWorkPolicy.REPLACE`), `ProgressModule.sessionSync` + `AppContainer.sessionProgressSync`,
  `ProgressSyncWorker` (con `factory(sessionSync)`) y `WorkSyncRequester` (`progress-sync`, `REPLACE`,
  `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL` de 30 s). Revisión del orquestador: JVM de `app`
  300/300 (22 clases) e instrumentados `ProgressSyncWorkerTest` (10/10) + `WorkSyncRequesterTest` (8/8) =
  18/18 (0 fallos).
- 2026-11-26: **13b-2 revisado y aprobado:** `AppContainer.progressSync` (`WorkSyncRequester`), borrados
  `ProgressSyncLauncher`, `ProgressSyncLauncherTest` y `appScope` sin usar, `StartViewModel` pide sincronizar
  una sola vez solo cuando el primer estado tras `Loading` es `SignedIn` (`initialHandled` en `.onEach` antes
  de `.stateIn`), cableado en `MainActivity` y `MainNavGraph`. Revisión del orquestador: JVM de `app`
  300/300 (21 clases, 0 fallos) e instrumentados `AppContainerTest` (6) + `GradusNavHostStartTest` (10) +
  `ProgressSyncWorkerTest` (10) + `WorkSyncRequesterTest` (8) = 34/34 en el mismo proceso (0 fallos).
  Pendientes las dos pruebas de emulador de los usuarios (modo avión cerrando desde recientes y 401 borrando
  la sesión en la base) para marcar la tarea 13 `[x]`.
- 2026-11-26: **12a revisado y aprobado:** `encodeCourseVersion` en `course/http/CourseDtoEncoder.kt`
  (compartiendo `internal val courseJson` desde `CourseDtoMappers.kt`; ids de opción `"o$index"` y
  `MultipleChoice.correct.sorted()`, `formatVersion = 1` y `mastery` omitido cuando es `null`), puerto
  `CourseCopyStore` + `InMemoryCourseCopyStore` (`MutableStateFlow`) y *Decorator*
  `OfflineCourseGateway(delegate, copies)`. Revisión del orquestador: `:sharedLogic:testAndroidHostTest --rerun`
  en verde (64 clases, 806 tests, 0 fallos; `CourseDtoEncoderTest` 8/8, `InMemoryCourseCopyStoreTest` 4/4,
  `OfflineCourseGatewayTest` 13/13).
- 2026-11-26: **12b-1 revisado y aprobado:** tabla `course_copies` (`CourseCopyEntity`), `CourseCopyDao`
  (`INSERT OR REPLACE`, SQLite 3.22 / `minSdk` 29) y `RoomCourseCopyStore` en `app/.../data/course/`;
  `ProgressDatabase` sube a versión 2 con `courseCopyDao()`; `AppContainer` comparte una sola instancia
  `progressDatabase` entre `progress` y `courseGateway = CachingCourseGateway(OfflineCourseGateway(http,
  RoomCourseCopyStore(...)))` sin acoplar `courseGateway` a `sessionStore`. Revisión del orquestador:
  `:app:testDebugUnitTest --rerun` 301/301 (21 clases, 0 fallos) e instrumentados `RoomCourseCopyStoreTest`
  (8) + `AppContainerTest` (7) + `RoomProgressSyncTest` (6) = 21/21 (0 fallos).

## Siguiente paso

1. Commit de **12b-1** (por los usuarios).
2. **12b-2** (propuesta ya aprobada): `CourseCopyRequester`, `CourseCopyLauncher` sobre `appScope`,
   `EnrollmentViewModel`, `MainNavGraph`, tests y pasos de prueba en el emulador.
