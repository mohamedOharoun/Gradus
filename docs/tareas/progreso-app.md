# Progreso en la app (partes C y D: tareas 7–13)

Parte C y D del seguimiento del progreso (índice común: `docs/tareas/seguimiento-progreso.md`).
Referencia viva de cómo funciona lo que ya hay: `docs/arquitectura-progreso.md`. Lo terminado de la parte
C, con su detalle: `docs/tareas/progreso-app-historico.md` (no hace falta leerlo para continuar). Rama:
`dev/course-progress-tracking`.

Estado: **parte C terminada (tareas 7–10, 2026-11-26). Parte D (11–13) sin empezar: falta su plan.**

## Relevo

Se **reescribe** al final de cada sesión; no se acumula.

- **Dónde estamos (2026-11-26):** parte C terminada, revisada, probada en el emulador por los usuarios
  y en commits. La app:
  - deja inscribirse desde una página de relleno (Buscar → curso → «Inscribirme» / «Ir al camino»);
  - no deja estudiar sin inscripción (`tapModule` en el camino y la puerta de `Route.Exercise`);
  - guarda inscripciones y dominados en Room, por usuario (`RoomProgressStore`, los tres puertos);
  - sincroniza con el servidor tras iniciar sesión, inscribirse y dominar (`ProgressSyncLauncher`, en
    un ámbito de la app), sin enseñar nada en pantalla (C-5).
- **Paso que toca:** el orquestador de D (puede ser un chat nuevo) planifica la parte D: lee este
  Relevo, las decisiones C-1, C-3 y C-5 y la arquitectura («Cómo extenderlo» y «Qué cuidar al
  mergear»), y propone a los usuarios lo que haya que decidir antes de los pasos. Al menos:
  - **Dependencias (sin aprobar):** WorkManager (D13; 2.12.0 era la estable el 2026-11-25) y lo que pida
    la sesión cifrada (D11; se pensó en DataStore Preferences 1.3.0 + Android Keystore, sin librería de
    cifrado; confirmar si `androidx.security:security-crypto` está obsoleta). Comprobar las versiones
    antes de proponerlas.
  - **D11, sesión guardada cifrada:** implementa `SessionStore` sin cambiar el puerto (es de la parte
    B); qué se guarda y cómo se cifra; qué pasa con `SessionExpired` (C-5: llevar a iniciar sesión;
    hoy no se enseña nada) y con la sesión caducada al abrir la app; si hace falta «cerrar sesión»
    (no existe: la regla de C-5 dice que lo que escucha el progreso vive en el grafo `Main`).
  - **D12, copia local del curso al inscribirse:** dónde se guarda (Room o fichero), cuándo se usa
    (sin red, el camino y la práctica) y su relación con `CachingCourseGateway`; fuera de alcance,
    actualizarla con versiones nuevas y el SHA-256.
  - **D13, WorkManager:** trabajo único, restricción de red, reintento con espera creciente; necesita
    la sesión persistente (D11) porque el `Worker` corre sin la app abierta; usa el mismo
    `ProgressSync` de `ProgressModule`; «al volver la conexión» se pasó aquí desde C (C-5).
  - Si D necesita lógica nueva, va en `sharedLogic/src/commonMain` con su test en `commonTest`; la
    parte B está cerrada (si hay que tocarla, se propone y se reabre con un paso nuevo).
- **Abierto (para después, no es de D):** inscribirse desde el camino (C-6: reutilizar `EnrollButton`
  y `EnrollmentViewModel`); la página definitiva del curso; en la tarea 14, congelar el esquema de Room
  (exportarlo y quitar `fallbackToDestructiveMigration`, C-4).
