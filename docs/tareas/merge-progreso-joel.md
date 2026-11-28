# Guía de merge: `dev/course-progress-tracking` + `dev/joel`

Documento de relevo para ejecutar en un chat nuevo la fusión de `dev/joel` sobre `dev/merge-temporal-branch`
(creada a partir de `dev/course-progress-tracking`).

## 1. Contexto Git (estado verificado el 2026-11-26)

- **Rama actual:** `dev/merge-temporal-branch` en `e9b3c53` (mismo commit que `dev/course-progress-tracking`:
  partes A, B, C, D y tarea 14 terminadas; **2 266 tests en verde**).
- **Rama a fusionar:** `dev/joel` en `a3b8dc3` (`origin/dev/joel` apunta al mismo commit).
- **Ancestro común (`merge-base`):** `97e0594` (`dev/backend-course-search`).
- **Qué aporta `dev/course-progress-tracking`:** inscripciones, progreso por cuenta en Room (`ProgressDatabase` v2
  congelada en `app/schemas/.../2.json`), sincronización `POST /api/progress/sync` (backend `006_create_enrollments.sql`
  + `sharedLogic` + WorkManager 2.12.0), sesión cifrada en DataStore 1.3.0 + Keystore, arranque según sesión
  (`StartViewModel` + `GradusNavHost`), copia local de cursos (`OfflineCourseGateway` + `course_copies`) y precarga al
  inscribirse (`CourseCopyLauncher` + `EnrollmentViewModel`).
- **Qué aporta `dev/joel`:** ficha real del curso (`ScreenCourseDetail`, `Course.publication`), notas con cámara
  (CameraX 1.7.0, `GradusDatabase` en `gradus.db`, `ModuleSheet` en el camino), ejercicios con foto (`Step.Photo`,
  `PhotoDto`, entregas multipart en backend `005_create_submissions.sql`), ajustes (modo oscuro, idiomas ES/EN con
  `UiText` + `strings.xml`, editar perfil, cambiar contraseña, cerrar sesión, eliminar cuenta, etiquetas preferidas).

## 2. Qué leer al empezar

1. `AGENTS.md` (reglas del proyecto: **no hacer commits, push ni cambiar de rama**; proponer primero y esperar OK).
2. Este documento completo.
3. `docs/arquitectura-progreso.md`: solo **«Qué cuidar al mergear»** (y consultar «Mapa de piezas» si hace falta).
4. Los ficheros conflictivos o afectados conforme se vayan resolviendo.

No hace falta leer `progreso-app-historico.md`, `progreso-logica.md` ni carpetas enteras de `backend/` salvo los
ficheros concretos tocados.

## 3. Decisiones que hay que preguntar a los usuarios ANTES de resolver el merge

Haz estas preguntas concretas en tu primera propuesta (con la opción recomendada marcada) y espera su OK:

1. **Cerrar sesión y eliminar cuenta (`dev/joel`) vs `GradusNavHost.leaveMain()` (nuestro):**
   - En `dev/joel`, `SettingsViewModel.onLogOut()` y `DeleteAccountViewModel` llaman a `sessions.clear()` y ponen
     `LoggedOut`, ante lo cual `SettingsRoute` llama a `navController.leaveToWelcome()` → pila `[Welcome]`.
   - En nuestra rama, cuando se borra la sesión estando en `Main`, `StartViewModel` emite `SignedOut` y
     `GradusNavHost` dispara `leaveMain()` → pila `[Welcome, Login]` (diseñado para un 401 inesperado).
   - **Riesgo:** doble navegación en carrera al pulsar «Cerrar sesión» o acabar en Login cuando el usuario quiso salir
     a Bienvenida.
   - **Opciones:**
     - *(Recomendada)* Unificar la salida de `Main` en `GradusNavHost`: si la salida viene de cerrar sesión / eliminar
       cuenta explícito → `[Welcome]`; si la sesión desaparece en segundo plano (401 de `ProgressSyncWorker`) →
       `[Welcome, Login]`. Quitar el `LaunchedEffect` duplicado de `SettingsRoute` (o coordinar ambos con una única
       fuente de navegación).
     - *(Alternativa simple)* Que cualquier pérdida de sesión en `Main` vaya al mismo sitio (`[Welcome]` o
       `[Welcome, Login]`), eliminando la navegación redundante en `SettingsRoute`.
