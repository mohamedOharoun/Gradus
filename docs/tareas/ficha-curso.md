# Ficha de un curso

Pantalla intermedia entre **Buscar** (la hace el compañero, `buscar-cursos.md`) y el **camino**
(`cursos-backend-app.md`): al tocar un curso en los resultados se ve su ficha, y desde ella se entra
en el camino. Diseño hecho en Compose (el Figma no tiene esta pantalla y no admite más llamadas),
con el estilo de Buscar: `TagLabel`, `CourseTexts` y `ProblemMessage` del compañero se reutilizan.

Estado: **en curso**. Decisiones A–E aprobadas el 2026-11-22 (D cambiada: ver abajo).

## Objetivo

Antes de empezar un curso, ver de qué va: título, autor, etiquetas, cuántos módulos tiene, cuándo
se publicó, la descripción completa y la lista de lo que se aprende. Un botón lleva al camino.

## Decisiones ya tomadas (2026-11-22)

1. **Sin valoración** de momento: el backend no guarda valoraciones (también fuera de alcance en
   `buscar-cursos.md`). Se añade cuando existan.
2. **Con «Qué vas a aprender»**: la lista de módulos. Sale del mismo curso descargado: no hay
   peticiones nuevas.

## Boceto

```
 ←
 Introducción a los grafos                 headlineMedium, encabezado
 por Ana Ruiz                              onSurfaceVariant
 (grafos) (algoritmos) (estructuras…)      TagLabel, en FlowRow (todas, no solo 3)
 2 módulos · nov 2026                      como la tarjeta de Buscar

 Cómo se guarda un grafo en memoria y      descripción completa
 cómo se recorre en anchura (BFS).

 Qué vas a aprender                        titleMedium, encabezado
  1  Representar un grafo
  2  Recorrido en anchura (BFS)
┌──────────────────────────────────────┐
│          Empezar el curso  →          │  fijo abajo (decisión C)
└──────────────────────────────────────┘
```

- Mientras se descarga: rueda y «atrás». Si falla: el problema y, si sirve, «Reintentar»
  (los mismos casos que el camino: no existe, sin conexión, formato nuevo, fallo del servidor).
- En horizontal o en tablet, como mucho 560 dp de ancho, centrado (como la práctica).

## Decisiones (aprobadas el 2026-11-22)

- **A · De dónde salen el autor y la fecha.** Hoy `CourseOutcome.Found(course)` solo trae el
  contenido; el autor, la versión y la fecha llegan en la misma respuesta (`GET /api/courses/{id}`)
  pero el *mapper* los ignora. Propuesta: `Course` gana `publication: Publication?` con
  `Publication(author, version, publishedAt)`. Es `null` en los cursos de ejemplo. Así no cambia la
  forma de `CourseOutcome` ni los usos que ya hay (camino, estudio, búsqueda del compañero).
  Alternativa: `Found(course, summary)`, más limpia en teoría pero obliga a tocar todos los usos y
  sus tests, también los del compañero.
- **B · Orden de «Qué vas a aprender»: el del camino** (de arriba abajo por niveles de
  prerrequisitos, como se dibuja), no el del JSON. Así la lista y el camino coinciden. Es una
  función pura nueva en `sharedLogic` (`Course.learningOrder()`), sobre `pathLayout()`.
- **C · El botón.** «Empezar el curso» y, si ya se ha dominado algún módulo, «Continuar el curso»
  con «1 de 2 dominados» encima (sale de `ProgressStore`, que ya existe).
- **D · La línea de Buscar: la cambia el compañero** (cambiado al aprobar). Hoy, tocar una tarjeta
  navega a `Route.Course(id)` (su código en `MainNavGraph.kt`) y debe pasar a
  `Route.CourseDetail(id)`. **No se toca nada suyo** (ni su código ni `buscar-cursos.md`) para no
  generar conflictos al fusionar: se le avisa y lo hace él cuando `Route.CourseDetail` exista.
- **E · El botón provisional de Inicio no se cambia** (cambiado en la tarea 4): sigue abriendo el
  camino. El compañero lo borra en su tarea 9 de `buscar-cursos.md`; si aquí se modificaran esas
  mismas líneas, git daría conflicto al fusionar. Para probar la ficha en el emulador se cambió
  solo un momento y se dejó como estaba.

## Tareas

### Parte A · Datos (`sharedLogic`, test primero)

