# Ficha del curso, inscripción y baja — parte C (Interfaz `ui/screens/detail/`)

Índice común, Relevo general, Decisiones 1–8 y acuerdos de proceso:
`docs/tareas/ficha-e-inscripcion-curso.md`.
Relevo y garantías de la parte B (`UnenrollFromCourse`, `EnrollmentStore`, `AppContainer`):
`docs/tareas/ficha-curso-logica.md` y `docs/arquitectura-progreso.md`.
Contexto previo de la ficha: `docs/tareas/ficha-curso.md`.
Mockups de referencia: `assets/mockups/course-inscription.png` y
`assets/mockups/course-already-inscripted.png`.

## Objetivo

Rediseñar e implementar la ficha definitiva del curso en `app/.../ui/screens/detail/` para cubrir
los dos estados principales del estudiante:

- **No inscrito** (`course-inscription.png`): cabecera tintada con botón atrás, título, autor y
  estadísticas (número de módulos y fecha de publicación; sin valoraciones ni marcador), cuerpo con
  descripción, lista actual de módulos «Qué aprenderás» (`LearnList`) y etiquetas del curso al final
  como chips (`TagLabel`), y botón inferior fijo **«Inscribirse al curso»**.
- **Ya inscrito** (`course-already-inscripted.png`): cabecera tintada con botón atrás, menú `⋮`
  («Pausar curso» provisional sin acción y «Darse de baja» con diálogo destructivo en línea), título,
  autor, fecha de publicación y bloque de progreso (porcentaje redondeado, barra lineal y «X de Y
  módulos dominados»), mismo cuerpo (descripción, `LearnList` y chips `TagLabel`), y botón inferior
  fijo **«Ir al curso →»**.

## Alcance

- **C1.** Rediseño de `CourseDetailState` (`enrolled`, `enrolling`, `progressFraction`,
  `progressPercent`, `UnenrollDialogState` y nuevas acciones en `CourseDetailActions`) y de
  `CourseDetailViewModel` (observación combinada de `enrollmentStore.enrolled` y
  `progressStore.mastered`, inscripción con `progressSync` y `courseCopy`, baja en línea con
  `UnenrollFromCourse`), ajuste del `factory` en `CourseDetailDestination.kt` y batería de tests
  unitarios en `CourseDetailViewModelTest`.
- **C2.** Recursos en español e inglés (`values/strings.xml` y `values-en/strings.xml`), funciones
  puras de formato en `CourseDetailTexts.kt` y tests unitarios en `CourseDetailTextsTest`.
- **C3.** Subcomponentes pequeños y sin estado: `CourseDetailHeader.kt` (cabecera tintada, menú
  `⋮`, estadísticas de no inscrito y progreso de inscrito), actualización de
  `CourseDetailContent.kt` (descripción + `LearnList` existente + `TagLabel` al final) y
  `UnenrollCourseDialog.kt` (diálogo destructivo estilo `DeleteAccountDialog` con previews
  claro/oscuro).
- **C4.** Rediseño de `ScreenCourseDetail.kt` (sin barra inferior de navegación, ancho máximo
  560 dp, CTA fijo abajo y montaje del diálogo), previews claro/oscuro en
  `ScreenCourseDetailPreviews.kt` y cableado completo de acciones en `CourseDetailDestination.kt`.
- **C5.** Tests instrumentados de UI y accesibilidad/TalkBack (`ScreenCourseDetailTest.kt` y, si
  conviene para mantener ficheros pequeños, `UnenrollCourseDialogTest.kt`) en verde en emulador.

## Fuera de alcance

- Conectar Buscar (`MainNavGraph.kt`) a `Route.CourseDetail` y retirar `ScreenCourseOverview` /
  `Route.CourseOverview` (**Parte D**).
- Valoraciones, estrellas, opiniones, pestañas «Sobre el curso / Opiniones» y marcador/guardar
  (Decisión 1).
- Sección «Incluye» (no existe en el modelo del backend; decisión C-1).
- Lógica y persistencia de «Pausar curso» (Decisión 6).
- Cambios en `sharedLogic/`, Room (`ProgressDatabase`), Gradle o el tema (`Color.kt`/`Theme.kt`).

## Relevo

Se **reescribe** al final de cada sesión.

- **Estado (2026-11-28): C2 hecha y revisada.** Textos ES/EN en `values/strings.xml` y
  `values-en/strings.xml`, funciones puras en `CourseDetailTexts.kt` y 9 tests nuevos en
  `CourseDetailTextsTest.kt` en verde (`:app:testDebugUnitTest` 659 passed,
  `:app:compileDebugKotlin` y `:app:compileDebugAndroidTestKotlin` en verde).