2. **Hoja del módulo (`ModuleSheet`) vs puerta de inscripción (`NotEnrolled`) en `CourseViewModel`:**
   - En `dev/joel`, tocar un módulo desbloqueado abre `ModuleSheet` («Estudiar» / «Mis notas»).
   - En nuestra rama, `onModuleClick` pasa por `course.tapModule(moduleId, mastered, studyAccess(enrolled))` y sin
     inscripción saca el aviso `CourseNotice.NotEnrolled`.
   - **Opciones:**
     - *(Recomendada)* Sin inscripción, tocar un módulo sigue sacando `CourseNotice.NotEnrolled` (usando `tapModule`;
       si `OpenPractice`, abre `sheet = moduleId` en vez de `practiceToOpen` directo; «Estudiar» en la hoja abre
       `practiceToOpen` y `Route.Exercise` mantiene la segunda puerta `when (exerciseViewModel.access)`).
     - *(Alternativa)* Permitir abrir `ModuleSheet` sin inscripción para ver notas, y bloquear solo «Estudiar».
3. **`ScreenCourseDetail` (`dev/joel`) vs `ScreenCourseOverview` (página de relleno nuestra):**
   - *(Recomendada para el merge)* Mantener `ScreenCourseOverview` funcionando durante el merge (con Buscar apuntando
     a `Route.CourseOverview`, y `courseDetailDestination` registrado sin romper nada) para no mezclar resolución de
     conflictos con rediseño de UI; integrar `EnrollButton` en `ScreenCourseDetail` y borrar `ScreenCourseOverview` en
     una tarea limpia inmediatamente posterior.
   - *(Alternativa)* Integrar ya `EnrollButton` + `EnrollmentViewModel` en `ScreenCourseDetail` durante el merge.
4. **Bases de datos Room:**
   - *(Recomendada)* Mantener las **dos bases separadas** durante el merge: `gradus.db` (`GradusDatabase`, notas de
     `dev/joel`) y `gradus-progress.db` (`ProgressDatabase` v2 congelada en `app/schemas/.../2.json`). Room soporta
     varias bases sin problema y así no se rompe el esquema recién congelado.

## 4. Mapa de conflictos textuales (14 ficheros tocados en ambas ramas)

Simulación verificada con `git merge-tree 97e0594 HEAD dev/joel`: **11 ficheros con conflicto textual (26 bloques)**
y **3 ficheros tocados por ambos lados sin conflicto textual pero que revisar**.

### 4.1. Conflictos mecánicos (fáciles)
- **`gradle/libs.versions.toml` (2 bloques):** ambas ramas usan exactamente **Room 2.8.5** y **KSP 2.3.14**. Conservar
  una sola vez `room`, `ksp`, `androidx-room-runtime`, `androidx-room-compiler` y el plugin `ksp`; conservar también
  `camerax = "1.7.0"` + `androidx-room-ktx` + librerías de CameraX (`dev/joel`), y `datastore = "1.3.0"` +
  `work = "2.12.0"` + `androidx-datastore`, `androidx-work-runtime`, `androidx-work-testing` (nuestros).
- **`app/build.gradle.kts` (0 bloques `<<<<<<<`, ¡pero revisar duplicados!):** ambos añadieron `room-runtime` y
  `ksp(room-compiler)` en posiciones distintas del bloque `dependencies`. Dejar **una sola vez** cada línea de Room y
  el plugin `ksp`, conservando `room-ktx` + CameraX (`dev/joel`), `datastore` + `work-runtime` + `work-testing` +
  `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` (nuestros).
- **`build.gradle.kts` (0 bloques `<<<<<<<`):** comprobar que `alias(libs.plugins.ksp) apply false` aparece una sola vez.
- **`app/src/main/AndroidManifest.xml` (1 bloque):** conservar `CAMERA` + `uses-feature` + `android:localeConfig`
  (`dev/joel`) **y** `android:name=".GradusApplication"` (nuestro, obligatorio para Room, DataStore y WorkManager).