1. ✅ **Hecha (2026-11-22).** **`Publication` en el modelo y en el *mapper*** (decisión A).
   - `course/Course.kt`: `data class Publication(val author: Author, val version: Int, val publishedAt: String)`
     y `Course.publication: Publication? = null`.
   - `course/http/CourseDtos.kt` y `CourseDtoMappers.kt`: `CourseVersionDto` lee también `version`,
     `author` y `publishedAt`; `toCourse` los pasa a `Publication`.
   - `FakeCourseGateway`: sin cambios (sus cursos no tienen publicación).
   - Si una respuesta no trae autor o fecha (no pasa con el backend de hoy), el curso se lee igual
     con `publication = null`: así no hay que tocar `HttpCourseGatewayTest`, que también es del
     compañero, y la ficha simplemente no enseña esa línea.
   - ✅ Criterio: `CourseDtoMappersTest` (el curso de ejemplo trae su autor, versión y fecha; sin
     esos campos, `publication` es null; un curso creado a mano no tiene); los demás tests, sin
     tocarlos, siguen pasando.
2. ✅ **Hecha (2026-11-22).** **`Course.learningOrder()`** (decisión B).
   - `course/LearningOrder.kt` (fichero propio): los módulos en el orden del camino, por
     fila y, dentro de la fila, de izquierda a derecha.
   - ✅ Criterio: tests con un curso en cadena, uno con una fila de 3 y el curso de ejemplo de la
     app (el orden coincide con el dibujo).

### Parte B · Pantalla (`app`)

3. ✅ **Hecha (2026-11-22).** **Pantalla sin estado (`ScreenCourseDetail`)** y sus textos.
   - `ui/screens/detail/ScreenCourseDetail.kt` + `CourseDetailActions` (`onStart`, `onRetry`,
     `onBack`); `ui/screens/detail/CourseDetailTexts.kt` (funciones puras: «por Ana Ruiz»,
     «2 módulos · nov 2026» reutilizando `moduleCountText` y `PublishedMonth`, el texto del botón).
   - Carga y problemas: `CourseLoadingContent` y `CourseProblemContent` del camino (ya existen).
   - TalkBack: título y «Qué vas a aprender» son encabezados; cada módulo de la lista se lee
     entero («1, Representar un grafo»); botón de 48 dp.
   - Previews en claro y oscuro: lista corta, descripción larga, con progreso, cargando y problema.
   - ✅ Criterio: `CourseDetailTextsTest` (JUnit) y `ScreenCourseDetailTest` (emulador): lo que se
     ve y lo que lee TalkBack en cada estado, y que el botón llama a `onStart`.
4. ✅ **Hecha (2026-11-22).** **`CourseDetailViewModel` y la ruta.**
   - `CourseDetailViewModel(courseId, gateway, progress)`: descarga con el gateway (con caché: al
     entrar después en el camino ya no se descarga otra vez) y escucha el progreso, como
     `CourseViewModel`. Test con `FakeCourseGateway` (`app/src/test`, como `SearchViewModelTest`).
   - `Route.CourseDetail(courseId)`; en `MainNavGraph`, su destino (sin barra) y «Empezar el curso»
     → `Route.Course(courseId)`; «atrás» vuelve a donde se estaba.
   - La tarjeta de Buscar la cambia el compañero (decisión D) y el botón de Inicio no se toca
     (decisión E).
   - `MainNavGraph.kt` es compartido: el destino va en un fichero propio
     (`navigation/CourseDetailDestination.kt`) y en `MainNavGraph.kt` solo se añade **una línea**,
     `courseDetailDestination(navController)`, tras el destino de la práctica.
   - ✅ Criterio, en el emulador con `gradus.sh up`: Buscar → tarjeta → ficha → «Empezar el curso»
     → camino; atrás vuelve a la ficha y a Buscar con la búsqueda igual; sin servidor, «Sin
     conexión» y «Reintentar».
5. **Repaso final**: modo oscuro, TalkBack, `especificacion.md` §5 (la ficha) y `resumen-proyecto.md`.
   El aviso al compañero (decisión D) va en el resumen al usuario, no en su documento.

## Fuera de alcance

- Valoraciones (decisión 1) e inscribirse («Inscrito»): no existen en el backend.
- Ver otras versiones del curso (`GET /api/courses/{id}/versions/{v}`).
- Cursos del mismo autor o parecidos.

## Progreso

- 2026-11-22: documento creado. Decisiones 1, 2 y A–E aprobadas; D cambiada: lo de Buscar lo hace el compañero.