- **Riesgos:**
  - La rama `dev/joel` toca `AppContainer`, `CourseViewModel`, `ScreenCourse`, `MainNavGraph`,
    `libs.versions.toml` y `build.gradle.kts`: cada paso los toca lo mínimo. Lo que hay que conservar al
    mergear está en la arquitectura («Qué cuidar al mergear»).
  - Hasta D11 la sesión está en memoria: si Android mata el proceso y restaura una pantalla de `Main`,
    no hay sesión (el camino sale vacío e inscribirse lanzaría). Conocido y aceptado; lo resuelve D11.
  - Un fallo del almacén (Room) dentro de la sincronización cierra la app (no se captura, a propósito).
- **Cómo comprobar (aprendido en C):**
  - La sincronización no se ve en pantalla. Para comprobar que llega al servidor: dominar, `adb shell pm
    clear es.ulpgc.pamn.dominio` (borra solo la base local) y volver a iniciar sesión: lo dominado
    vuelve del servidor. Escribir en la base de desarrollo necesita permiso de los usuarios cada vez.
  - Tests instrumentados: con el emulador caído, `connectedDebugAndroidTest` acaba en BUILD SUCCESSFUL
    sin ejecutar nada. Mirar siempre `app/build/outputs/androidTest-results/connected/debug/*.xml`
    (un solo fichero, con espacios en el nombre: contar `testcase name` y mirar el `timestamp`).
  - Leer un XML de resultados con `read_file`, o con un `grep` que imprima `<`/`>`, se cuelga en esta
    máquina: usar la herramienta de búsqueda con `-o` y un patrón sin etiquetas.
  - La herramienta de shell corta las órdenes largas: lanzar `./gradlew` desacoplado (`setsid nohup …
    > /tmp/log &`) y leer el log con la herramienta de búsqueda. `--rerun` solo afecta a la tarea que va
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
desde el camino (C-6).

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

## Decisiones (2026-11-25; siguen vigentes)

- **C-1 Dependencias.** Aprobadas y añadidas: Room 2.8.5 (`room-runtime`, `room-compiler` con KSP),
  KSP 2.3.14 y `kotlinx-coroutines-test` en `androidTestImplementation`. Sin driver extra (SQLite del
  sistema). Room 3 se descartó por ahora. **Para D, sin aprobar:** WorkManager y lo que pida D11.
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
  con `SessionExpired`**. Lo que escucha el progreso vive dentro del grafo `Main` (se crea tras iniciar
  sesión): un futuro «cerrar sesión» debe sacar de `Main`.
- **C-6 Botón y página:** página de relleno `ScreenCourseOverview` desde Buscar, con `EnrollButton`
  (reutilizable, no navega) e «Ir al camino» (siempre activo, se borra con la página). La ruta lleva
  solo `courseId`. Sin inscripción, tocar un módulo enseña «Inscríbete para empezar a estudiar».
  Inscribirse desde el camino, para después.

## Tareas

La casilla la marca el orquestador tras revisar. Los tests nombrados son el **mínimo** (AGENTS §7).

### [x] Parte C: 7 Room · 8 sincronizar · 9 la puerta · 10 botón de inscribirse

Hechas y revisadas (2026-11-25/26). Pasos, modelos y detalle: `progreso-app-historico.md`.

### [ ] 11. Sesión guardada cifrada (parte D)

- La sesión sobrevive a cerrar la app, cifrada en el móvil; `clear` la borra. Implementa `SessionStore`
  sin cambiar el puerto. `SessionExpired` lleva a iniciar sesión. Requisito de 13.

### [ ] 12. Copia local del curso al inscribirse (parte D)

- Al inscribirse se guarda el curso; sin red, el camino y la práctica abren desde la copia.

### [ ] 13. WorkManager (parte D)

- Sincronización en segundo plano: trabajo único, con red, reintento con espera creciente; sin sesión
  no hace nada.

Los criterios de aceptación y los pasos de 11–13 los escribe el orquestador de D al planificar.

## Progreso

- 2026-11-25/26: parte C terminada (detalle en `progreso-app-historico.md`).

## Siguiente paso

Planificar la parte D: decisiones para los usuarios (dependencias, D11, D12, D13; ver el Relevo) y,
tras su OK, los pasos con su modelo y esfuerzo.