- **`app/src/main/java/es/ulpgc/pamn/dominio/ui/navigation/Routes.kt` (1 bloque):** conservar todas las rutas de ambas
  ramas (`CourseOverview` nuestra + `EditProfile`, `ChangePassword`, `PreferredTags`, `CourseDetail`, `ModuleNotes`,
  `TakeNote`, `NoteViewer`, `TakeSubmissionPhoto` de `dev/joel`).
- **`backend/src/main/java/es/ulpgc/pamn/dominio/backend/App.java` (4 bloques):** combinar `ProgressController` +
  `POST /api/progress/sync` (nuestro) con `UserController` ampliado (`RenameUser`, `ChangePassword`, `DeleteAccount`),
  `SubmissionController`, límites multipart y rutas `/api/users/me` (`PATCH`/`PUT`/`DELETE`) y `/api/submissions/...`
  (`dev/joel`).
  - **SQL verificado sin colisión:** `005_create_submissions.sql` (`dev/joel`) y `006_create_enrollments.sql`
    (nuestra, que ya lleva `ON DELETE CASCADE` desde `users(id)` y `enrollments`, compatible con `DeleteAccount`).
- **`docs/especificacion.md` (1 bloque):** combinar las actualizaciones de `dev/joel` (ajustes/cámara/entregas/idiomas)
  con las nuestras (§4.4, §5 con nota provisional C-6, §8).

### 4.2. Conflictos de cableado y lógica (atención cuidadosa)
- **`app/src/main/java/es/ulpgc/pamn/dominio/data/AppContainer.kt` (3 bloques):**
  - Conservar nuestro `private var appContext: Context? = null`, `requireContext()` y `fun init(context)` idempotente
    (llamado desde `GradusApplication.onCreate`, **no** `lateinit var` ni inicializar en `MainActivity`).
  - Conservar nuestras piezas: `progressDatabase` (`ProgressDatabase` v2), `progress` (`ProgressModule`),
    `progressStore`, `enrollmentStore`, `sessionProgressSync`, `progressSync` (`WorkSyncRequester`),
    `courseGateway` (`CachingCourseGateway(OfflineCourseGateway(http, RoomCourseCopyStore))`), `appScope`,
    `courseCopy` (`CourseCopyLauncher`), `sessions` (`DataStoreSessionStore`, sustituyendo al `InMemorySessionStore` de
    `dev/joel`), `sessionStore` y `sessionUpdates`.
  - Conservar todas las piezas de `dev/joel` usando `requireContext()` en vez de `appContext`: `database`
    (`GradusDatabase` en `"gradus.db"`), `notesStore`, `notePhotos`, `submissionGateway`, `submissionPhotos`,
    `backgroundScope`, `languageSetting` y `settingsStore`.
  - Actualizar/revisar `AppContainerInitTest.kt` y `AppContainerTest.kt`.
- **`app/src/main/java/es/ulpgc/pamn/dominio/MainActivity.kt` (2 bloques):**
  - Quitar `AppContainer.init(this)` de `onCreate` (ya lo hace `GradusApplication`).
  - Conservar nuestro `startViewModel` (`StartViewModel.factory(AppContainer.sessionUpdates, AppContainer.progressSync)`)
    y el paso de `start` y `progressSync = AppContainer.progressSync` a `GradusNavHost`.
  - Conservar el modo oscuro reactivo (`settingsStore.settings.collectAsStateWithLifecycle()`, `DisposableEffect(dark)`
    con `enableEdgeToEdge(...)` y `GradusTheme(darkTheme = dark)` de `dev/joel`).
