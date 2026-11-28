# Pantalla Inicio

## Objetivo

La pestaña **Inicio** del Figma («Dominio · Pantallas principales») con datos reales: saludo, «Continúa
donde lo dejaste» y «Tus cursos» con su porcentaje. Sustituye a la pantalla provisional «¡Hola!».

No hace falta tocar el backend: las inscripciones y lo dominado llegan al sincronizar
(`POST /api/progress/sync`, guardado en Room) y cada curso sale de `GET /api/courses/{id}` o de su copia
local (`OfflineCourseGateway`), así que Inicio funciona también sin conexión.

## Alcance

- **Cabecera:** la fecha de hoy en el idioma de la app («Miércoles, 28 de octubre»), «Hola, {nombre}» y
  las iniciales (`Initials.kt`).
- **Continúa donde lo dejaste:** el curso sin terminar con la actividad más reciente (inscripción o último
  módulo dominado). Título, autor, «12 de 20 conceptos dominados», barra, «Siguiente: {módulo}» (el primero
  desbloqueado sin dominar, en el orden del camino) y «Continuar», que abre el camino.
- **Tus cursos:** todos los inscritos, del más reciente al menos. Inicial, título, «autor · 7 de 20» y
  «35 %». Al tocar uno se abre su camino.
- **Sin cursos:** un aviso con un botón a Buscar.
- Textos en español e inglés (`strings.xml`), previews en claro y oscuro, TalkBack.

## Fuera de alcance

- **Repasos de hoy** (decisión A): la tarjeta no sale hasta que existan los repasos SM-2, que tendrán su
  propio documento.
- **«Ver todos»** (decisión B): no sale; Inicio muestra todos los cursos hasta que exista «Mis cursos».
- Imagen de cada curso: el backend no la tiene. Se usa la inicial.

## Decisiones

- **A.** Repasos de hoy: no se muestra la tarjeta (aceptada el 2026-11-28).
- **B.** «Ver todos»: no se muestra; Inicio lista todos los cursos (aceptada el 2026-11-28).
- **C.** Para leer todas las inscripciones hay que añadir una consulta de solo lectura a `ProgressDao` y
  `RoomProgressStore` (del compañero), sin cambiar el esquema v2. Permiso de Joel el 2026-11-28.
- **D.** «Actividad» de un curso = lo más reciente entre la fecha de inscripción y la del último módulo
  dominado. Empate: por título y, si coincide, por id (orden estable).
- **E.** Porcentaje redondeado hacia abajo: un curso no marca 100 % hasta estar terminado. Un curso sin
  módulos cuenta como 0 de 0, 0 %, y no sale en «Continúa».
- **F.** Los ids dominados que ya no existen en el curso (el autor quitó un módulo) no cuentan.
- **G.** El azul de las iniciales de los cursos entra en el tema como `secondaryContainer` /
  `onSecondaryContainer` (`ExtraAzul*` de `Color.kt`; en oscuro, al revés). Aceptada el 2026-11-28.
- **H.** `ScreenHome.kt` se mueve a `ui/screens/home/`, junto a su ViewModel (paso 4d). Aceptada el 2026-11-28.
- **I.** El icono de «Continúa» es el cuadrado con la inicial del curso, en grande. Aceptada el 2026-11-28.
- **J.** «Tus cursos» no repite el curso de «Continúa», como en el Figma; si no queda ninguno, la sección
  no sale. Aceptada el 2026-11-28.

## Tareas

- [x] **1** (`sharedLogic`, test primero; hecho el 2026-11-28). `homeSummary`: función pura que, con los cursos inscritos
  (curso, inscripción, módulos dominados), da el curso de «Continúa», su siguiente módulo, la lista
  ordenada y los porcentajes. *Acepta si* los tests cubren: orden por actividad (inscripción y dominio),
  empates, cursos terminados (salen en la lista, no en «Continúa»), todos terminados, sin cursos, curso sin
  módulos, redondeo del porcentaje, ids que ya no existen, siguiente módulo en el orden del camino.