- **Toca:** implementar y revisar **C3** (`CourseDetailHeader.kt`, `CourseDetailContent.kt`,
  `UnenrollCourseDialog.kt` e `ic_more_vert.xml`).
- **Pendiente después:** C4 (`ScreenCourseDetail`, previews y `CourseDetailDestination`) → C5
  (tests instrumentados).

## Lo que ya hay y sirve de modelo

- Ficheros actuales de la ficha:
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailState.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailViewModel.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailTexts.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailContent.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/ScreenCourseDetail.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/ScreenCourseDetailPreviews.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/navigation/CourseDetailDestination.kt`
- Tests actuales de la ficha:
  - `app/src/test/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailViewModelTest.kt`
  - `app/src/test/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailTextsTest.kt`
  - `app/src/androidTest/java/es/ulpgc/pamn/dominio/ui/screens/detail/ScreenCourseDetailTest.kt`
- Patrones de referencia en el proyecto:
  - Inscripción: `app/.../ui/screens/enrollment/EnrollmentViewModel.kt`
  - Diálogo destructivo: `app/.../ui/screens/settings/DeleteAccountDialog.kt`,
    `DeleteAccountState.kt`, `DeleteAccountViewModel.kt`
  - Errores de red/servidor: `FailureReason` (`sharedLogic`) y `FailureReason.toMessage()` +
    `ErrorText` (`app/.../ui/components/`)
  - Etiquetas y barra atrás: `TagLabel`, `BackButton`, `PrimaryButton`, `LightDarkPreviews`
    (`app/.../ui/components/`)

## Decisiones de la parte C (aprobadas el 2026-11-28)

- **C-1. Sin sección «Incluye» por ahora:** no existe en el modelo del backend; se omite en ambas
  vistas (actualiza el punto de «Incluye» de la Decisión 8 para esta rama).
- **C-2. Etiquetas del curso como chips `TagLabel` al final del cuerpo:** se muestran al final del
  cuerpo desplazable en un `FlowRow` reutilizando `TagLabel` (si `course.tags.isNotEmpty()`), y no
  se repiten arriba en mayúsculas en la cabecera.
- **C-3. Conservar `LearnList` actual en el cuerpo:** debajo de la cabecera tintada aparecen en
  orden: la descripción (si no está en blanco), la lista de módulos actual (`LearnList` sobre
  `course.learningOrder()`, sin rediseñarla) y, al final, los chips `TagLabel` del curso.
- **C-4. Estado sellado y combinación sin parpadeo:**
  - `CourseDetailState.Ready` añade `enrolled: Boolean = false`, `enrolling: Boolean = false` y
    `unenrollDialog: UnenrollDialogState = UnenrollDialogState.Closed`, más las derivadas
    `totalModules`, `progressFraction` (`0f` si `totalModules == 0`) y `progressPercent`
    (`(progressFraction * 100f).roundToInt()`, ej. `6 / 14 → 43`).
  - `UnenrollDialogState` es un tipo suma `Closed | Confirming | Sending | Failed(FailureReason)`,
    haciendo irrepresentable estar enviando y con error a la vez.
  - `CourseDetailViewModel` combina `enrollment.enrolled(course.id)` y `progress.mastered(course.id)`
    con `combine`: permanece en `Loading` hasta recibir ambos valores iniciales (sin parpadeo de no
    inscrito → inscrito) y preserva `enrolling` / `unenrollDialog` frente a nuevas emisiones de Room
    (`enrolling` se apaga al llegar `enrolled = true`; `unenrollDialog` se cierra si `enrolled` pasa
    a `false` fuera de `Sending`).
- **C-5. Baja en línea y `SessionExpired`:** `UnenrollCourseDialog` sigue el estilo de
  `DeleteAccountDialog` (`AlertDialog`, acción principal en `MaterialTheme.colorScheme.error`,
  botones y cierre deshabilitados durante `Sending`, `ErrorText` con `FailureReason.toMessage()` en
  `Failed`). Ante `UnenrollResult.NoSession` o `Finished(SessionExpired)` cierra el diálogo sin
  navegar (`UnenrollFromCourse` ya llamó a `clearIfCurrent` y `GradusNavHost` redirige a Login).
- **C-6. «Pausar curso» provisional y compatibilidad de `CourseDetailActions`:** «Pausar curso»
  aparece en el menú `⋮` solo cuando el usuario está inscrito y al pulsarlo cierra el menú sin
  acción (no requiere evento en el ViewModel). `CourseDetailActions` mantiene `onStart` para «Ir al
  curso →» y añade `onEnroll`, `onOpenUnenrollDialog`, `onConfirmUnenroll` y
  `onDismissUnenrollDialog` con lambdas vacías por defecto. `CourseDetailDestination.kt` se actualiza
  para pasar los puertos de `AppContainer`, manteniendo Buscar (`MainNavGraph.kt`) y
  `ScreenCourseOverview` intactos hasta la Parte D.

## Tareas

La casilla la marca el orquestador de la parte tras revisar. Cada paso sigue: propuesta → OK →
implementación y tests → revisión → mensaje de commit.

### [x] C1. Estado, eventos y `CourseDetailViewModel` (inscripción + baja)

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.6 (esfuerzo **medio**) · Gemini 2.5 Pro (esfuerzo
  **bajo–medio**).
- **Ficheros:**
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailState.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailViewModel.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/navigation/CourseDetailDestination.kt` (solo paso
    de dependencias al `factory`)
  - `app/src/test/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailViewModelTest.kt`