- **`app/src/main/java/es/ulpgc/pamn/dominio/ui/navigation/MainNavGraph.kt` (3 bloques):**
  - Conservar `SettingsRoute`, `moduleNotesDestination`, `takeNoteDestination`, `takeSubmissionPhotoDestination`,
    `noteViewerDestination`, `courseDetailDestination`, `editProfileDestination`, `changePasswordDestination`,
    `preferredTagsDestination` (`dev/joel`).
  - En `Route.Search`: conservar `Route.CourseOverview(id)` (si se elige la opción recomendada 3a).
  - Conservar `composable<Route.CourseOverview>` con `EnrollmentViewModel.factory(route.courseId,
    AppContainer.enrollmentStore, AppContainer.progressSync, AppContainer.courseCopy)`.
  - En `composable<Route.Course>`: pasar **ambos** almacenes a `CourseViewModel.factory` (`progressStore`,
    `enrollmentStore`, `notesStore`), conservar `notesToOpen` y `ModuleSheetActions` de `dev/joel`.
  - En `composable<Route.Exercise>`: pasar todos los parámetros a `ExerciseViewModel.factory` (`progressStore`,
    `enrollmentStore`, `progressSync`, `submissionGateway`, `sessionStore`, `submissionPhotos`, `backgroundScope`),
    conservar **nuestro bloque `when (exerciseViewModel.access)`** antes de componer `ScreenExercise`, y conservar
    `rememberExercisePhotoSources` + `PhotoStepUi` + las nuevas lambdas de foto de `ExerciseActions`.
- **`app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/course/CourseViewModel.kt` (5 bloques):**
  - Unir `enrollment: EnrollmentStore` (nuestro) y `notes: NotesStore = InMemoryNotesStore()` (`dev/joel`) en
    constructor y `factory`.
  - En `CourseUiState`: conservar `enrolled: Boolean = false` (nuestro) y `sheet`, `noteCounts`, `notesToOpen` (`dev/joel`).
  - En `CourseNotice`: conservar `Locked` y `NotEnrolled`.
  - En `showWithProgress`: hacer `combine(progress.mastered(course.id), enrollment.enrolled(course.id), notes.counts(course.id))`
    conservando el estado previo (`notice`, `sheet`, eventos).
  - En `onModuleClick`: aplicar la decisión 2 (acordada con los usuarios) combinando `tapModule` con `ModuleSheet`.
  - Revisar tanto `CourseViewModelGateTest.kt` (nuestro) como `CourseViewModelTest.kt` (`dev/joel`).
- **`app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/exercise/ExerciseViewModel.kt` (3 bloques):**
  - Unir constructor y `factory`: `course`, `moduleId`, `progress`, `enrollment`, `progressSync`, `submissions`,
    `sessions`, `photos`, `cleanup`, `random` (valorar valores por defecto en el constructor para no romper tests).
  - Conservar nuestra comprobación de puerta (`access` leído una vez con `studyAccess(enrollment.enrolled(course.id).first())`).
  - En `onContinue()`: conservar `change(next)` de `dev/joel` **y** llamar a `progressSync.requestSync()` justo tras
    `progress.markMastered(next.course.id, next.module.id)` (solo al aprobar, no en cada «Continuar»).
  - Conservar toda la lógica de fotos (`onPhotoChosen`, `onSubmitPhoto`, `deleteUnused`, `canSubmitPhotos`, etc.).
  - Revisar `ExerciseViewModelGateTest.kt`, `ExerciseViewModelSyncTest.kt` (nuestros) y `ExerciseViewModelTest.kt` (`dev/joel`).
- **`app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/course/CourseNoticeMessages.kt` (1 bloque):**
  - `dev/joel` cambió el retorno de `String` a `UiText` (porque `ScreenCourse.kt` llama ahora a
    `notice.toMessage().resolve(resolver)`).
  - Añadir `CourseNotice.NotEnrolled -> UiText.of(R.string.course_not_enrolled)` y las cadenas en
    `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml`.
  - Actualizar `CourseNoticeMessagesTest.kt` al nuevo retorno `UiText`.
- **`sharedLogic/src/commonMain/kotlin/es/ulpgc/pamn/dominio/course/http/CourseDtoMappers.kt` (0 bloques `<<<<<<<`):**
  - Comprobar tras el merge que `internal val courseJson` (nuestro) y `toPublication()` + `is PhotoDto -> Step.Photo(...)`
    (`dev/joel`) conviven intactos.

## 5. Puntos semánticos que Git NO marca como conflicto (revisar obligatoriamente)

1. **`sharedLogic/.../course/http/CourseDtoEncoder.kt` (¡error de compilación tras el merge!):**
   - Añadir `is Step.Photo -> PhotoDto(id = id, prompt = prompt, explanation = explanation)` en `Step.toDto()`.
   - Serializar también `course.publication` en `CourseVersionDto(id = course.id, content = content, version = ..., author = ..., publishedAt = ...)` para que las copias sin conexión no pierdan los metadatos de publicación (autor, versión, fecha) ni `ExerciseViewModel.canSubmitPhotos` pase a `false` sin red.
   - Añadir tests en `CourseDtoEncoderTest.kt` (test primero) cubriendo `Step.Photo` y `Course.publication` (nulo y presente).