- [x] **2** (hecho el 2026-11-28) Leer todas las inscripciones con lo dominado: puerto en `sharedLogic`, versión en memoria y
  consulta en Room (decisión C). Tests: vacía, varias, solo las del usuario de la sesión, se actualiza al
  dominar.
- [x] **3** (hecho el 2026-11-28) `HomeViewModel`: junta inscripciones, cursos (red o copia) y la sesión. Un curso que no se
  puede cargar no rompe Inicio. Tests JVM con fakes.
- [x] **4** (hecho el 2026-11-28) Interfaz: `HomeHeader`, `ContinueCard`, `CourseProgressRow`, aviso sin cursos y `ScreenHome`,
  con previews y textos es/en. Tests de interfaz en español forzado y en inglés.
- [x] **5** (hecho el 2026-11-28) Navegación (Continuar y filas → camino; aviso → Buscar) y comprobación en el emulador.

## Progreso

- 2026-11-28: documento creado. Decisiones A y B aceptadas; empieza el paso 1.
- 2026-11-28: **paso 1 hecho.** `home/HomeSummary.kt` en `sharedLogic`: `EnrolledCourse` (entrada),
  `HomeCourse` (una fila: dominados, total, porcentaje, siguiente módulo, última actividad, `isFinished`),
  `HomeSummary` (`continueWith` y `courses`) y `homeSummary(...)`. Reutiliza `learningOrder`, `stateOf` y
  `masteredCount`. Los títulos se ordenan sin tildes ni mayúsculas (`sortKey`: en `commonMain` no hay
  `Collator`), así «Álgebra» va antes que «Kotlin».
  Tests: `HomeSummaryTest` (19), en rojo antes. **1089 tests de `sharedLogic`**, todos en verde.

- 2026-11-28: **paso 2 hecho.**
  - Puerto nuevo `EnrollmentsReader` (`sharedLogic`, `course/progress/`): `enrollments(): Flow<List<EnrollmentSnapshot>>`.
    Va aparte de `EnrollmentStore` (segregación de interfaces): no obliga a cambiar los fakes que ya hay.
    Reutiliza `EnrollmentSnapshot` (inscripción con lo dominado, la forma del contrato).
  - `InMemoryProgressStore` lo implementa con `UserProgress.snapshot()` (todo, pendiente o no).
  - `RoomProgressStore` lo implementa con dos consultas nuevas de solo lectura en `ProgressDao`
    (`watchEnrollments`, `watchMasteredModules`) juntas con `combine` y el Mapper `ProgressRows.toSnapshot()`.
    Sin sesión, lista vacía y termina (como los demás). El esquema exportado no cambia (sigue la v2).
  - Tests: `EnrollmentsReaderTest` (6, `sharedLogic`) y `RoomEnrollmentsReaderTest` (7, instrumentado),
    en rojo antes: vacía, fechas y módulos, solo el usuario de la sesión, lo que trae el servidor, avisa al
    dominar y al inscribirse, sin sesión. **1095 tests de `sharedLogic`, 557 JVM de la app.** Emulador:
    el nuevo más `RoomProgressFlowTest` y `RoomSettleFlowTest`, **14/14**.

- 2026-11-28: **paso 3 hecho.**
  - `home/Greeting.kt` (`sharedLogic`): `greetingName`, el primer nombre para «Hola, Ana» (null sin nombre:
    solo «Hola»). Test `GreetingTest` (5), en rojo antes.
  - `ui/screens/home/HomeViewModel.kt`: `HomeScreenState` (`Loading` / `Ready`) y `HomeUiState` (`name`,
    `initials`, `summary`, `unavailable`). Lee la sesión una vez y escucha `EnrollmentsReader` con
    `collectLatest`; descarga a la vez (`async`) solo los cursos que aún no tiene y guarda los encontrados;
    los que fallan no se guardan (se vuelven a pedir en el siguiente cambio) y se cuentan en `unavailable`.
    El cálculo es de `homeSummary`.
  - Test `HomeViewModelTest` (12), en rojo antes: cargando, saludo e iniciales, sin inscripciones, sin
    sesión, orden, dominar actualiza, inscribirse añade, cada curso se descarga una vez, curso que no carga,
    se vuelve a pedir, descargas a la vez, cambio en mitad de una descarga. **569 tests JVM de la app.**
  - Falta conectarlo: `AppContainer` todavía no expone el `EnrollmentsReader` (va con la pantalla, paso 4).