- 2026-11-22: **tarea 1 hecha**.
  - `Course.kt`: `Publication(author, version, publishedAt)` y `Course.publication` (null por defecto).
  - `CourseDtos.kt`: `CourseVersionDto` lee también `version`, `author` y `publishedAt` (opcionales).
  - `CourseDtoMappers.kt`: `toPublication()` (solo si llegan los tres) y `toCourse(id, publication)`.
  - 4 tests nuevos en `CourseDtoMappersTest`; 476 en total. Solo ficheros nuestros (ninguno tiene
    cambios del compañero): no habrá conflictos al fusionar.

- 2026-11-22: **tarea 2 hecha**.
  - `course/LearningOrder.kt`: `Course.learningOrder()` ordena los nodos de `pathLayout()` por
    fila y, dentro de la fila, por `x`. No repite reglas del camino: las reutiliza.
  - `LearningOrderTest`: 6 tests (cadena escrita al revés, fila de izquierda a derecha, nivel
    partido en dos filas, rombo sin repetir, curso vacío y el curso de ejemplo del backend).
    482 en total. Ningún fichero del compañero.

- 2026-11-22: **tarea 3, código hecho** (todo en `app/.../ui/screens/detail/`, carpeta nueva).
  - `CourseDetailTexts.kt`: «por Ana Ruiz», «2 módulos · nov 2026» y su lectura («… publicado en
    noviembre de 2026»), el botón («Empezar», «Continuar» o «Repasar el curso») y «1 de 2
    dominados». Reutiliza `moduleCountText`, `shortLabel` y `spokenLabel` del compañero sin tocarlos.
  - `CourseDetailState.kt`: `Loading | Problem(CourseProblem) | Ready(course, mastered)` (con
    `modules` en el orden del camino) y `CourseDetailActions`.
  - `CourseDetailContent.kt` (cabecera y «Qué vas a aprender»; cada módulo se lee entero, con
    «dominado» si lo está) y `ScreenCourseDetail.kt` (botón fijo abajo; carga y problemas con las
    pantallas del camino).
  - `ScreenCourseDetailPreviews.kt`: nuevo, con progreso, todo dominado, sin publicación,
    cargando y sin conexión (claro y oscuro).
  - Tests: `CourseDetailTextsTest` (10, JUnit) y `ScreenCourseDetailTest` (9, emulador). En el
    emulador pasan los 135 tests instrumentados de la app (también los del compañero).
  - Vista en el emulador en claro (curso nuevo: «Empezar el curso») y en oscuro (3 de 10
    dominados, con ✓ y «Continuar el curso»).
  - Ningún fichero del compañero.

- 2026-11-22: **tarea 4 hecha**.
  - `screens/detail/CourseDetailViewModel.kt`: descarga el curso (con la caché) y escucha el
    progreso; «Reintentar» solo si sirve. 7 tests en `CourseDetailViewModelTest` (reloj simulado,
    como `SearchViewModelTest`); 88 tests de la app y 482 de `sharedLogic`.
  - `screens/course/CourseViewModel.kt`: `CourseOutcome.problem()`, la traducción de un fallo a
    `CourseProblem`, ahora compartida por el camino y la ficha (antes estaba dentro de `load()`).
  - `Routes.kt`: `Route.CourseDetail(courseId)`. `navigation/CourseDetailDestination.kt`: el destino;
    «Empezar el curso» → `Route.Course(id)` y «atrás» vuelve (los dos con `dropUnlessResumed`).
  - `MainNavGraph.kt`: una sola línea añadida (comprobado con `git diff`).
  - Emulador, con el servidor local y el botón de Inicio cambiado solo para la prueba: la ficha
    enseña los datos reales («por Autora Cursos», 3 etiquetas, «2 módulos · nov 2026»);
    «Empezar el curso» abre el camino; atrás vuelve a la ficha y luego a Inicio; tras dominar
    «Representar un grafo», la ficha enseña ✓, «1 de 2 dominados» y «Continuar el curso».
  - Sin conexión no se probó a mano (Inicio busca antes y falla primero); lo cubren
    `CourseDetailViewModelTest` y `ScreenCourseDetailTest`.
  - **Para el compañero:** en su `onCourseClick` de la pestaña Buscar, `Route.Course(id)` →
    `Route.CourseDetail(id)`. Es lo único que falta para llegar a la ficha desde Buscar.

## Siguiente paso

**Tarea 5**: repaso final (modo oscuro y TalkBack ya probados en la tarea 3; `especificacion.md` §5
y `resumen-proyecto.md`).