2. **Internacionalización (`strings.xml` / `values-en/strings.xml`) de nuestra UI nueva:**
   - Añadir traducciones ES y EN para `CourseNotice.NotEnrolled`, `EnrollButton` y `ScreenCourseOverview` (y revisar
     si `EnrollButtonTest` / `ScreenCourseOverviewTest` necesitan la regla `InSpanish` de `dev/joel` para no depender
     del idioma del emulador).
   - Comprobar que ningún fichero nuestro importaba `SpanishText.kt` (eliminado en `dev/joel`).
3. **`CourseViewModel` y `ExerciseViewModel` en todos los tests:**
   - Existen tests en ambas ramas (`CourseViewModelGateTest` + `CourseViewModelTest`; `ExerciseViewModelGateTest` +
     `ExerciseViewModelSyncTest` + `ExerciseViewModelTest`). Asegurar que **todos** compilan y siguen probando sus
     invariantes originales.
4. **Cierre de sesión / borrado de cuenta y `StartViewModel`:**
   - Añadir/actualizar tests en `GradusNavHostStartTest` o `SettingsRoute` verificando que el flujo elegido en la
     decisión 1 funciona sin doble navegación y sin romper el caso del 401 (`SessionProgressSync` → `StartViewModel` →
     `leaveMain`).
5. **Base de desarrollo (PostgreSQL):**
   - Sin permiso de los usuarios, no escribir nada en la base de desarrollo. Avisar a los usuarios de que, si aún no
     han aplicado `005_create_submissions.sql` en su PostgreSQL local, deben aplicarla para probar las entregas con foto.

## 6. Plan sugerido para el nuevo chat

1. **Propuesta y decisiones:** leer lo indicado en §2, presentar a los usuarios las 4 decisiones de §3 y esperar su OK
   (y recordar que el `git merge dev/joel` lo ejecutan los usuarios o lo aprueba explícitamente el usuario antes de
   lanzarlo — revisar AGENTS §9: «Hacer commits, push o cambiar de rama: eso lo hacemos nosotros»; preguntar si prefieren
   lanzar ellos `git merge --no-commit --no-ff dev/joel` y que el agente resuelva los conflictos).
2. **Resolver conflictos mecánicos y de cableado** (§4.1 y §4.2).
3. **Arreglos semánticos** (§5: `CourseDtoEncoder` + tests, logout/`leaveMain`, `ModuleSheet` + `NotEnrolled`, `UiText`
   y `strings.xml`, tests de ViewModels).
4. **Verificación completa (sin avisos nuevos):**
   - `sharedLogic`: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :sharedLogic:testAndroidHostTest --rerun --console=plain`
   - `app` JVM: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest --rerun --console=plain`
   - `app` instrumentados (emulador encendido): `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:connectedDebugAndroidTest --console=plain` (verificar XML en `app/build/outputs/androidTest-results/connected/debug/*.xml`, sin abrir XML con `read_file` ni imprimir `<`/`>`).
   - `backend`: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk mvn -f backend/pom.xml test`
5. **Pruebas manuales recomendadas en el emulador (para los usuarios):**
   - Cerrar sesión desde Ajustes (verificar destino y botón atrás).
   - Inscribirse en un curso → abrir camino → tocar módulo desbloqueado (`ModuleSheet` → Estudiar / Mis notas) y módulo
     sin inscripción (`NotEnrolled`).
   - Modo avión + `am kill` sobre un curso con paso de foto (copia local conserva `Step.Photo` y `publication`).
   - Cambio de idioma ES ↔ EN en `ScreenCourseOverview`, `EnrollButton` y aviso `NotEnrolled`.
6. **Cierre:** actualizar `docs/arquitectura-progreso.md` con lo que haya cambiado tras el merge, proponer el mensaje
   de commit del merge (y preguntar si borrar este fichero temporal `docs/tareas/merge-progreso-joel.md` antes de
   mergear a `develop`/`main`).