- 2026-11-28: plan del paso 4 aceptado: 4a cabecera, 4b «Tus cursos», 4c «Continúa», 4d pantalla y conexión.
- 2026-11-28: **paso 4a hecho.**
  - `HomeTexts.kt`: `homeDateText` (patrón `home_date_pattern` de cada idioma, nombres de Java con el
    Locale de la app y primera letra en mayúscula) y `greetingText` («Hola, Ana» / «Hola»).
  - `components/InitialsAvatar.kt`: el círculo de las iniciales, sacado de `ProfileRow` (era privado). Lo
    usan Ajustes e Inicio.
  - `HomeHeader.kt`: fecha, saludo (heading) e iniciales (TalkBack no las lee). La fecha llega por parámetro.
  - Tema (decisión G): `secondaryContainer` / `onSecondaryContainer` azules, en claro y en oscuro.
  - Tests: `HomeTextsTest` (5, JVM, en rojo antes) y `HomeHeaderTest` (5, instrumentado: fecha y saludo,
    heading, TalkBack sin iniciales, sin nombre, en inglés). **574 tests JVM de la app.** Emulador: el nuevo
    y `SettingsRowsTest` (la fila del perfil con el avatar extraído): todo en verde. El test nuevo falló una
    vez por el propio test: en el árbol sin fusionar las iniciales sí están (se pintan); TalkBack usa el
    fusionado.

- 2026-11-28: **paso 4b hecho.**
  - `HomeTexts.kt`: `courseInitial` (reutiliza `initialsOf`), `courseDetailText` («Luis Pérez · 16 de 20»,
    sin autor solo la cuenta), `percentText` («35 %» con espacio fijo; «35%» en inglés) y
    `courseRowDescription` (TalkBack: título, autor y «7 de 20 conceptos dominados», plural según el total;
    el porcentaje no se lee porque dice lo mismo).
  - `components/CourseInitialTile.kt`: el cuadrado con la inicial y `TileTone` (lila, ámbar, azul). El tono
    depende del id del curso, no de su sitio en la lista: no cambia de color al subir. Decorativo.
  - `CourseProgressRow.kt` (un botón para TalkBack, «Abrir curso», mínimo 64 dp) y `HomeCoursesSection.kt`
    («Tus cursos» como heading y la tarjeta con las filas separadas). `HomePreviewData.kt`: los cursos del
    Figma, pasados por `homeSummary`.
  - Tests: `HomeTextsTest` (+9, JVM, en rojo antes) y `HomeCoursesSectionTest` (8, instrumentado:
    heading, lo que se ve, un botón por curso, TalkBack y su acción, 48 dp, al tocar dice qué curso,
    singular, en inglés). **583 tests JVM de la app.** Emulador: 8/8 (la primera vez no compiló por dos
    imports del test).

- 2026-11-28: **paso 4c hecho.**
  - `HomeTexts.kt`: `masteredOfText` («12 de 20 conceptos dominados»; ahora también la usa
    `courseRowDescription`), `nextModuleText` («Siguiente: Grafos») y `continueDescription` (TalkBack).
  - `ContinueCard.kt`: `ContinueSection` (título como heading + tarjeta) y `ContinueCard`: inicial grande
    (56 dp), título y autor, cuenta, barra (`LinearProgressIndicator` en `primary` sobre `surfaceVariant`),
    «• Siguiente: …» en ámbar (`tertiaryContainer`, el color de la frontera del camino) y «Continuar ›».
    Toda la tarjeta es un solo botón con la acción «Continuar» (reutiliza `action_continue` e
    `ic_chevron_right`); la barra y la flecha son decorativas.
  - Tests: `HomeTextsTest` (+4, JVM, en rojo antes) y `ContinueCardTest` (7, instrumentado: heading, lo
    que se ve, un solo botón, TalkBack y su acción, tocar continúa, sin autor, en inglés).
    **587 tests JVM de la app.** Emulador: 7/7.