- **Criterios de aceptación y tests mínimos:**
  - `CourseDetailState.Ready` con `enrolled`, `enrolling`, `mastered`, `unenrollDialog`,
    `totalModules`, `progressFraction` y `progressPercent` (sin división por cero; `6/14 → 43`).
  - `UnenrollDialogState` sellado (`Closed | Confirming | Sending | Failed(FailureReason)`).
  - `CourseDetailActions` con `onEnroll`, `onStart`, `onOpenUnenrollDialog`, `onConfirmUnenroll`,
    `onDismissUnenrollDialog`, `onRetry`, `onBack` (todos con `{}` por defecto).
  - `CourseDetailViewModel(courseId, gateway, enrollment, progress, progressSync, courseCopy,
    unenroll)` + `factory`.
  - Tests unitarios en `CourseDetailViewModelTest`:
    - carga inicial, `Problem` (no reintentable vs reintentable) y `onRetry`;
    - permanece en `Loading` hasta recibir tanto `enrolled` como `mastered` (sin parpadeo);
    - curso no inscrito vs inscrito con progreso previo y actualización en vivo al dominar módulos;
    - `onEnroll`: guardia contra doble toque (`enrolling = true` inmediato), llama a `enroll`,
      `progressSync.requestSync()` y `courseCopy.requestCopy(courseId)`, pasa a `enrolled = true`
      apagando `enrolling` sin parpadeo, ignorado si ya inscrito o en `Loading`/`Problem`;
    - reinscripción limpia tras haberse dado de baja;
    - `onOpenUnenrollDialog`: abre `Confirming` solo si está en `Ready` e inscrito (ignorado si no
      inscrito o si ya estaba abierto);
    - `onDismissUnenrollDialog`: cierra en `Confirming` y `Failed`, ignorado en `Sending`;
    - `onConfirmUnenroll`: guardia contra doble envío, pasa a `Sending` y cubre los 5 desenlaces:
      - `Unenrolled` → cierra diálogo y los `Flow` actualizan `enrolled = false` y `mastered = emptySet()`;
      - `Unreachable` → `Failed(FailureReason.UNREACHABLE)` y permite reintentar;
      - `ServerFailure` → `Failed(FailureReason.SERVER_FAILURE)` y permite reintentar;
      - `SessionExpired` → cierra diálogo (sesión borrada por `UnenrollFromCourse`, sin navegar);
      - `NoSession` → cierra diálogo;
    - emisión concurrente de `progress.mastered` mientras el diálogo está abierto (`Confirming`,
      `Sending` o `Failed`) actualiza `mastered` **sin cerrar** el diálogo.
  - `./gradlew :app:testDebugUnitTest` en verde y `:app:compileDebugKotlin` sin avisos nuevos.

### [x] C2. Recursos ES/EN y funciones puras de textos (`CourseDetailTexts`)

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.6 (esfuerzo **bajo**) · Gemini 2.5 Flash (esfuerzo
  **medio**).