- 2026-11-28: **paso 4d y paso 5 hechos.**
  - `ScreenHome.kt` movida a `ui/screens/home/` (decisión H) y rehecha: `HomeActions` (`onOpenCourse`,
    `onSearch`); cargando (TalkBack: «Cargando tus cursos»), cabecera, «Continúa» (si hay
    `continueWith`), «Tus cursos» (si hay cursos), aviso sin cursos con «Buscar cursos» y aviso de cursos
    que no se han podido cargar (plural, `liveRegion`). Previews: con cursos, sin cursos y cargando.
    Se quita el texto `home_hello` (ya no se usa).
  - `HomeUiState.isEmpty` (sin cursos y sin cursos sin cargar) y `unavailableText` (null con 0).
  - Conexión: `ProgressModule.enrollmentsReader` (el mismo store), `AppContainer.enrollmentsReader`,
    `navigation/HomeRoute.kt` (como `SettingsRoute`: abrir un curso va a su camino, solo con la pantalla
    activa). En `MainNavGraph.kt` solo cambia la línea de Inicio, que le pasa `navigateToTab(SEARCH)`.
  - Arreglos vistos en el emulador: la etiqueta «Siguiente» se cortaba sin necesidad (ahora una caja
    ocupa lo que deja «Continuar ›»), y en oscuro la pista de la barra no se veía (ahora `outlineVariant`).
  - Tests: `HomeTextsTest` (+2) y `HomeViewModelTest` (+3), JVM, en rojo antes; `ScreenHomeTest` (8) y
    `ProgressModuleTest` (+2), instrumentados. **592 tests JVM de la app.** Emulador: los de Inicio y
    `ProgressModuleTest`, en verde.
  - Emulador con el servidor local y la cuenta de prueba: el servidor no tenía inscripciones de esta
    cuenta (la tabla es de la migración 006, de hoy; las de antes solo estaban en el móvil). Inicio vacío
    → «Search courses» lleva a Buscar → inscrita en «Introducción a los grafos» y «Curso de prueba con
    foto» → Inicio los enseña (el último, en «Continúa»). Continuar y cada fila abren su camino; «atrás»
    vuelve a Inicio. Visto en claro/inglés y oscuro/español; Ajustes quedó como estaba.

## Abierto

- (Resuelto el 2026-11-28) El saludo tras cambiar el nombre en «Editar perfil»: ver Progreso.

- 2026-11-28: **decisión J hecha.** `HomeSummary.otherCourses` (`sharedLogic`): todos menos `continueWith`;
  `courses` sigue con todos (lo usa `isEmpty`). `ScreenHome` pinta «Tus cursos» con `otherCourses` y no
  la saca si está vacía; `HomePreviewData.courses` la usa. Tests: `HomeSummaryTest` (+4, en rojo antes) y
  `ScreenHomeTest` (+2: no se repite, con un solo curso no sale «Tus cursos»). **1104 tests de
  `sharedLogic`, 592 JVM de la app.** Los dos tests de interfaz nuevos compilan pero **falta ejecutarlos**:
  el emulador se cerró por falta de memoria en el ordenador.

- 2026-11-28: `ScreenHomeTest` en el emulador: **10/10**. **Tarea terminada.**

- 2026-11-28: **el saludo se actualiza al volver a Inicio.** `HomeViewModel.onResume()` relee la sesión,
  como Ajustes, y `HomeRoute` lo llama con `LifecycleResumeEffect`. El saludo se guarda aparte (`Header`)
  y cada actualización de los cursos usa el último, así no vuelve el nombre viejo con el siguiente cambio
  de progreso. Tests: `HomeViewModelTest` (+4, en rojo antes: nombre nuevo, no toca ni descarga los
  cursos, se mantiene tras dominar, volviendo mientras carga). **596 tests JVM de la app.** Probado en el
  emulador (a petición de Joel): Inicio «Hello, Prueba» (PC) → Editar perfil «Beatriz Gil» → al volver,
  «Hello, Beatriz» (BG) sin reabrir la app; después, el nombre vuelve a «Prueba Curso» y el saludo también.

## Siguiente paso

Ninguno obligatorio. Opcional: escuchar Inicio con TalkBack a mano.