- **Ficheros:**
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailTexts.kt`
  - `app/src/test/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailTextsTest.kt`
- **Criterios de aceptación y tests mínimos:**
  - Todos los textos visibles y de accesibilidad en ES y EN (terminología «módulos» / «modules»):
    etiqueta «módulos» / «publicado» en estadísticas, «Publicado en …» (corto y hablado con mes
    completo para TalkBack), «Tu progreso», «%d %%», «%1$d de %2$d módulos dominados», botones
    «Inscribirse al curso», «Inscribiendo…», «Ir al curso», `contentDescription` del menú `⋮`,
    «Pausar curso», «Darse de baja», y textos del diálogo destructivo (título, aviso de pérdida de
    progreso, confirmar, «Dando de baja…» y cancelar reutilizando `action_cancel`).
  - Funciones puras en `CourseDetailTexts.kt` reutilizando `PublishedMonth` y helpers existentes sin
    duplicar traducciones de meses ni `FailureReason.toMessage()`.
  - `CourseDetailTextsTest` cubriendo todos los formatos, casos límite sin publicación, 0 módulos,
    singular/plural y lectura TalkBack con mes completo.
  - `./gradlew :app:testDebugUnitTest` en verde.

### [ ] C3. Piezas de UI: `CourseDetailHeader`, `CourseDetailContent` y `UnenrollCourseDialog`

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.6 (esfuerzo **medio**) · Gemini 2.5 Pro (esfuerzo
  **medio**).
- **Ficheros:**
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailHeader.kt` *(nuevo)*
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/CourseDetailContent.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/UnenrollCourseDialog.kt` *(nuevo)*
- **Criterios de aceptación:**
  - `CourseDetailHeader`: superficie tintada desde `MaterialTheme.colorScheme` (sin `Color(0x...)`);
    barra superior con `BackButton` (≥ 48 dp) y, solo cuando `enrolled == true`, menú `⋮` (≥ 48 dp,
    `contentDescription`) con «Pausar curso» (cierra menú sin acción) y «Darse de baja» (en
    `colorScheme.error`, llama a `onOpenUnenrollDialog`); título (`heading()`) y autor; si
    `!enrolled`, fila de estadísticas entre divisores horizontales (`N / módulos` y, si hay fecha,
    `mes año / publicado` con `contentDescription` hablado); si `enrolled`, línea `Publicado en …`
    (si hay fecha, con `contentDescription` hablado), divisor y bloque de progreso (`Tu progreso`,
    `XX %`, `LinearProgressIndicator` con extremos redondeados, `X de Y módulos dominados`).
  - `CourseDetailContent`: descripción (si no está en blanco), `LearnList` actual (sin rediseñar) y,
    al final, `FlowRow` de chips `TagLabel` (si `course.tags.isNotEmpty()`). Sin sección «Incluye».
  - `UnenrollCourseDialog`: `AlertDialog` estilo `DeleteAccountDialog` (título, cuerpo con aviso de
    pérdida de progreso, `ErrorText` en `Failed`, botón confirmar en `colorScheme.error` con texto
    de envío y deshabilitado en `Sending`, botón cancelar deshabilitado en `Sending`, y
    `@LightDarkPreviews` para `Confirming`, `Sending`, `UNREACHABLE` y `SERVER_FAILURE`).
  - Todo composable acepta `modifier: Modifier = Modifier`; compila sin avisos nuevos.

### [ ] C4. Rediseño de `ScreenCourseDetail`, previews claro/oscuro y cableado en `CourseDetailDestination`

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.6 (esfuerzo **bajo–medio**) · Gemini 2.5 Flash
  (esfuerzo **medio**) o 2.5 Pro (esfuerzo **bajo**).
- **Ficheros:**
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/ScreenCourseDetail.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/screens/detail/ScreenCourseDetailPreviews.kt`
  - `app/src/main/java/es/ulpgc/pamn/dominio/ui/navigation/CourseDetailDestination.kt`
- **Criterios de aceptación:**
  - `ScreenCourseDetail` sin barra inferior de navegación, con `CourseDetailHeader` arriba,
    `CourseDetailContent` debajo, ancho máximo 560 dp centrado para horizontal/tablet, barra
    inferior fija con CTA principal («Inscribirse al curso» / «Inscribiendo…» deshabilitado si
    `!enrolled`; «Ir al curso →» si `enrolled`) y `UnenrollCourseDialog` cuando
    `state.unenrollDialog !is UnenrollDialogState.Closed`.
  - `CourseDetailDestination` cablea todas las acciones (`onEnroll`, `onStart` con
    `dropUnlessResumed`, `onOpenUnenrollDialog`, `onConfirmUnenroll`, `onDismissUnenrollDialog`,
    `onRetry`, `onBack` con `dropUnlessResumed`). Buscar (`MainNavGraph.kt`) y
    `ScreenCourseOverview` intactos.
  - `ScreenCourseDetailPreviews` con `@LightDarkPreviews` para todos los estados: no inscrito,
    inscribiéndose, inscrito 0 %, progreso parcial, 100 %, sin publicación/descripción, cargando y
    problema. Revisadas visualmente en claro y oscuro.
  - `./gradlew :app:assembleDebug :app:testDebugUnitTest` en verde.

### [ ] C5. Tests instrumentados de UI y accesibilidad (`ScreenCourseDetailTest`) y cierre de Parte C

- **Modelo y esfuerzo mínimos:** Claude Sonnet 4.6 (esfuerzo **medio**) · Gemini 2.5 Pro (esfuerzo
  **bajo–medio**).
- **Ficheros:**
  - `app/src/androidTest/java/es/ulpgc/pamn/dominio/ui/screens/detail/ScreenCourseDetailTest.kt`
  - (Opcional si supera ~150 líneas)
    `app/src/androidTest/java/es/ulpgc/pamn/dominio/ui/screens/detail/UnenrollCourseDialogTest.kt`
- **Criterios de aceptación y tests mínimos:**
  - Vista no inscrito: muestra título, autor, módulos, fecha de publicación, descripción, lista de
    módulos, chips de etiquetas y botón «Inscribirse al curso» (llama a `onEnroll`); no muestra
    menú `⋮` ni bloque de progreso.
  - Estado `enrolling = true`: botón muestra estado de envío y no vuelve a disparar `onEnroll`.
  - Vista ya inscrito: muestra bloque de progreso (porcentaje y módulos dominados), botón «Ir al
    curso» (llama a `onStart`) y menú `⋮`; al abrir `⋮` aparecen «Pausar curso» (cierra sin fallar)
    y «Darse de baja» (llama a `onOpenUnenrollDialog`).
  - Diálogo de baja: `Confirming` (confirmar llama a `onConfirmUnenroll`, cancelar llama a
    `onDismissUnenrollDialog`), `Sending` (botones deshabilitados) y `Failed` (`UNREACHABLE` y
    `SERVER_FAILURE` muestran error y permiten reintentar).
  - Accesibilidad/TalkBack: encabezados (`heading`), lectura hablada de fecha con mes completo,
    `contentDescription` en botón atrás y `⋮`, zonas táctiles ≥ 48 dp.
  - Estados `Loading` y `Problem` (reintentar y atrás) siguen en verde.
  - Tests instrumentados de `ui.screens.detail` en el emulador y `./gradlew :app:testDebugUnitTest`
    en verde.

## Progreso

- 2026-11-28: documento creado por el orquestador C tras aprobar el plan C1–C5 y las decisiones
  C-1 a C-6 (sin sección «Incluye» por ahora, `LearnList` actual conservada y etiquetas del curso
  como chips `TagLabel` al final del cuerpo).
- 2026-11-28: **C1 hecha y revisada.** Implementados `UnenrollDialogState`, `CourseDetailState.Ready`
  ampliado (`enrolled`, `enrolling`, `unenrollDialog`, `totalModules`, `progressFraction`,
  `progressPercent`), `CourseDetailActions`, `CourseDetailViewModel` con `combine`, `onEnroll` y
  baja en línea (`dialogAfterEmission` y `dialogAfterUnenroll`), `factory` en
  `CourseDetailDestination.kt` y 26 tests unitarios (`CourseDetailViewModelTest` y
  `CourseDetailUnenrollViewModelTest`). Compilan `:app:compileDebugKotlin`,
  `:app:compileDebugAndroidTestKotlin` y `:app:testDebugUnitTest` (650 passed).

- 2026-11-28: **C2 hecha y revisada.** Añadidas claves ES/EN (`detail_modules_label`,
  `detail_stat_published`, `detail_published_in`, `detail_progress_title`, `detail_progress_percent`,
  `detail_progress_modules`, `detail_enroll`, `detail_go_to_course`, `detail_more_options`,
  `detail_pause_course`, `detail_unenroll`, `detail_unenrolling`, `detail_unenroll_title`,
  `detail_unenroll_body`), funciones puras en `CourseDetailTexts.kt` (manteniendo compatibilidad
  hasta C4) y 9 tests nuevos en `CourseDetailTextsTest.kt`. En verde: `:app:compileDebugKotlin`,
  `:app:compileDebugAndroidTestKotlin` y `:app:testDebugUnitTest` (659 passed).

## Siguiente paso

Lanzar el implementador para **C3** (`CourseDetailHeader.kt`, actualización de
`CourseDetailContent.kt`, `UnenrollCourseDialog.kt` e icono vectorial `ic_more_vert.xml`).
