# Buscar cursos (con filtros)

Pestaña **Buscar** del Figma (pantallas «3 · Buscar» y «4 · Buscar con filtros»): lista de cursos
del catálogo del servidor, búsqueda por texto y filtros en una hoja inferior. Al tocar un curso se
abre su camino (`Route.Course(courseId)`, ya hecho en `docs/tareas/cursos-backend-app.md`, tarea 7).

Contrato de la API: `backend/docs/api-cursos.md` (`GET /api/courses?q=&tag=&language=&page=&size=`).
Especificación: `docs/especificacion.md`, «Con sesión → Buscar».
Diseño: captura del Figma en `docs/tareas/resources/search-courses.png` (las dos pantallas). Es una
foto del 2026-11-21: si no coincide con el Figma, manda el Figma.

Estado: **decisiones 1–10 aprobadas** (2026-11-21). Sin empezar.

## Objetivo

1. Ver la lista de cursos publicados nada más entrar en Buscar (los más recientes primero).
2. Buscar por texto mientras se escribe, con el número de resultados.
3. Filtrar por etiquetas en una hoja inferior; los filtros activos se ven como chips que se quitan.
4. Abrir un curso desde su tarjeta.
5. Dejar la lógica en `sharedLogic` para que una app de iOS solo tenga que rehacer las vistas.

## Figma frente a la API de hoy

| En el Figma | En la API | Propuesta |
|---|---|---|
| Caja de búsqueda | `q` (título o descripción) | Fase 1 |
| «8 resultados» | `total` de la página | Fase 1 |
| Tarjeta: título, «por Ana Ruiz», etiquetas | metadatos (`title`, `author.name`, `tags`) | Fase 1 |
| «20 conceptos» | `moduleCount` | Fase 1, como **«20 módulos»** (renombrado ya decidido) |
| «oct 2026» | `publishedAt` | Fase 1 |
| Etiquetas (varias) | `tag` repetible; el curso debe tenerlas **todas** | Fase 1 |
| «Ver todas» (lista de etiquetas) | no hay ruta de etiquetas | Decisión 3 |
| Creador | el dato está (`author.name`), pero **no se puede filtrar** por él | Decisión 1 → fase 2 |
| Fecha de publicación (cualquiera, último mes, último año) | el dato está (`publishedAt`), pero **no se puede filtrar** | Decisión 1 → fase 2 |
| Ordenar: más recientes | orden fijo: más recientes | Ya es así; sin control mientras sea la única opción |
| Ordenar: mejor valorados · ★ 4,7 (32) | **no hay valoraciones**, ni los datos (backend §8: fuera de alcance) | Fuera de alcance |
| «Relevancia» (hoja y encima de la lista) | — | **Errata del Figma**: no existe (decisión 10) |
| «Inscrito» | **no hay inscripción** todavía | Fuera de alcance |
| «Mostrar 8 cursos» | `total` de una búsqueda con el borrador | Decisión 6 |
| — | `language` | Fuera de alcance (no está en el Figma; añadirlo después es trivial) |

## Idea de diseño (código)

Mismo enfoque que el camino y la práctica: **lógica pura en `sharedLogic`** (se prueba con
`kotlin.test` y sirve tal cual en iOS) y, en `app`, un ViewModel fino (corrutinas) y pantallas sin
estado. La interfaz va en **`:app`**, no en `:androidApp` (que es la plantilla KMP sin usar).

```
sharedLogic/commonMain/.../course/
├── CourseGateway                 search(query: CourseQuery, page)  (+ extensión search(text, page))
├── http/HttpCourseGateway        CourseQuery → ?q=…&tag=…&tag=…&page=…&size=…
├── FakeCourseGateway             también filtra por etiquetas (previews y tests)
├── PublishedMonth                "2026-10-03T08:35:00Z" → año 2026, mes 10 (sin java.time)
└── search/
    ├── CourseQuery               texto + etiquetas (fase 2: creador y fecha). Inmutable.
    ├── ActiveFilter              los chips quitables y el número del distintivo «Filtros 2»
    ├── CourseSearchState         máquina de estados: resultados, páginas, cargando, problema
    ├── TagSuggestions            qué etiquetas ofrecer en la hoja y en qué orden
    └── FilterSheetState          la hoja de filtros: borrador, sugerencias, «Ver todas», qué aplicar
                                  y el recuento del borrador («Mostrar N cursos», `DraftCount`)
app/.../ui/
├── screens/ScreenSearch.kt                 (ya existe; deja de ser provisional)
├── screens/search/SearchViewModel.kt       espera al teclear, cancela la búsqueda anterior
├── screens/search/SearchActions.kt         eventos de la pantalla (como LoginActions)
├── screens/search/SearchResultsContent.kt  lista, cargando, vacío, problema, «Cargar más»
├── screens/search/SearchTexts.kt           «8 resultados», vacío y problemas (funciones puras)
├── screens/search/FilterBar.kt             fila bajo la caja: «Filtros n» y chips quitables
├── screens/search/FilterSheet.kt           hoja inferior (ModalBottomSheet)
├── screens/search/FilterTexts.kt           textos de los filtros (funciones puras)
└── components/
    ├── SearchField.kt            caja redondeada con lupa y ✕
    ├── CourseCard.kt             tarjeta de un resultado
    ├── CourseTexts.kt            «20 módulos · oct 2026» y sus partes (funciones puras)
    ├── ProblemMessage.kt         título, texto y «Reintentar» (reutilizable)
    ├── TagLabel.kt               etiqueta gris de la tarjeta
    └── FilterChips.kt            «Filtros n», chip quitable y chip seleccionable
app/.../data/DemoSearchResults.kt           los 3 cursos del Figma, para las previews
```

**Patrones** (para defenderlos igual que en cuentas y cursos):

- **Objeto inmutable + funciones puras** (estilo funcional): `CourseQuery.toggleTag("grafos")`
  devuelve una copia nueva; nada se modifica en sitio.
- **Máquina de estados / reductor**: `CourseSearchState` recibe sucesos (`started`, `pageLoaded`,
  `moreRequested`) y devuelve el estado siguiente. Una respuesta de una búsqueda ya superada se ignora.
- **Puerto/adaptador** (ya existe) y ***Decorator*** (`CachingCourseGateway` deja pasar la búsqueda).
- ***Mapper***: `CourseQuery` → parámetros de la URL, como función pura y probada.
- ***Factory method***: `SearchViewModel.factory(...)` y `CourseQuery.EMPTY`.

**Qué haría falta en iOS:** las vistas (SwiftUI) y un equivalente del ViewModel (la espera al
teclear y lanzar las peticiones). Modelo, reglas, estados y red se reutilizan de `sharedLogic`.

## Alcance (fase 1: con la API de hoy)

- Lista inicial (sin texto ni filtros): todos los cursos, los más recientes primero.
- Búsqueda al escribir, «N resultados», tarjetas, «Cargar más».
- Estados: cargando (sin borrar los resultados anteriores), sin resultados, sin conexión y fallo
  del servidor, con «Reintentar».
- Fila de filtros: «Filtros» con su número y un chip quitable por etiqueta («#grafos ✕»).
- Hoja de filtros: **Etiquetas** (varias, «Ver todas»), «Restablecer», ✕ y «Mostrar N cursos».
- Tocar una tarjeta abre el curso; «atrás» vuelve a Buscar tal como estaba.
- Previews en claro y oscuro, TalkBack y zonas táctiles de 48 dp.

## Fuera de alcance

- **Valoraciones** (★ y «Mejor valorados»): no existen en el backend.
- **«Inscrito»**: no hay inscripción. Llegará con Mis cursos / Room.
- Filtro por idioma, historial de búsquedas y búsqueda sin conexión.
- **Ordenar por**: solo tendría sentido con «Mejor valorados», que necesita valoraciones.
- La parte de backend de la **fase 2** (creador y fecha) se detalla en su propio documento
  de `backend/docs/tareas/` (parte C).

## Decisiones (aprobadas el 2026-11-21)

1. **Filtros de creador y fecha: fase 2.** Los datos **ya están** en el backend (el nombre
   del autor sale de `users` y la fecha es `published_at` de `course_versions`); lo que falta es
   **exponerlos como parámetros** de `GET /api/courses`, por ejemplo `author=` (subcadena del
   nombre) y `published=month|year`. En el backend es una condición más en `CourseSearch` /
   `CourseSearchSql`. La fase 1 no los muestra. Filtrarlos en el móvil no sirve: rompería la
   paginación y el número de resultados. Las **valoraciones** son otra cosa: no existen los datos
   (habría que guardarlas, valorar y calcular la media).
2. **Tarjeta sin valoración ni «Inscrito»:** la línea de abajo queda «20 módulos · oct 2026».
3. **Las etiquetas de la hoja salen de los resultados ya cargados** de la búsqueda aplicada:
   primero las elegidas, luego las más repetidas y, a igualdad, por orden alfabético. Se ven 7 y
   «Ver todas» despliega el resto. Así, cualquier etiqueta que se elija da al menos un resultado.
   Si se queda corto, en la fase 2 se añade `GET /api/courses/tags`.
4. **Se busca al escribir, tras 300 ms sin teclear**; cada búsqueda nueva cancela la anterior
   (gana la última). La tecla «Buscar» del teclado busca al momento y lo cierra. Se hace con un
   `Job` y `delay`, como `CourseViewModel` cancela su carga (sin APIs en preview).
5. **Paginación**: botón **«Cargar más»** al final de la lista, como ya preveía
   `cursos-backend-app.md` (más simple y accesible que cargar al llegar al final).
6. **«Mostrar N cursos»**: al cambiar el borrador se busca con él (300 ms de espera) y se enseña
   su `total`; al pulsar, se usan esos resultados sin volver a pedirlos. Mientras tanto: «Mostrar
   cursos». Es una tarea aparte (la 8) por si se complica.
7. **La hoja trabaja con un borrador.** ✕, deslizar hacia abajo o «atrás» lo descartan;
   «Restablecer» vacía los filtros del borrador (no el texto); «Mostrar N cursos» lo aplica. Los
   chips de fuera de la hoja se quitan al momento.
8. **Este documento va en `docs/tareas/`**, como dice `AGENTS.md` (se escribió primero en
   `.ia/plans/search-course.md` y se ha movido aquí).
9. **Botón provisional de Inicio** («Abrir curso del servidor» y `latestCourseId` en
   `MainNavGraph.kt`): se quita en la última tarea, como dice `cursos-backend-app.md`.
10. **«Relevancia» es una errata del Figma.** Los órdenes son solo «Mejor valorados» y «Más
    recientes». Como no hay valoraciones, el único orden posible es «Más recientes», que ya es el
    del backend: **no se muestra «Ordenar por»** (ni en la hoja ni encima de la lista) y no hace
    falta `sort` en la API. Aparecerá cuando existan las valoraciones.

## Tareas

### Parte A · Lógica (`sharedLogic`, test primero)

1. ✅ **Hecha (2026-11-21).** **`CourseQuery` y `ActiveFilter`.**
   - `CourseQuery(text, tags)` con `withText`, `toggleTag`, `withoutTag`, `withoutFilters` y
     `activeFilters` (lista ordenada de chips; su tamaño es el número de «Filtros 2»).
   - Ficheros: `sharedLogic/src/commonMain/kotlin/es/ulpgc/pamn/dominio/course/search/` y sus tests
     en `sharedLogic/src/commonTest/kotlin/es/ulpgc/pamn/dominio/course/search/`. No se toca nada
     más (el puerto cambia en la tarea 2).
   - Reglas:
     - `CourseQuery.EMPTY`: sin texto ni etiquetas.
     - `text` se guarda **tal como se escribe** (la caja de búsqueda lo enseña así); recortarlo
       es cosa del adaptador HTTP, que ya lo hace. Un texto en blanco cuenta como «sin texto».
     - Las etiquetas van en **el orden en que se eligen** y sin repetir. Dos etiquetas son la misma
       si coinciden **sin distinguir mayúsculas** y sin espacios de sobra, como en el backend
       (`TextNormalizer`); se guarda la primera forma elegida.
     - `toggleTag`: la añade si no está y la quita si está. `withoutTag` de una que no está no cambia nada.
     - `withoutFilters` quita las etiquetas y **conserva el texto** («Restablecer», decisión 7).
     - `ActiveFilter` es una *sealed interface* con `Tag(name)` (en la fase 2, creador y fecha).
       El texto **no** es un filtro activo: no tiene chip ni suma en «Filtros n».
   - ✅ Criterio: tests de cada regla (etiqueta repetida con otras mayúsculas, quitar la que no está,
     texto en blanco, restablecer conserva el texto, orden de los chips). Pasan con
     `./gradlew :sharedLogic:testAndroidHostTest`, junto con los que ya había.
2. ✅ **Hecha (2026-11-21).** **La búsqueda del puerto recibe un `CourseQuery`** (decisiones aprobadas el 2026-11-21).
   - Ficheros de `sharedLogic/src/commonMain/kotlin/es/ulpgc/pamn/dominio/course/`:
     - `CourseGateway.kt`: `suspend fun search(query: CourseQuery, page: Int = 0): SearchOutcome`.
       Debajo de la interfaz, una **extensión** para buscar solo por texto:
       `suspend fun CourseGateway.search(text: String, page: Int = 0) = search(CourseQuery(text = text), page)`.
       Se queda: los tests que ya llaman a `search("grafos")` no cambian y sirven de red.
     - `http/HttpCourseGateway.kt`: una función privada en el mismo fichero (sin fichero aparte),
       `appendSearch(query)`, que añade a los parámetros de la URL `q` (recortado) solo si
       `query.hasText` y un `tag` por etiqueta, en su orden. `page` y `size` (20), como ahora.
     - `FakeCourseGateway.kt`: `searchAnswer: (CourseQuery, Int) -> SearchOutcome` y
       `searches: List<Pair<CourseQuery, Int>>`. El curso debe tener **todas** las etiquetas,
       comparadas con la misma regla que `CourseQuery`, sin copiarla: `comparisonKey` pasa de
       `private` a `internal` en `search/CourseQuery.kt` y se renombra a `tagKey` (al ser
       `internal` se ve en todo `sharedLogic`, y el nombre dice para qué sirve).
     - `CachingCourseGateway.kt`: pasa la consulta tal cual; las búsquedas no se guardan.
   - Una línea de `app`: en `latestCourseId` de `ui/navigation/MainNavGraph.kt`,
     `gateway.search(query = "")` → `gateway.search(CourseQuery.EMPTY)` (con su `import`). Sin este
     cambio no compila: el nombre `query` lleva a la función nueva, que espera un `CourseQuery`.
     Ese código se borra en la tarea 9.
   - No cambia: el adaptador sigue recortando el texto; las etiquetas se mandan como se guardaron
     (el backend las normaliza); los desenlaces (`SearchOutcome`) son los mismos.
   - ✅ Criterio:
     - `HttpCourseGatewayTest`, 3 tests nuevos: dos etiquetas → `parameters.getAll("tag")` es
       `["grafos", "árboles"]`; sin etiquetas no hay `tag`; texto y etiquetas juntos.
     - `FakeCourseGatewayTest`: 2 tests nuevos (exige todas las etiquetas; sin distinguir
       mayúsculas) y se adapta `it remembers what it was asked` (`CourseQuery(text = "grafos") to 1`).
     - Los demás tests, sin tocarlos, siguen pasando (`./gradlew :sharedLogic:testAndroidHostTest`)
       y la app compila (`./gradlew :app:assembleDebug`).
3. ✅ **Hecha (2026-11-21).** **`CourseSearchState`** (decisiones aprobadas el 2026-11-21).
   - Qué es: lo que enseña la lista de Buscar (resultados, cuántos hay, si quedan más, si carga o
     ha fallado). **Máquina de estados inmutable**, como `ModuleStudy`: cada acción devuelve un
     estado nuevo y, si no toca, el mismo. **Sin corrutinas ni red**: el ViewModel (tarea 6) lee la
     petición pendiente, llama al gateway y le pasa la respuesta.
   - Ficheros: `sharedLogic/src/commonMain/kotlin/es/ulpgc/pamn/dominio/course/search/CourseSearchState.kt`
     y `sharedLogic/src/commonTest/kotlin/es/ulpgc/pamn/dominio/course/search/CourseSearchStateTest.kt`.
     No se toca nada más.
   - Forma (nombres aprobados, decisión D):
     ```kotlin
     data class SearchRequest(val query: CourseQuery, val page: Int)
     enum class SearchProblem { UNREACHABLE, SERVER_FAILURE }   // los dos se pueden reintentar

     sealed interface SearchStatus {
         data class Loading(val request: SearchRequest) : SearchStatus      // primera página
         data class LoadingMore(val request: SearchRequest) : SearchStatus  // «Cargar más»
         data object Ready : SearchStatus
         data class Failed(val problem: SearchProblem) : SearchStatus       // falló la primera página
         data class MoreFailed(val problem: SearchProblem) : SearchStatus   // falló «Cargar más»
     }

     data class CourseSearchState(
         val query: CourseQuery,             // la consulta aplicada (no lo que se está tecleando)
         val results: List<CourseSummary>,
         val total: Int,
         val nextPage: Int,
         val hasMore: Boolean,
         val status: SearchStatus,
     ) {
         val pending: SearchRequest?         // la de Loading o LoadingMore; null en los demás
         val canLoadMore: Boolean            // Ready y hasMore
         val isEmpty: Boolean                // Ready y sin resultados
         fun search(query: CourseQuery): CourseSearchState
         fun loadMore(): CourseSearchState
         fun retry(): CourseSearchState
         fun receive(request: SearchRequest, outcome: SearchOutcome): CourseSearchState
         companion object { fun start(query: CourseQuery = CourseQuery.EMPTY): CourseSearchState }
     }
     ```
     La petición pendiente va **dentro** de `Loading` y `LoadingMore`: así no se puede escribir un
     estado imposible («listo, pero esperando respuesta»).
   - Reglas:
     - `start(query)` (*factory method*): `Loading` de la página 0, sin resultados.
     - `search(query)`: **siempre** empieza de nuevo (`Loading`, página 0), aunque la consulta sea
       la misma, y **conserva los resultados anteriores** mientras carga (la lista no parpadea).
     - **Respuestas tardías (decisión A):** `receive` solo acepta la respuesta si su `request` es
       igual a `pending`; si no (búsqueda ya superada, «más» de una búsqueda anterior o nada
       pendiente), devuelve el mismo estado.
     - Primera página recibida: sustituye los resultados; `total`, `hasMore` y `nextPage = 1`; `Ready`.
     - `loadMore()`: solo si `canLoadMore` → `LoadingMore` de `nextPage`, con los resultados.
     - Página siguiente recibida: **añade sin repetir ningún curso por `id` (decisión C)**. Si se
       publica un curso entre página y página, todo se desplaza un puesto y uno se repetiría; en
       la tarea 6 eso rompe la `LazyColumn` (`key = id`). `total` y `hasMore` son los de esa página.
     - Falla la primera página: `Failed`, **sin resultados (decisión B)**: los anteriores eran de
       otra búsqueda.
     - Falla «Cargar más»: `MoreFailed`, **conservando los resultados**.
     - `retry()`: `Failed` → `Loading` de la página 0; `MoreFailed` → `LoadingMore` de `nextPage`;
       en otro estado, el mismo.
     - `SearchOutcome.Unreachable` → `UNREACHABLE`; `SearchOutcome.ServerFailure` → `SERVER_FAILURE`.
   - Fuera de esta tarea: el texto que se está tecleando, la espera de 300 ms y cancelar (tarea 6,
     con corrutinas); los textos de pantalla («N resultados», avisos), en `app`. La tarea 8
     reutilizará esta máquina para el borrador de la hoja (otro `CourseSearchState`).
   - ✅ Criterio: tests con `kotlin.test`, sin corrutinas, en `CourseSearchStateTest`:
     - empezar; primera página con resultados y vacía (`isEmpty`);
     - cargar más, y no cargar si no hay más, mientras carga o mientras carga más;
     - página siguiente añadida y sin repetir un `id`;
     - búsqueda nueva conservando los resultados;
     - respuesta tardía de una búsqueda anterior; «más» tardío tras una búsqueda nueva;
       respuesta sin nada pendiente;
     - fallo de la primera página (los dos problemas) y de «más»;
     - reintentar tras cada fallo, y reintentar sin fallo.
     - Pasan con `./gradlew :sharedLogic:testAndroidHostTest`, junto con los que ya había.
4. ✅ **Hecha (2026-11-21).** **`TagSuggestions` y `PublishedMonth`** (decisiones aprobadas el 2026-11-21).
   - Qué es: dos funciones puras e independientes. `TagSuggestions`: qué etiquetas ofrecer en la
     hoja y en qué orden (la usa la tarea 7). `PublishedMonth`: el año y el mes de `publishedAt`,
     para «oct 2026» en la tarjeta (tarea 5). Sin corrutinas ni red.
   - Ficheros (no se toca nada más):
     - `sharedLogic/src/commonMain/kotlin/es/ulpgc/pamn/dominio/course/search/TagSuggestions.kt`
       y su test en `sharedLogic/src/commonTest/kotlin/es/ulpgc/pamn/dominio/course/search/TagSuggestionsTest.kt`.
     - `sharedLogic/src/commonMain/kotlin/es/ulpgc/pamn/dominio/course/PublishedMonth.kt` y su test
       en `sharedLogic/src/commonTest/kotlin/es/ulpgc/pamn/dominio/course/PublishedMonthTest.kt`.
       Va en `course/`, junto a `CourseSummary`: es un dato del curso, no de la búsqueda (decisión C).
   - Forma:
     ```kotlin
     // course/search/TagSuggestions.kt
     data class TagSuggestions(val shown: List<String>, val hidden: List<String>) {
         val all: List<String> get() = shown + hidden        // tras pulsar «Ver todas»
         val hasHidden: Boolean get() = hidden.isNotEmpty()  // ¿se enseña «Ver todas»?
         companion object {
             const val SHOWN = 7
             // Factory method: a partir de los resultados cargados y las etiquetas elegidas.
             fun from(results: List<CourseSummary>, selected: List<String>, shown: Int = SHOWN): TagSuggestions
         }
     }

     // course/PublishedMonth.kt
     data class PublishedMonth(val year: Int, val month: Int) {   // month: 1–12
         companion object { fun parse(publishedAt: String): PublishedMonth? }  // null si no se entiende
     }
     val CourseSummary.publishedMonth: PublishedMonth? get() = PublishedMonth.parse(publishedAt)
     ```
     Los textos «ene», «feb»… no van aquí: son de la pantalla (`app`, tarea 5).
   - Reglas de `TagSuggestions`:
     - **Orden:** primero las elegidas, en el orden en que se eligieron; después, las que están en
       más cursos; a igualdad, alfabético.
     - **Alfabético sin mayúsculas ni tildes (decisión A):** para ordenar, á é í ó ú ü cuentan como
       su vocal («árboles» antes que «bases de datos»). En `commonMain` no hay orden alfabético
       español sin librería; la «ñ» se queda como está (en etiquetas es raro).
     - **Cuántas veces está:** en cuántos cursos de `results`; cada curso cuenta una vez aunque
       repita la etiqueta (`DUPLICATE_TAG` solo es un aviso del backend). Dos etiquetas son la misma
       si coinciden con `tagKey` («Grafos» = «grafos»).
     - **Qué forma se enseña:** la elegida, tal como está en `selected` (coincide con su chip
       «#grafos ✕»); si no está elegida, la primera que aparece en `results` (la del curso más reciente).
     - **Las elegidas se enseñan siempre**, aunque no salgan en los resultados (si no, no se
       podrían quitar desde la hoja).
     - **Cuántas se ven:** `shown` (7) y el resto en `hidden`. Si hay más de 7 elegidas, en `shown`
       van todas las elegidas.
     - Una etiqueta en blanco se ignora. Sin resultados ni elegidas: las dos listas vacías.
     - Nota para la tarea 7 (decisión B): se calcula **al abrir la hoja** con las etiquetas
       *aplicadas*, no las del borrador. Si siguiera al borrador, al tocar una etiqueta saltaría al
       principio y los chips se moverían bajo el dedo.
   - Reglas de `PublishedMonth`:
     - Solo lee el principio, `AAAA-MM`; el resto (día, hora, zona) no hace falta.
     - `null` si el mes no está entre 1 y 12, si está vacío o si no empieza así. La tarjeta enseñará
       entonces solo «20 módulos».
     - **El mes es el de la hora UTC (decisión D)**, sin ajustar a la zona del móvil: ajustarlo
       necesita `kotlinx-datetime` (dependencia nueva) y no compensa por un día de diferencia.
   - Criterio: tests con `kotlin.test`, sin corrutinas:
     - `TagSuggestionsTest`: orden por frecuencia; empate alfabético con tildes; elegidas primero
       y en su orden; una elegida que no está en los resultados; la misma etiqueta con otras
       mayúsculas cuenta una vez y se enseña con su primera forma; un curso que repite una etiqueta
       cuenta una vez; límite de 7 y `hidden`; más de 7 elegidas; sin nada, listas vacías.
     - `PublishedMonthTest`: fecha normal; solo la fecha (sin hora); mes 0 y 13; texto vacío y
       texto que no es una fecha; la extensión sobre `CourseSummary`.
     - ✅ Pasan con `./gradlew :sharedLogic:testAndroidHostTest`, junto con los que ya había.

### Parte B · Pantalla (`app`)

5. **Componentes** (decisiones aprobadas el 2026-11-21). Partida en dos (decisión E): 5a y 5b son
   independientes y se revisan por separado. Diseño: `docs/tareas/resources/search-courses.png`.
   Rutas: `app/src/main/java/es/ulpgc/pamn/dominio/` (abreviado `app/.../`).

   **5a · La tarjeta (`CourseCard`).** ✅ **Hecha (2026-11-21).**
   - Ficheros (no se toca nada más):
     - `ui/components/TagLabel.kt`: etiqueta gris de la tarjeta (`surfaceVariant` y
       `onSurfaceVariant`); no se puede pulsar.
     - `ui/components/CourseTexts.kt`: los textos de la tarjeta, como funciones puras (como
       `SubmissionTexts.kt`).
     - `ui/components/CourseCard.kt`: la tarjeta.
     - `data/DemoSearchResults.kt`: `demoSearchResults`, los 3 cursos del Figma como `CourseSummary`
       («Estructuras de datos», Ana Ruiz, grafos/árboles/algoritmos, 20 módulos, oct 2026;
       «Teoría de grafos para principiantes», Jorge Medina, grafos/matemáticas, 14, sep 2026;
       «Búsqueda en grafos: BFS y DFS», Lucía Santana, grafos/algoritmos, 11, mar 2026). Solo para
       previews y tests; la usarán también las tareas 6–8.
     - `app/src/test/java/es/ulpgc/pamn/dominio/ui/components/CourseTextsTest.kt` (JUnit, como
       `ExampleUnitTest`; decisión D).
     - `app/src/androidTest/java/es/ulpgc/pamn/dominio/ui/components/CourseCardTest.kt` (como
       `GradusNavigationBarTest`, en el emulador).
   - Textos (`CourseTexts.kt`):
     - `moduleCountText(n)`: «1 módulo», «20 módulos».
     - `PublishedMonth.shortLabel()`: «oct 2026». Meses: ene, feb, mar, abr, may, jun, jul, ago,
       sep, oct, nov, dic (el Figma dice «sep»).
     - `PublishedMonth.spokenLabel()`: «octubre de 2026» (para TalkBack).
     - `CourseSummary.metaText()`: «20 módulos · oct 2026», o solo «20 módulos» si
       `publishedMonth` es null.
   - La tarjeta: `CourseCard(course: CourseSummary, onClick: () -> Unit, modifier: Modifier = Modifier)`.
     - Toda ella se pulsa: `Surface` con borde `outlineVariant`, esquinas redondeadas y `surface`.
     - De arriba abajo: título (`titleLarge`), «por Ana Ruiz» (`bodySmall`, `onSurfaceVariant`),
       etiquetas y la línea «20 módulos · oct 2026».
     - **Como mucho 3 etiquetas** (decisión A); si hay más, una más con «+N» («+2»). El backend
       admite hasta 10 y las tarjetas tendrían alturas muy distintas.
     - Sin ★, sin «Inscrito» (decisión 2) y sin la descripción (el Figma no la enseña).
     - **TalkBack (decisión B):** la tarjeta es una sola cosa, con la acción «Abrir curso», que se
       lee «Estructuras de datos, por Ana Ruiz, grafos, árboles, algoritmos, 20 módulos, publicado
       en octubre de 2026» (el mes completo: «oct 2026» se lee mal en voz alta).
     - Previews en claro y oscuro (`@LightDarkPreviews`): los 3 cursos del Figma, uno con más de
       3 etiquetas y uno con una fecha que no se entiende.
   - Criterio (tests, como mínimo; ver «Los tests también documentan» en `AGENTS.md` §7):
     - `CourseTextsTest`: singular y plural de módulos; los 12 meses cortos (o al menos enero,
       septiembre y diciembre) y el largo; `metaText` con y sin fecha.
     - `CourseCardTest` (emulador): la tarjeta es un solo nodo que se puede pulsar y llama a
       `onClick`; su texto leído incluye «publicado en octubre de 2026»; con 5 etiquetas se ven 3 y
       «+2»; mide al menos 48 dp de alto.
     - `./gradlew :app:testDebugUnitTest` y `./gradlew :app:connectedDebugAndroidTest` pasan; las
       previews se ven como la captura (sin ★ ni «Inscrito») en claro y oscuro.

   **5b · La caja de búsqueda (`SearchField`).** ✅ **Hecha (2026-11-21).**
   - Ficheros: `ui/components/SearchField.kt` y
     `app/src/androidTest/java/es/ulpgc/pamn/dominio/ui/components/SearchFieldTest.kt`.
   - `SearchField(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier)`.
     Controlada como `FormField`: no guarda el texto.
     - `OutlinedTextField` en forma de píldora, `surface`, una línea, lupa a la izquierda
       (decorativa: `contentDescription = null`).
     - Texto de ayuda: «Busca por título o descripción» (decisión C).
     - La ✕ solo aparece con texto; avisa con `onValueChange("")` y TalkBack la lee «Borrar
       búsqueda». **Zona táctil de 48 dp**: en esta versión de Material 3, `IconButton` mide 40 dp
       (ya pasó con la ✕ de la práctica, `cursos-backend-app.md`).
     - Tecla del teclado «Buscar» (`ImeAction.Search`), que llama a `onSearch`.
     - Previews en claro y oscuro: vacía y con «grafos».
   - Criterio (tests, como mínimo):
     - `SearchFieldTest` (emulador): sin texto no hay ✕; con texto, la ✕ mide al menos 48 dp y al
       pulsarla llama a `onValueChange("")`; la tecla «Buscar» llama a `onSearch`.
     - `./gradlew :app:connectedDebugAndroidTest` pasa; las previews se ven como la captura.
6. ✅ **Hecha (2026-11-21).** **`SearchViewModel` + `ScreenSearch` con resultados**, sin filtros todavía (decisiones aprobadas
   el 2026-11-21). Junta las piezas: `CourseSearchState` (tarea 3), `SearchField` y `CourseCard`
   (tarea 5), las corrutinas (espera de 300 ms y cancelar) y la navegación al curso. Partida en
   cuatro pasos (6a–6d), en este orden: primero la pantalla sin estado, que se ve y se prueba con
   datos fijos, y después el ViewModel. Rutas: `app/src/main/java/es/ulpgc/pamn/dominio/` (`app/.../`).
   Recordatorio: los tests de cada paso son los mínimos (`AGENTS.md` §7, «Los tests también documentan»).

   **6a · Textos (`SearchTexts`).** ✅ **Hecha (2026-11-21).**
   - Ficheros: `ui/screens/search/SearchTexts.kt` (funciones puras, como `CourseTexts.kt`) y
     `app/src/test/java/es/ulpgc/pamn/dominio/ui/screens/search/SearchTextsTest.kt` (JUnit).
   - Textos:
     - `resultsCountText(n)`: «1 resultado», «8 resultados», «0 resultados».
     - `SearchProblem.toText()` → título y texto: `UNREACHABLE` → «Sin conexión» / «No se ha podido
       buscar. Comprueba la conexión y vuelve a intentarlo.»; `SERVER_FAILURE` → «Algo ha fallado» /
       «El servidor no ha respondido bien. Vuelve a intentarlo en un momento.»
     - Lista vacía (decisión C de esta tarea): con texto o filtros → «No hay cursos que coincidan» /
       «Prueba con otras palabras.»; con `CourseQuery.EMPTY` → «Todavía no hay cursos publicados».
     - «Cargar más» fallido: «No se han podido cargar más cursos.»
   - Criterio: `SearchTextsTest` (singular, plural y 0; los dos problemas; los dos vacíos).
     `./gradlew :app:testDebugUnitTest` pasa.

   **6b · Pantalla sin estado (`ScreenSearch`).** ✅ **Hecha (2026-11-21).**
   - Ficheros:
     - `ui/screens/ScreenSearch.kt` (ya existe; deja de ser provisional):
       `ScreenSearch(text: String, search: CourseSearchState, actions: SearchActions, modifier: Modifier = Modifier)`.
       De arriba abajo: título «Buscar cursos» (`headlineLarge`), `SearchField` y los resultados.
     - `ui/screens/search/SearchActions.kt`: `onTextChange`, `onSearch`, `onLoadMore`, `onRetry` y
       `onCourseClick(id: String)` (como `LoginActions`).
     - `ui/screens/search/SearchResultsContent.kt`: la parte de los resultados según `search.status`.
     - `ui/components/ProblemMessage.kt` (decisión G): título, texto y, si se pasa `onRetry`,
       «Reintentar» (48 dp); se anuncia solo con `liveRegion`, como en `CourseStatusContent`. Esa
       pieza no se cambia ahora (no es de esta tarea), aunque podría reutilizarlo más adelante.
     - `ui/components/MainScaffold.kt`: **solo su preview**, que llama a `ScreenSearch(it)` y dejará
       de compilar; pasa a usar otra pestaña provisional (`ScreenSettings`).
     - `app/src/androidTest/java/es/ulpgc/pamn/dominio/ui/screens/ScreenSearchTest.kt` (emulador).
   - Qué se enseña:
     - `Loading` **sin resultados** (primera carga): un círculo de progreso en el centro, que TalkBack
       lee «Buscando cursos».
     - `Loading` **con resultados** (búsqueda nueva, decisión A): la lista y «N resultados» siguen
       visibles, con una **barra de progreso fina** (`LinearProgressIndicator`) encima. La lista no
       parpadea al teclear.
     - `Ready`: «N resultados» y una `LazyColumn` de `CourseCard` con `key = id` (por eso
       `CourseSearchState` no repite cursos). Al final, «Cargar más» si `canLoadMore`.
     - `LoadingMore`: al final de la lista, un círculo pequeño en lugar de «Cargar más».
     - `MoreFailed`: al final, «No se han podido cargar más cursos.» y «Reintentar».
     - `Failed`: `ProblemMessage` con «Reintentar».
     - `isEmpty`: `ProblemMessage` sin botón, con el texto que toque (6a).
     - **«N resultados» es `liveRegion` educado (decisión B)**: TalkBack lo anuncia cuando cambia.
   - Previews en claro y oscuro (con `demoSearchResults`): primera carga, resultados con «Cargar
     más», carga con resultados, cargando más, fallo al cargar más, error y vacío.
   - Criterio: `ScreenSearchTest` (emulador): cada estado enseña lo suyo; «Reintentar» y «Cargar
     más» llaman a su acción; tocar una tarjeta llama a `onCourseClick` con su `id`; «N resultados»
     es `liveRegion`; escribir en la caja llama a `onTextChange`. `./gradlew :app:connectedDebugAndroidTest`
     pasa y las previews se ven como la captura (sin la fila de filtros, que es de la tarea 7).

   **6c · `SearchViewModel`.** ✅ **Hecha (2026-11-21).**
   - Ficheros: `ui/screens/search/SearchViewModel.kt` y
     `app/src/test/java/es/ulpgc/pamn/dominio/ui/screens/search/SearchViewModelTest.kt`.
   - **Gradle (decisión D, aprobada):** en `app/build.gradle.kts`, una línea junto a
     `testImplementation(libs.junit)`: `testImplementation(libs.kotlinx.coroutines.test)`. La librería
     ya está en el catálogo (la usa `sharedLogic`). Nada más de Gradle.
   - Forma (como `CourseViewModel`: estado con `mutableStateOf`, puertos por constructor, *factory*):
     ```kotlin
     class SearchViewModel(private val gateway: CourseGateway) : ViewModel() {
         var text by mutableStateOf("")                            // lo que hay en la caja
             private set
         var search by mutableStateOf(CourseSearchState.start())  // consulta aplicada y resultados
             private set
         private var job: Job? = null

         init { load() }                    // al abrir Buscar: todos los cursos, sin esperar

         fun onTextChange(text: String)     // guarda el texto; tras 300 ms sin teclear, busca
         fun onSearch()                     // tecla «Buscar»: busca ya, sin esperar
         fun onLoadMore()                   // search.loadMore() y pide
         fun onRetry()                      // search.retry() y pide

         companion object { fun factory(gateway: CourseGateway) = viewModelFactory { … } }
     }
     ```
   - Reglas:
     - Pedir es siempre igual: `val request = search.pending ?: return`, llamar a
       `gateway.search(request.query, request.page)` y `search = search.receive(request, outcome)`.
     - `onTextChange`: guarda `text` al momento (la caja lo enseña); cancela la espera anterior y,
       **tras 300 ms sin teclear**, `search.search(search.query.withText(text))` y pide.
     - `onSearch`: cancela la espera y busca **ya** con el `text` actual.
     - **Dos `Job` (decisión E, cambiada el 2026-11-21; antes era uno solo):** `typing` (la espera)
       y `request` (la petición en camino). Una petición nueva cancela la anterior, también un
       «Cargar más» en curso cuando la búsqueda tecleada empieza; una pulsación nueva reinicia la
       espera; `onSearch` cancela la espera. Con un solo `job`, pulsar «Cargar más» o «Reintentar»
       antes de 300 ms cancelaba la espera y el texto de la caja no se buscaba nunca. La máquina ya
       ignora las respuestas tardías; cancelar ahorra peticiones.
     - La espera es `delay(300)` dentro del `launch`, como `CourseViewModel` cancela su carga (sin
       APIs en preview como `debounce`).
     - El ViewModel **no navega**: tocar una tarjeta llega a `MainNavGraph` (6d).
   - Criterio: `SearchViewModelTest` con tiempo simulado (`runTest`, `Dispatchers.setMain` con un
     `StandardTestDispatcher`, `advanceTimeBy`) y `FakeCourseGateway` (`searches` dice qué se pidió):
     - al crearse pide la primera página de `CourseQuery.EMPTY` sin esperar;
     - teclear guarda `text` al momento; a los 299 ms no ha pedido nada y a los 300 sí;
     - teclear dos veces seguidas: solo se pide la última (gana la última);
     - `onSearch` pide al momento y la espera pendiente ya no pide nada después;
     - `onLoadMore` pide la página siguiente y no hace nada si no hay más;
     - `onRetry` tras un fallo vuelve a pedir;
     - teclear durante «Cargar más»: la página siguiente no se añade a la búsqueda nueva.
     - `./gradlew :app:testDebugUnitTest` pasa.

   **6d · Conectarlo (`MainNavGraph`).** ✅ **Hecha (2026-11-21).**
   - Fichero: `ui/navigation/MainNavGraph.kt`, en `tab<Route.Search>`.
     - `viewModel(factory = SearchViewModel.factory(AppContainer.courseGateway))`.
     - `ScreenSearch(text, search, SearchActions(…), modifier)` con las funciones del ViewModel.
     - `onCourseClick = { id -> navController.navigate(Route.Course(id)) }`, **solo si la pantalla
       está activa** (estado `RESUMED` de la entrada; decisión F): dos toques rápidos no abren el
       curso dos veces.
   - Criterio (emulador, con `gradus.sh up`), comprobado a mano:
     - Buscar lista los cursos del servidor; escribir filtra tras dejar de teclear.
     - Abrir un curso y volver deja la búsqueda igual; cambiar de pestaña y volver, también.
     - Con el servidor apagado sale «Sin conexión»; al encenderlo, «Reintentar» funciona.
     - Doble toque rápido en una tarjeta abre el curso una sola vez.
     - TalkBack anuncia «N resultados» al terminar una búsqueda.

   - Fuera de la tarea 6: los filtros (tarea 7), quitar el botón provisional de Inicio (tarea 9) y
     recordar la búsqueda si Android cierra la app en segundo plano (`SavedStateHandle`; los demás
     ViewModel tampoco lo hacen).
   - Alternativa descartada a la decisión D: llevar la espera y la cancelación a `sharedLogic` (iOS
     las reutilizaría), porque rompería el patrón actual de ViewModel en `app`.
7. ✅ **Hecha (2026-11-22).** **Filtros: fila de chips + hoja con Etiquetas** (decisiones A–I aprobadas el 2026-11-21).
   Partida en cuatro pasos (7a–7d), en el mismo orden que la 6: lo que no depende de nada primero
   y el ViewModel al final. Rutas: `app/src/main/java/es/ulpgc/pamn/dominio/` (`app/.../`).
   Recordatorio: los tests de cada paso son los mínimos (`AGENTS.md` §7). Diseño: pantalla
   «4 · Buscar con filtros» de la captura (`docs/tareas/resources/search-courses.png`); solo la
   sección Etiquetas (Creador y Fecha son de la fase 2; «Ordenar por» no existe, decisión 10).
   - **Dónde va la lógica (aprobado):** todo lo de la hoja que no es dibujo va en `sharedLogic`
     (`FilterSheetState`, puro e inmutable), para que iOS solo rehaga las vistas. El ViewModel
     guarda `filters: FilterSheetState?` (`null` = cerrada): si se gira el móvil con la hoja
     abierta, no se pierde. Cómo se ve en horizontal queda para más adelante.

   **7a · `FilterSheetState` (`sharedLogic`, test primero).** ✅ **Hecha (2026-11-21).**
   - Ficheros: `sharedLogic/src/commonMain/kotlin/es/ulpgc/pamn/dominio/course/search/FilterSheetState.kt`
     y `sharedLogic/src/commonTest/kotlin/es/ulpgc/pamn/dominio/course/search/FilterSheetStateTest.kt`.
   - Forma:
     ```kotlin
     data class FilterSheetState(
         val draft: CourseQuery,             // el borrador (empieza igual que la consulta aplicada)
         val suggestions: TagSuggestions,    // calculadas al abrir (tarea 4, decisión B)
         val showingAll: Boolean = false,    // se ha pulsado «Ver todas»
     ) {
         val visibleTags: List<String>       // suggestions.shown, o suggestions.all tras «Ver todas»
         val canShowAll: Boolean             // !showingAll && suggestions.hasHidden
         val canReset: Boolean               // el borrador tiene etiquetas
         fun isSelected(tag: String): Boolean
         fun toggle(tag: String): FilterSheetState
         fun reset(): FilterSheetState
         fun showAll(): FilterSheetState
         fun queryToApply(typedText: String, applied: CourseQuery): CourseQuery?
         companion object {
             fun open(applied: CourseQuery, results: List<CourseSummary>): FilterSheetState  // factory method
         }
     }
     ```
   - Reglas:
     - `open`: `draft = applied`; `suggestions = TagSuggestions.from(results, applied.tags)`.
     - `isSelected` compara como `CourseQuery` (`tagKey`: sin mayúsculas ni espacios de sobra).
     - `toggle` usa `draft.toggleTag`; **las sugerencias no cambian** al marcar (los chips no se
       mueven bajo el dedo, tarea 4 decisión B).
     - `reset` usa `draft.withoutFilters()` (conserva el texto); no toca `showingAll`.
     - `showAll`: `showingAll = true`; si ya estaba, el mismo estado.
     - `queryToApply(typedText, applied)` (decisión A): `draft.withText(typedText)`; `null` si pide
       lo mismo que `applied` (no hay nada que buscar): mismo texto y mismas etiquetas **sin importar
       el orden ni las mayúsculas** (cambiado el 2026-11-21: quitar una etiqueta y volver a ponerla
       deja el borrador en otro orden y repetiría una búsqueda idéntica). El texto se compara
       **recortado** (`trim()`, como el adaptador HTTP; 2026-11-22): «red » y «red» piden lo mismo.
   - Criterio (tests, como mínimo): abrir (borrador = aplicado; sugerencias con las elegidas
     primero); marcar y desmarcar (también con otras mayúsculas); las sugerencias no cambian al
     marcar; restablecer conserva el texto y `canReset` pasa a falso; «Ver todas» (`visibleTags`
     y `canShowAll`); `queryToApply` con cambios, sin cambios (`null`) y con el texto tecleado
     distinto del aplicado. `./gradlew :sharedLogic:testAndroidHostTest` pasa.

   **7b · Los chips (`FilterChips`) y sus textos.** ✅ **Hecha (2026-11-22).** Partida en dos:
   **7b-1** `FilterTexts` y su test (✅) y **7b-2** `FilterChips` y su test de emulador (✅).
   - Ficheros:
     - `ui/components/FilterChips.kt`: `FiltersButton(count, onClick)`, `RemovableFilterChip(label,
       removeDescription, onRemove)` y `SelectableTagChip(tag, selected, onToggle)`, con previews en
       claro y oscuro.
     - `ui/screens/search/FilterTexts.kt` (funciones puras, como `SearchTexts.kt`).
     - `app/src/test/java/es/ulpgc/pamn/dominio/ui/screens/search/FilterTextsTest.kt` (JUnit) y
       `app/src/androidTest/java/es/ulpgc/pamn/dominio/ui/components/FilterChipsTest.kt` (emulador).
   - Textos (`FilterTexts.kt`): «Filtros»; TalkBack del botón: «Filtros, 1 activo», «Filtros, 2
     activos», «Filtros» con 0; etiqueta del chip quitable de una etiqueta: «#grafos»; su lectura:
     «Quitar filtro grafos»; sin sugerencias: «No hay etiquetas que elegir» (decisión G).
   - Reglas:
     - `FiltersButton` (decisión D): icono `ic_tune` (decorativo), «Filtros» y un círculo con el
       número (`primary` / `onPrimary`), que **no aparece con 0**. TalkBack lo lee como un botón con
       el texto de arriba («Filtros, 2 activos»), no el número suelto.
     - `RemovableFilterChip` (decisión C): «#grafos» y ✕; **todo el chip** quita el filtro y
       TalkBack lo lee «Quitar filtro grafos». Fondo `primaryContainer`, como en el Figma.
     - `SelectableTagChip` (decisión E): `FilterChip` de Material. Marcado: relleno `primary`,
       texto `onPrimary` y ✓ (`ic_check`); sin marcar: borde `outline`. **Colores puestos a mano
       desde el tema** (el tema no define `secondary`, que es el que usa Material por defecto).
       TalkBack lo lee como casilla, marcada o no.
     - Los tres miden al menos 48 dp de zona táctil.
   - Criterio (tests, como mínimo): `FilterTextsTest` (singular, plural y 0; «#»; «Quitar
     filtro»; sin sugerencias). `FilterChipsTest` (emulador): el número solo con filtros y la
     lectura de TalkBack; el chip quitable avisa al tocarlo y se lee «Quitar filtro…»; el chip de
     etiqueta está marcado o no y avisa al tocarlo; 48 dp en los tres.

   **7c · La fila y la hoja en la pantalla.** ✅ **Hecha (2026-11-22)** en tres pasos (7c-1, 7c-2 y 7c-3).
   - Ficheros:
     - `ui/screens/search/FilterBar.kt`: fila bajo `SearchField`, desplazable en horizontal:
       `FiltersButton` con `search.query.activeFilters.size` y un `RemovableFilterChip` por cada
       `ActiveFilter` aplicado (`when` sobre `ActiveFilter.Tag`).
     - `ui/screens/search/FilterSheet.kt`: `ModalBottomSheet` con, de arriba abajo, el título
       «Filtros» (encabezado), «Restablecer» (desactivado si `!canReset`, decisión F) y ✕
       «Cerrar filtros» (48 dp); la sección «Etiquetas» con «· elige una o varias» y los
       `SelectableTagChip` de `visibleTags` en un `FlowRow`, más «Ver todas» si `canShowAll` (al
       pulsarlo desaparece; no hay «Ver menos»), o «No hay etiquetas que elegir» si no hay
       ninguna; abajo, el botón «Mostrar cursos» (sin número: llega en la tarea 8, decisión H).
     - `ui/screens/ScreenSearch.kt`: parámetro nuevo `filters: FilterSheetState?`; `FilterBar`
       entre la caja y los resultados; la hoja solo si `filters != null`.
     - `ui/screens/search/SearchActions.kt`: `onOpenFilters`, `onToggleTag(tag)`, `onResetFilters`,
       `onShowAllTags`, `onApplyFilters`, `onCloseFilters` y `onRemoveFilter(ActiveFilter)`.
     - `ui/screens/search/ScreenSearchPreviews.kt`: previews con filtros aplicados y con la hoja
       abierta (con y sin «Ver todas», y sin etiquetas).
     - `ui/navigation/MainNavGraph.kt`: **solo** pasar `filters = null` para que compile (lo
       conecta 7d; en 6b faltaba prever esto).
   - Reglas de la hoja (decisión I):
     - Se abre **entera** (`rememberModalBottomSheetState(skipPartiallyExpanded = true)`).
     - Deslizar hacia abajo, tocar fuera o «atrás» → `onCloseFilters` (descarta el borrador).
     - «Mostrar cursos» y ✕ **cierran con animación**: primero `sheetState.hide()` y, al acabar,
       la acción (`onApplyFilters` / `onCloseFilters`). Si no, la hoja desaparece de golpe.
   - Criterio (tests, como mínimo) en `ScreenSearchTest`: sin filtros, «Filtros» sin número y sin
     chips; con dos etiquetas aplicadas, «Filtros, 2 activos» y dos chips que avisan con su
     `ActiveFilter`; tocar «Filtros» avisa; con `filters` la hoja se ve con sus etiquetas marcadas
     o no; tocar una etiqueta avisa con ella; «Ver todas» aparece solo si hay ocultas y avisa;
     «Restablecer» desactivado sin etiquetas; «No hay etiquetas que elegir»; «Mostrar cursos» y
     ✕ avisan. `./gradlew :app:connectedDebugAndroidTest` pasa.

   **7d · ViewModel y conexión.** ✅ **Hecha (2026-11-22)** en dos pasos (7d-1 y 7d-2).
   - Ficheros: `ui/screens/search/SearchViewModel.kt` (+ `SearchViewModelTest.kt`) y
     `ui/navigation/MainNavGraph.kt`.
   - `SearchViewModel`:
     - `var filters: FilterSheetState?` (`null` = cerrada).
     - `onOpenFilters`: `FilterSheetState.open(search.query, search.results)`.
     - `onToggleTag`, `onResetFilters`, `onShowAllTags`: cambian `filters` (si es `null`, nada).
     - `onApplyFilters` (decisión A): cierra la hoja, cancela la espera al teclear y, si
       `queryToApply(text, search.query)` no es `null`, busca con ella **una sola vez**.
     - `onCloseFilters`: cierra la hoja sin buscar.
     - `onRemoveFilter(filter)` (decisión B): busca **al momento** con
       `search.query.withoutTag(…).withText(text)` y cancela la espera al teclear.
   - `MainNavGraph`: pasa `filters` y las acciones del ViewModel **con una sola llamada**
     (aprobado el 2026-11-22): `fun SearchViewModel.actions(onCourseClick: (String) -> Unit): SearchActions`,
     extensión al final de `SearchViewModel.kt` (fuera de la clase: el ViewModel no depende de
     `SearchActions`, y `SearchActions` sigue sin depender de nada para previews y tests). La
     navegación (`onCourseClick` con `onlyWhenResumed`) la sigue decidiendo `MainNavGraph`. Las
     pantallas del curso y de la práctica no se tocan (no es de esta tarea). Con su test en
     `SearchViewModelTest`: cada acción llega a su función del ViewModel y `onCourseClick` pasa tal cual.
   - Criterio:
     - `SearchViewModelTest` (como mínimo): abrir calcula las sugerencias con los resultados
       cargados; marcar no busca nada; aplicar busca una sola vez con el texto de la caja y
       cancela la espera pendiente; aplicar sin cambios cierra sin buscar; cerrar descarta el
       borrador; quitar un chip busca al momento con el resto de etiquetas y el texto de la caja.
     - En el emulador, con `gradus.sh up`, comprobado a mano: elegir dos etiquetas reduce los
       resultados y la fila muestra «Filtros 2» y sus chips; quitar un chip los amplía;
       «Restablecer», ✕, deslizar y «atrás» funcionan como dice la decisión 7 del plan; girar el
       móvil con la hoja abierta no la pierde; TalkBack lee «Filtros, 2 activos» y «Quitar filtro…».

   - Fuera de la tarea 7: el número de «Mostrar N cursos» (tarea 8), creador y fecha (fase 2) y
     cómo se ve en horizontal.
8. **«Mostrar N cursos»** (decisión 6; decisiones A–G aprobadas el 2026-11-22). Mientras la hoja
   está abierta, el botón dice cuántos cursos daría el borrador; al pulsarlo se usan esos
   resultados sin volver a pedirlos. El recuento es **otro `CourseSearchState`** (lo previsto en la
   tarea 3), así que la paginación, los fallos y las respuestas tardías ya están resueltos.
   Partida en tres pasos (8a–8c). Recordatorio: los tests son los mínimos (`AGENTS.md` §7).
   - Cómo funciona:

     | Momento | Botón | Petición |
     |---|---|---|
     | Al abrir, borrador igual a lo aplicado | «Mostrar 8 cursos» (el `total` ya conocido) | Ninguna |
     | Se marca o se quita una etiqueta | «Mostrar cursos» mientras espera y cuenta | Tras 300 ms sin cambios, la página 0 con el borrador |
     | Llega la respuesta | «Mostrar N cursos» | — |
     | El borrador vuelve a pedir lo mismo que lo aplicado | El `total` aplicado | Se cancela la que hubiera |
     | Falla el recuento | «Mostrar cursos» | Al pulsar, búsqueda normal |
     | Pulsar con el recuento hecho | — | **Ninguna**: se usan esos resultados |
     | Pulsar antes de que acabe | — | Búsqueda normal (se cancela el recuento) |
     | Cerrar la hoja | — | Se cancela el recuento |

     Se pide la página 0 entera (20) y no solo el número: así, al pulsar, los resultados ya están.
   - Decisiones: el recuento vive **dentro de `FilterSheetState`** (al cerrar la hoja se descarta
     con ella; lógica compartida con iOS) (A); mientras cuenta, **«Mostrar cursos» sin número**
     (B); con **0 resultados, «Ningún curso coincide» y el botón desactivado**; «Restablecer» y ✕
     siguen funcionando (C); al pulsar, **se reutiliza el recuento si terminó y pide lo mismo**; si
     no, búsqueda normal (D); **un solo trabajo de recuento**: cada cambio cancela la espera y la
     petición en curso, y cerrar también (E); `asksTheSameAs` pasa a `CourseQuery.kt` como
     **`internal`** (F); **TalkBack no anuncia el número** al cambiar; se lee al llegar al botón (G).

   **8a · Recuento en `FilterSheetState` (`sharedLogic`, test primero).**
   - Ficheros: `course/search/CourseQuery.kt` (+ `CourseQueryTest.kt`) y
     `course/search/FilterSheetState.kt` (+ `FilterSheetStateTest.kt`).
   - `CourseQuery.kt`: `internal fun CourseQuery.asksTheSameAs(other: CourseQuery): Boolean`,
     movida tal cual desde `FilterSheetState.kt` (texto recortado; mismas etiquetas con `tagKey`, en
     cualquier orden). Sus tests pasan a `CourseQueryTest` (espacios, orden, mayúsculas, etiquetas
     distintas con el mismo número); los de `queryToApply` siguen en `FilterSheetStateTest`.
   - `FilterSheetState`:
     ```kotlin
     data class FilterSheetState(
         …,                                     // lo que ya tenía
         val preview: CourseSearchState? = null, // el recuento del borrador; null = sin pedir
     ) {
         fun previewQuery(typedText: String): CourseQuery          // draft.withText(typedText)
         fun previewNeeded(typedText: String, applied: CourseQuery): Boolean
         fun startPreview(typedText: String): FilterSheetState      // preview = CourseSearchState.start(previewQuery)
         fun receivePreview(request: SearchRequest, outcome: SearchOutcome): FilterSheetState
         fun count(typedText: String, applied: CourseSearchState): DraftCount
         fun previewToApply(typedText: String): CourseSearchState?
     }

     sealed interface DraftCount {
         data class Known(val total: Int) : DraftCount
         data object Counting : DraftCount   // esperando los 300 ms o la respuesta
         data object Unknown : DraftCount    // falló el recuento
     }
     ```
   - Reglas:
     - `previewNeeded`: `true` si `previewQuery` no pide lo mismo que `applied` **ni** que
       `preview?.query` (no se cuenta dos veces lo mismo).
     - `receivePreview`: `preview.receive(…)`; sin `preview`, el mismo estado. Una respuesta de un
       recuento ya superado se ignora (lo hace `CourseSearchState`).
     - `count`: si `previewQuery` pide lo mismo que `applied.query` → `Known(applied.total)` si
       `applied` está `Ready`, si no `Counting`. Si pide lo mismo que `preview.query` → `Ready` →
       `Known(preview.total)`; `Loading` → `Counting`; `Failed` → `Unknown`. En otro caso, `Counting`
       (aún no se ha empezado a contar).
     - **Decisión tomada al implementar el 8a (2026-11-22), cambia lo escrito arriba para lo aplicado:**
       con lo aplicado, `Ready`, `LoadingMore` y `MoreFailed` dan `Known(total)` (con «Cargar más»
       en curso o fallido el `total` sigue siendo válido), `Loading` da `Counting` y `Failed` da
       `Unknown` (no hay total y quedarse en `Counting` engañaría). Una sola función privada,
       `CourseSearchState.draftCount()`, sirve para lo aplicado y para el recuento.
     - **Nota para el revisor:** esta decisión **no estaba en el plan aprobado** (A–G): la propuso
       Claude y la aprobó el usuario «con reservas». Al revisar el 8a, decidir si fue la correcta.
       A favor: el número mostrado es siempre verdadero y un fallo de la lista no se disfraza de
       «contando». En contra: se aparta de la letra del plan, y con `Failed` el botón dice lo mismo
       que con `Counting` («Mostrar cursos»), así que para el usuario no cambia nada hoy; solo se
       nota en 8c si el ViewModel distingue `Unknown` de `Counting`. Si no convence, volver a
       «solo `Ready` da `Known`, el resto `Counting`»: es cambiar `draftCount()` y 3 tests
       (`count keeps the total … loading more`, `… after loading more failed`, `… what is applied failed`).
     - **Revisado (2026-11-22): se mantiene.** «Cargar más» no cambia el `total` de la consulta, así
       que con la regla del plan, abrir la hoja durante un «Cargar más» daría «Mostrar cursos» sin
       motivo. Y `Unknown` evita un «contando» que nunca termina si algún día el botón enseña un
       indicador mientras cuenta. Además, lo aplicado y el recuento siguen la misma regla con una sola
       función. Para 8c: con lo aplicado fallido y el borrador sin cambios no hay nada que contar
       (`previewNeeded` es `false`) y «Mostrar cursos» solo cierra la hoja; la lista sigue enseñando
       su aviso con «Reintentar».
     - `previewToApply`: `preview` si está `Ready` y pide lo mismo que `previewQuery`; si no, `null`.
     - `toggle` y `reset` **no** tocan `preview` (el ViewModel decide cuándo contar).
   - Criterio (tests, como mínimo): `CourseQueryTest` con lo que ya cubría `asksTheSameAs`.
     `FilterSheetStateTest`: `previewNeeded` (igual a lo aplicado, igual a lo ya contado, distinto);
     `count` en cada caso (aplicado `Ready` y cargando, recuento `Ready`, cargando, fallido, sin
     empezar); respuesta tardía de un recuento anterior; `previewToApply` sí (terminado y mismo
     borrador) y no (cargando, fallido, borrador cambiado). `./gradlew :sharedLogic:testAndroidHostTest`.

   **8b · Texto y botón en la hoja.**
   - Ficheros: `ui/screens/search/FilterTexts.kt` (+ `FilterTextsTest`), `FilterSheetContent.kt`,
     `FilterSheet.kt`, `ScreenSearch.kt` (si hace falta pasar el número), `ScreenSearchPreviews.kt`
     y `FilterSheetTest`.
   - `showCoursesText(count: DraftCount)`: `Known(0)` → «Ningún curso coincide»; `Known(1)` →
     «Mostrar 1 curso»; `Known(n)` → «Mostrar n cursos»; `Counting` y `Unknown` → «Mostrar cursos».
   - El botón de la hoja recibe el `DraftCount` (calculado fuera con `count(text, search)`, para que
     la hoja siga sin estado) y está **desactivado con `Known(0)`** (C). Sin `liveRegion` (G).
   - Previews: con número, contando y con 0.
   - Criterio (tests, como mínimo): `FilterTextsTest` (1, N, 0 y sin número en los dos casos);
     `FilterSheetTest`: cada texto del botón y desactivado con 0 (pulsarlo no avisa).

   **8c · ViewModel: recuento, cancelaciones y aplicar reutilizando.** ✅ **Hecha (2026-11-22).**
   Partida en dos pasos (aprobado el 2026-11-22): **8c-1** `FilterSheetState.cancelPreview()` (✅) y
   **8c-2** el ViewModel (✅).
   - **Hueco del plan, visto al proponer el 8c (2026-11-22).** La decisión E (cada cambio cancela la
     petición en curso) choca con `previewNeeded` del 8a (no se vuelve a contar un borrador que «ya
     se está contando»). Ejemplo: marcar «grafos» → a los 300 ms sale el recuento → antes de la
     respuesta, marcar «árboles» (se cancela la petición, pero `preview` sigue en `Loading(grafos)`)
     → desmarcar «árboles»: el borrador vuelve a «grafos», `previewNeeded` da `false` y nadie pide
     nada. El botón se queda en «Mostrar cursos» para siempre (aplicar sí funciona: búsqueda normal).
   - **Decisión (8c-1):** `fun cancelPreview(): FilterSheetState` en `FilterSheetState`: un recuento
     **a medias** (`preview.pending != null`) se olvida (`preview = null`); uno terminado o fallido se
     conserva (mismo estado). El ViewModel la llama **cada vez que cancela** el trabajo `preview`.
   - **Nota para el revisor:** la propuso Claude y la aprobó el usuario «en principio»; al revisar el
     8c, decidir si fue la correcta. A favor: la regla «un recuento cancelado se olvida» queda en
     `sharedLogic`, probada y igual para iOS; el `previewNeeded` del 8a no cambia. En contra: toca el
     8a (un método más y 6 tests), y al volver al borrador cancelado se vuelve a pedir lo mismo (tras
     otros 300 ms). Alternativas descartadas: (1) arreglarlo solo en el ViewModel con
     `filters?.copy(preview = null)` (dos ficheros, pero la regla sin test en `sharedLogic` y que iOS
     tendría que repetir); (2) no cancelar la petición si el nuevo borrador pide lo mismo que la que
     va en camino (ahorra una petición, pero se aparta de E y el ViewModel necesitaría
     `asksTheSameAs`, que es `internal` de `sharedLogic`); (3) que `previewNeeded` no cuente como
     «ya contado» un recuento en curso (cambia una regla probada del 8a y solo es correcto si quien la
     usa cancela siempre antes de preguntar).
   - **Revisado (2026-11-22): se mantiene.** El hueco era real y era del plan: con la regla del 8a
     y la decisión E juntas, el botón podía quedarse sin número para siempre. `cancelPreview` lo
     arregla donde está la regla, en `sharedLogic` (probada e igual para iOS), sin cambiar
     `previewNeeded`. El coste (volver a pedir un recuento cancelado tras otros 300 ms) es pequeño
     y solo ocurre si se cambia el borrador antes de que llegue la respuesta. Las alternativas
     dejaban la regla sin probar en `sharedLogic` (1), se apartaban de E (2) o hacían que
     `previewNeeded` dependiera de que quien la usa cancele antes (3).
   - Ficheros: 8c-1, `FilterSheetState.kt` (+ `FilterSheetStateTest.kt`). 8c-2, `SearchViewModel.kt` y
     **`SearchViewModelPreviewTest.kt`**, fichero nuevo para los tests del recuento (aprobado el
     2026-11-22: `SearchViewModelFiltersTest` pasaría de 600 líneas); en `SearchViewModelFiltersTest`
     solo se adapta `toggling a tag marks it in the draft without searching`, que con
     `advanceUntilIdle` vería ahora el recuento. `MainNavGraph.kt` no cambia (`ScreenSearch` ya
     calcula `count(text, search)`).
   - Un trabajo `preview` (como `typing`): `onToggleTag` y `onResetFilters` lo cancelan y, si
     `previewNeeded(text, search.query)`, tras **300 ms** `startPreview(text)` y piden su `pending`;
     la respuesta va a `receivePreview` (si la hoja sigue abierta). Si el borrador vuelve a pedir lo
     aplicado, solo se cancela (E).
   - `onApplyFilters` (D): cancela `preview`; si `previewToApply(text)` no es `null`, ese estado pasa
     a ser `search` **sin pedir nada**; si no, lo de siempre (`queryToApply`).
   - `onCloseFilters` cancela `preview`.
   - Criterio:
     - `SearchViewModelFiltersTest` (como mínimo): al abrir no se pide nada y el número es el
       aplicado; marcar no pide nada hasta los 300 ms y a los 300 sí; dos cambios seguidos: solo se
       cuenta el último; volver al borrador aplicado cancela el recuento en curso; un recuento
       fallido da `Unknown`; aplicar con el recuento hecho no pide nada y deja sus resultados;
       aplicar con el recuento a medias busca normal y no repite; cerrar cancela el recuento.
     - En el emulador, con `gradus.sh up`, a mano: el número cambia al elegir etiquetas y
       coincide con «N resultados» al aplicar; con 0, «Ningún curso coincide» y no se puede pulsar.
   - Fuera de la tarea 8: el arreglo de rendimiento de `onCourseClick` (tarea 9).
9. **Repaso final.**
   - Quitar el botón provisional de Inicio (decisión 9).
   - TalkBack: «Quitar filtro grafos», «Filtros, 2 activos», etiquetas seleccionadas como tales.
   - Modo oscuro, `especificacion.md` (qué filtros hay hoy; quitar «relevancia» de los órdenes,
     decisión 10) y `cursos-backend-app.md` (tarea 6 hecha).
   - Rendimiento (visto en la revisión de 7d): en `MainNavGraph`, `onlyWhenResumed { … }` crea una
     función nueva en cada recomposición, así que `SearchActions` nunca es «igual» y la fila, la
     lista y las tarjetas se vuelven a dibujar con cada tecla. Arreglo: recordarla con
     `remember(lifecycleOwner, navController) { … }`. Comprobarlo con el contador de
     recomposiciones del *Layout Inspector* antes y después de teclear.

### Parte C · Fase 2 (después de la fase 1)

10. **Backend**: `author` y `published` en `GET /api/courses` (JUnit, `mvn test`) y
    `api-cursos.md` al día. Documento propio en `backend/docs/tareas/`.
    - Decidido (2026-11-21): el nombre del autor se compara **sin distinguir mayúsculas y
      minúsculas** (`u.name` no está normalizado como `search_text`).
11. **App**: `CourseQuery` con creador y fecha; secciones Creador y Fecha de publicación en la hoja.

## Progreso

- 2026-11-21: plan creado en `.ia/plans/search-course.md`.
- 2026-11-21: decisiones 1–9 aprobadas; el documento pasa a `docs/tareas/buscar-cursos.md`.
  Aclarado en la decisión 1: creador y fecha ya son datos del backend; solo falta filtrarlos.
- 2026-11-21: decisión 10 («Relevancia» es una errata: sin «Ordenar por» ni `sort`). Enlace a la
  captura del diseño. Reglas de la tarea 1 detalladas para poder empezarla en un chat nuevo.
- 2026-11-21: **tarea 1 hecha** (`CourseQuery`, `ActiveFilter` y `CourseQueryTest`, 17 tests en verde
  junto con los que ya había). Las etiquetas se guardan con los espacios ordenados (recortados y
  agrupados) y las mayúsculas de la primera forma elegida; una etiqueta en blanco se ignora. No se
  aplica NFC (no hay en `commonMain`; el servidor sí compara con NFC).
- 2026-11-21: tarea 2 discutida y detallada. Aprobado: se mantiene la extensión `search(text)`,
  una línea de `MainNavGraph.kt` pasa a `search(CourseQuery.EMPTY)` y `appendSearch` va dentro de
  `HttpCourseGateway.kt`.
- 2026-11-21: **tarea 2 hecha**. `CourseGateway.search(CourseQuery, page)` y el atajo
  `search(text, page)`; `HttpCourseGateway` manda `q` y un `tag` por etiqueta (`appendSearch`);
  `FakeCourseGateway` exige todas las etiquetas con `tagKey` (antes `comparisonKey`, ahora `internal`);
  `MainNavGraph` usa `CourseQuery.EMPTY`. 5 tests nuevos y 1 adaptado (`HttpCourseGatewayTest` 19,
  `FakeCourseGatewayTest` 8); todos pasan y la app compila.
  - Nota: `HttpCourseGatewayTest` importa `es.ulpgc.pamn.dominio.course.*` porque la función
    `search` y el subpaquete `course.search` se llaman igual. Solo afecta al código de fuera del
    paquete `course` que use el atajo de texto.
  - Repaso: `comparisonKey` → `tagKey`, el comentario de `appendSearch` ya no dice «puro» y fuera
    las importaciones que sobraban con el `*`. Tests en verde; compila para Android e iOS.

- 2026-11-21: tarea 3 discutida y detallada. Aprobado: respuestas tardías por comparación de la
  petición (A), sin resultados si falla la primera página (B), sin repetir cursos al cargar más
  (C) y los nombres (D). Las corrutinas van en el ViewModel (tarea 6), no en la máquina.

- 2026-11-21: **tarea 3 hecha** (`CourseSearchState`, `SearchRequest`, `SearchProblem`, `SearchStatus`
  y `CourseSearchStateTest`, 21 tests). Hecha en tres pasos: tests en rojo con un esqueleto en
  `TODO()`, código mínimo en verde y comentarios. Todos los tests de `sharedLogic` pasan; compila
  la app y `sharedLogic` para iOS. Detalles aprobados al proponerla:
  - Mientras carga una búsqueda nueva se conservan también `total` y `hasMore` («N resultados» no
    parpadea); `canLoadMore` es falso porque exige `Ready`.
  - En `MoreFailed`, `loadMore()` no hace nada: se vuelve a pedir con `retry()` (la pantalla
    enseñará «Reintentar» en lugar de «Cargar más»).
  - Si falla la primera página: sin resultados, `total = 0`, `hasMore = false` y `nextPage = 0`.
  - Repaso: un test más (22) para un **fallo** que llega tarde (`Unreachable` de una búsqueda
    anterior): se ignora y no borra los resultados. Pasa sin cambiar el código, porque la
    comprobación de la petición va antes de mirar el desenlace; queda documentado con un test.

- 2026-11-21: tarea 4 discutida y detallada. Aprobado: alfabético sin mayúsculas ni tildes (A),
  las sugerencias se calculan al abrir la hoja con las etiquetas aplicadas (B), `PublishedMonth`
  en `course/` (C) y el mes en UTC (D).

- 2026-11-21: **tarea 4 hecha** (`PublishedMonth` con 8 tests y `TagSuggestions` con 15). Hecha en
  dos pasos, cada uno con rojo (esqueleto en `TODO()`), verde y comentarios; toda la suite de
  `sharedLogic` pasa, compila el código común y la app. Detalles:
  - `PublishedMonth.parse` solo lee `AAAA-MM` y no mira lo que venga después (aprobado: `"2026-1012"`
    daría octubre de 2026; el servidor siempre manda ISO 8601). Los dígitos se comprueban con
    `in '0'..'9'`, no con `isDigit()`.
  - `TagSuggestions.from` reutiliza `tagKey()` para decidir si dos etiquetas son la misma. Si dos
    etiquetas solo se distinguen por la tilde («arbol» / «árbol»), se desempata por `tagKey()`
    para que el orden sea siempre el mismo.
  - Tests extra respecto a la lista del criterio: empate alfabético sin distinguir mayúsculas, la
    elegida se enseña tal como se eligió, etiquetas en blanco ignoradas y `shown` personalizado.

- 2026-11-21: tarea 5 discutida y detallada, partida en 5a (tarjeta) y 5b (caja). Aprobado: como
  mucho 3 etiquetas y «+N» (A), TalkBack lee la tarjeta entera con el mes completo (B), texto de
  ayuda «Busca por título o descripción» (C), textos con JUnit en `app/src/test` y accesibilidad
  en `androidTest` (D) y la partición (E). Nueva regla en `AGENTS.md` §7: «Los tests también
  documentan» (cada comportamiento descrito tiene su test).

- 2026-11-21: **tarea 5a hecha** (`CourseTexts`, `TagLabel`, `DemoSearchResults` y `CourseCard`).
  Hecha en cuatro pasos: tests de los textos en rojo (esqueleto con `TODO()`, 19 tests), código
  mínimo en verde, tarjeta con previews y `CourseCardTest` en el emulador (12 tests). Pasan
  `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest` y `:sharedLogic:testAndroidHostTest`.
  Detalles aprobados o decididos por el camino:
  - `CourseTexts.kt` tiene dos funciones puras más que el documento: `CourseSummary.spokenText()`
    (la frase de TalkBack) y `CourseSummary.tagsPreview(max = 3)` (etiquetas visibles y cuántas
    sobran, `TagsPreview`). Así se prueban en JVM y `CourseCardTest` solo comprueba lo que depende
    de Compose.
  - TalkBack lee **todas** las etiquetas, también las que la tarjeta esconde tras «+N».
  - El título de la tarjeta usa `titleLarge` («Texto/Sección», 18 en negrita): confirmado contra el
    Figma (no es «Texto/Cuerpo fuerte», 16).
  - Repaso: `PublishedMonth` **no se puede construir con un mes fuera de 1..12** (`init { require(...) }`,
    también con `copy`). Antes solo `parse` lo comprobaba y `PublishedMonth(2026, 13).shortLabel()`
    se habría salido de la lista de meses. 4 tests nuevos en `PublishedMonthTest` (mes 0, 13, negativo
    y `copy`) y uno para el primer y el último mes; `parse` no cambia.
  - `TagLabel` lleva un borde `outlineVariant`: en el tema oscuro `surfaceVariant` es igual que
    `surface` y sin borde la etiqueta no se veía dentro de la tarjeta. No se ha tocado el tema.
    **Aprobado dejarlo así (2026-11-21)**, aunque en claro el Figma no tiene borde: arreglarlo en el
    tema obligaría a definir otro `surfaceVariant` oscuro y afectaría a más piezas.
  - La tarjeta usa `clickable` + `clearAndSetSemantics` dentro de la `Surface`: es un solo nodo
    pulsable (rol botón, acción «Abrir curso») que TalkBack lee entero. Los textos sueltos solo se
    ven en el árbol sin fusionar (`useUnmergedTree = true` en los tests).

- 2026-11-21: **tarea 5b hecha** (`SearchField` y `SearchFieldTest`, 12 tests de emulador). Hecha en
  tres pasos: esqueleto con `TODO()` y tests en rojo (todos fallaban con `NotImplementedError`),
  código mínimo en verde y previews en claro y oscuro. Pasan `:app:testDebugUnitTest`,
  `:app:connectedDebugAndroidTest` y `:sharedLogic:testAndroidHostTest`. Detalles:
  - Aprobado: la tecla «Buscar» **cierra el teclado dentro de `SearchField`**
    (`LocalSoftwareKeyboardController.hide()`) y luego llama a `onSearch`. Se prueba en
    `androidTest` con un teclado falso (`CompositionLocalProvider`): los avisos llegan en el orden
    `hide`, `search` (comprobado invirtiendo el orden en el código: el test falla). Es el test 12.
  - Tests extra respecto al criterio: escribir avisa con el texto nuevo, la caja muestra el `value`
    que recibe y no lo guarda por sí sola (ni siquiera si el padre ignora el cambio), sigue al
    `value` cuando el padre lo cambia, el texto de ayuda se ve vacía y desaparece con texto, y la
    lupa no tiene `contentDescription`.
  - La ✕ usa `IconButton` con `size(48.dp)`, como `ExerciseParts.kt`; la caja mide al menos 56 dp
    de alto, como `FormField`.
  - Sin patrón nuevo: componente sin estado (*state hoisting*), con `modifier` y previews.

- 2026-11-21: tarea 6 discutida y detallada, partida en 6a (textos), 6b (pantalla sin estado), 6c
  (ViewModel) y 6d (conectarlo). Aprobado: durante una búsqueda nueva se conservan la lista y
  «N resultados» con una barra de progreso fina (A); «N resultados» se anuncia con `liveRegion`
  (B); dos textos de lista vacía (C); `kotlinx-coroutines-test` para los tests de `app` (D, toca
  `app/build.gradle.kts`); un solo `job` (E); navegar solo con la pantalla activa (F);
  `ProblemMessage` reutilizable (G).

- 2026-11-21: **tareas 6a y 6b hechas** (`SearchTexts` con 11 tests JUnit; `SearchActions`,
  `ProblemMessage`, `SearchResultsContent`, `ScreenSearch` y sus previews, con `ScreenSearchTest`,
  23 tests de emulador). Hechas con tests primero (esqueleto con `TODO()`: 10 de 11 y 23 de 23 en
  rojo) y luego el código mínimo. Pasan `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest`,
  `:sharedLogic:testAndroidHostTest` y `:app:assembleDebug`. Detalles:
  - Aprobado: `MainNavGraph.kt` (que el documento no mencionaba) llama a `ScreenSearch` con un
    estado provisional (`CourseSearchState.start()` y `SearchActions()`): solo enseña la carga hasta
    que 6d lo sustituya.
  - `SearchMessage(title, body = "")` es propio de Buscar (no se reutiliza `CourseProblemText`, que es
    del camino). `ProblemMessage` no pinta el texto si va vacío («Todavía no hay cursos publicados»).
  - `RetryButton` (48 dp) es público en `ProblemMessage.kt`: lo usan el aviso y el pie de la lista.
  - Fuera de la lista del documento: `demoSearchState(...)` en `DemoSearchResults.kt` (un
    `CourseSearchState` de ejemplo para previews y tests; lo usarán las tareas 7 y 8) y
    `ui/screens/search/ScreenSearchPreviews.kt` (las 7 previews, para no alargar `ScreenSearch.kt`).
  - Sin resultados o con fallo no se pinta «0 resultados»: el aviso (`liveRegion`) ya lo dice y se
    evita anunciar dos cosas. Con `MoreFailed` el pie también es `liveRegion`.
  - Los tests cazaron un fallo real: una clave propia (`object`) para el pie de la `LazyColumn`
    falla en Android (las claves deben poder guardarse en un `Bundle`). El pie va sin clave.
  - La barra de progreso tiene un hueco fijo de 4 dp para no empujar la lista al aparecer, con 8 dp
    de aire encima (antes quedaba pegada a la caja).
  - **Revisión (Opus):** una búsqueda nueva vuelve arriba. La posición de la lista se ata a la
    consulta: `rememberSaveable(search.query, saver = LazyListState.Saver)` en `ResultsList`. Con otra
    consulta empieza de cero; con la misma («Cargar más», o volver de un curso) se conserva. No se
    usa `LaunchedEffect(query) { scrollToItem(0) }` porque también se ejecutaría al volver de un
    curso y se perdería el sitio. 4 tests más en `ScreenSearchTest` (27): consulta nueva arriba,
    «Cargar más» mantiene la posición, la posición sobrevive a guardar y restaurar el estado
    (`StateRestorationTester`) y el hueco de la barra. Los dos de posición que ya pasaban antes del
    arreglo se dejan como red. Nota: el nodo de la barra mide 24 dp aunque la dibujada sea de 4 dp,
    por eso el test mide desde su centro.

- 2026-11-21: **tarea 6c hecha** (`SearchViewModel` y `SearchViewModelTest`, 25 tests JUnit con
  tiempo simulado). Primero el esqueleto con `TODO()` (24 de 25 en rojo; el que pasaba solo mira el
  estado inicial) y después el código mínimo. Pasan `:app:testDebugUnitTest` (56) y `:app:assembleDebug`.
  Detalles:
  - Aprobado al proponerla: **dos `Job`** en lugar de uno (decisión E actualizada). Dos tests lo
    documentan: «Cargar más» y «Reintentar» durante la espera no se comen la búsqueda tecleada.
  - Si la máquina devuelve el mismo estado (`===`), la acción no tocaba y no se cancela nada
    («Cargar más» sin más, o mientras carga; «Reintentar» sin fallo).
  - `TYPING_PAUSE = 300.milliseconds` (`Duration`, no `Long`: la versión con `Long` da aviso).
  - Solo para tests, `app/src/test/.../search/DelayedCourseGateway.kt` (*Decorator*): pasa la
    petición a `FakeCourseGateway` al momento y responde con retraso; `answered` dice qué búsquedas
    llegaron a responderse, y así se ve que una petición cancelada no termina.
  - Tests extra respecto al criterio: el estado inicial, los resultados y el fallo llegan a `search`,
    la *factory*, «Buscar» con el mismo texto vuelve a buscar, la espera no cancela la petición en
    camino, una búsqueda nueva cancela la anterior, «Cargar más» mientras carga la primera página o
    ya cargando más, «Reintentar» tras fallar «Cargar más» y sin fallo.

- 2026-11-21: **tarea 6d, código hecho** (`MainNavGraph.kt`: la pestaña Buscar usa `SearchViewModel`;
  fuera el estado provisional de 6b). Pasan `:app:testDebugUnitTest` (61) y `:app:assembleDebug`.
  Detalles:
  - `dropUnlessResumed` solo admite acciones sin parámetros y `onCourseClick` recibe el `id`: nuevo
    ayudante `LifecycleOwner.onlyWhenResumed`, `internal` al final de `MainNavGraph.kt`, que mira el
    estado **en el momento del toque**. Fichero nuevo, no previsto en el plan:
    `app/src/test/.../ui/navigation/OnlyWhenResumedTest.kt` (5 tests JUnit con un `LifecycleRegistry`
    falso; primero en rojo con `TODO()`): `RESUMED` sí, `STARTED` y `CREATED` no, se mira al llamar
    y no al crear, y el segundo toque se descarta cuando la pantalla deja de estar activa.
- 2026-11-21: **tarea 6d hecha y, con ella, la tarea 6.** Comprobada a mano en el emulador con el
  servidor (`gradus.sh up`): funciona como se esperaba.

- 2026-11-21: tarea 7 discutida y detallada, partida en 7a (`FilterSheetState`), 7b (chips y
  textos), 7c (fila y hoja en la pantalla) y 7d (ViewModel y conexión). Aprobado: la lógica de la
  hoja en `sharedLogic`; las sugerencias al abrir; aplicar con el texto de la caja y solo si cambia
  algo (A); quitar un chip busca al momento (B); el chip quitable entero es la zona táctil (C);
  «Filtros n» y su lectura (D); chips marcados en `primary` (E); «Restablecer» desactivado y
  «Ver todas» sin «Ver menos» (F); «No hay etiquetas que elegir» (G); «Mostrar cursos» sin
  número (H); la hoja se abre entera y se cierra con animación (I). Horizontal, más adelante.
- 2026-11-21: **tarea 7a hecha.** `FilterSheetState` y `FilterSheetStateTest` (31 tests, primero en
  rojo contra un esqueleto con `TODO()`). Además del criterio, tests de: «Ver todas» dos veces
  (mismo estado), `reset` sin tocar «Ver todas», `canReset` sin contar el texto, etiqueta elegida
  que no sale en los resultados, etiqueta en blanco, y `queryToApply` con el orden y las
  mayúsculas cambiados (`null`). `./gradlew :sharedLogic:testAndroidHostTest` pasa.
- 2026-11-22: **detalle 1 hecho** (el texto de `queryToApply` se compara con `trim()`): 3 tests en
  rojo y después en verde, más 2 que documentan lo que no cambia (espacios dentro del texto y
  texto tecleado tal cual cuando cambia otra cosa). `FilterSheetStateTest` pasa a 36 tests.
- 2026-11-22: **tarea 7b-1 hecha.** `FilterTexts.kt` (`FILTERS_LABEL`, `NO_TAGS_TEXT`,
  `filtersButtonDescription`, `tagChipLabel`, `removeFilterDescription`) y `FilterTextsTest`
  (10 tests JUnit; primero en rojo contra un esqueleto con `TODO()`). Además del criterio: una
  cuenta negativa se lee como 0, etiquetas con mayúsculas, tildes y espacios, y el «Quitar filtro»
  sin «#». `./gradlew :sharedLogic:testAndroidHostTest :app:testDebugUnitTest` pasa.
- Pendiente para la tarea 8 (detalle 2 del orquestador): mover `asksTheSameAs` (hoy privada en
  `FilterSheetState.kt`) a `CourseQuery.kt`, con sus tests en `CourseQueryTest`, para compararlo
  con el borrador y decidir si hay que pedir el número de cursos.
- 2026-11-22: **tarea 7b-2 hecha y, con ella, la 7b.** `FilterChips.kt` (`FiltersButton`,
  `RemovableFilterChip`, `SelectableTagChip`, con `@LightDarkPreviews`) y `FilterChipsTest` (21
  tests de emulador; primero en rojo, 19 de 20, contra un esqueleto que no dibujaba nada). Previews
  revisadas en claro y oscuro frente al Figma. Cambio respecto al criterio: **la prueba de 48 dp es
  tocar, no medir.** `assertHeightIsAtLeast` mide el nodo (lo que se ve: 32–37 dp) y no la zona
  táctil que Material amplía con `minimumInteractiveComponentSize` (igual que el `IconButton` de
  `SearchField`, que por eso fuerza `size(48.dp)`). Los tests tocan 22 dp fuera del dibujo y
  comprueban que cuenta, y 30 dp fuera (chip quitable) que no. `FiltersButton` y
  `RemovableFilterChip` usan `Surface(onClick)` con `semantics` (rol y lectura) y `clearAndSetSemantics`
  en el contenido para que el número y la ✕ no se lean aparte; `SelectableTagChip` es un `FilterChip`
  con los colores del tema. El ✓ es decorativo y no tiene test propio (el estado lo dice la casilla).

- 2026-11-22: **tarea 7c partida en tres pasos (aprobado):** 7c-1 la fila `FilterBar`, 7c-2 la hoja
  `FilterSheet` (el `FlowRow` de etiquetas con `verticalArrangement = spacedBy(0.dp)`: cada chip ya
  reserva 48 dp de alto) y 7c-3 previews y `filters = null` en `MainNavGraph`.
- 2026-11-22: **tarea 7c-1 hecha.** `FilterBar.kt` (fila con `horizontalScroll`: `FiltersButton` y un
  `RemovableFilterChip` por `ActiveFilter`, con `when` exhaustivo) y los 7 avisos nuevos de
  `SearchActions`. 10 tests nuevos en `ScreenSearchTest` (primero en rojo contra un esqueleto que no
  dibujaba nada: fallaron los 10 y pasaron los que ya había). Además del criterio: «Filtros, 1
  activo», el «#» del chip, la fila se desplaza si no caben, queda entre la caja y los resultados y
  está también en la primera carga y tras un fallo. Un test que ya existía
  (`an_empty_result_has_no_cards_or_buttons`) cuenta ahora el botón «Filtros» como excepción, igual
  que la caja. Desviación del plan: el parámetro `filters: FilterSheetState?` de `ScreenSearch` se
  añade en 7c-2 (donde se usa), para no tocar `MainNavGraph` ni las previews antes de tiempo.
  `ScreenSearchTest` pasa en el emulador.

- 2026-11-22: **tarea 7c-2 hecha.** `FilterSheet.kt` (la ventana: `ModalBottomSheet` entera, cierra
  con animación con `hideThen`), `FilterSheetContent.kt` (cabecera, Etiquetas con `FlowRow`,
  «Ver todas» de borde discontinuo y «Mostrar cursos») y 7 constantes de texto en
  `FilterTexts.kt`. `ScreenSearch` recibe `filters: FilterSheetState?` y dibuja la hoja si no es
  `null`. Tests primero: 24 de 26 en rojo contra un esqueleto vacío (los 2 que pasaban eran de «no
  aparece»). `FilterSheetTest` (27, nuevo fichero) y 3 en `ScreenSearchTest` (40). Con `:app:testDebugUnitTest`
  y `:app:connectedDebugAndroidTest` (121 en el emulador) en verde.
  - El `FlowRow` de etiquetas usa `verticalArrangement = spacedBy(0.dp)` (cada chip ya reserva 48 dp
    de alto); un test comprueba que las filas quedan a 48 dp de distancia entre sí.
  - **Fallo real que cazaron los tests:** con `hide()` cancelado por la animación de «atrás», el
    aviso de «Mostrar cursos» se perdía (la primera versión miraba `isVisible`). Ahora se avisa
    siempre salvo que el `scope` esté cancelado (la hoja ya ha salido de la pantalla).
    Tests: «atrás» mientras se esconde se ignora, el segundo toque no avisa dos veces y el otro
    botón queda ignorado.
  - Desviaciones del plan aprobado: los tests de la hoja van en un fichero nuevo
    (`FilterSheetTest.kt`) para no alargar `ScreenSearchTest` (que ya pasa de 480 líneas); el
    contenido va en `FilterSheetContent.kt` aparte de la ventana (`FilterSheet.kt`); y el
    `filters = null` de `MainNavGraph.kt` y de la preview base se puso ya aquí (si no, no
    compilaba), así que el 7c-3 solo añade previews.

- 2026-11-22: **tarea 7c-3 hecha y, con ella, la 7c.** 5 previews nuevas en
  `ScreenSearchPreviews.kt`, todas en claro y oscuro: `FiltersAppliedPreview` (la pantalla con dos
  etiquetas aplicadas) y la hoja sin ventana (`FilterSheetContent`, porque `ModalBottomSheet` no se
  dibuja en preview): con «Ver todas», tras pulsarlo, sin «Ver todas» y sin etiquetas. Revisadas
  renderizadas frente a la captura del Figma. Diferencias conocidas: las filas de chips quedan a
  48 dp (Figma, ~42) por el `verticalArrangement = 0.dp` pedido, y el chip de etiqueta mide 32 dp
  (Figma, 36; viene de la 7b-2, no se ha tocado). Sin tests nuevos: son solo previews.
- 2026-11-22: **dos detalles de la 7c, arreglados** (revisión de la 7c):
  1. La fila de filtros ocupa todo el ancho y el margen de 20 dp es relleno interior
     (`FilterBar(contentPadding)`): los chips que no caben pasan por debajo del borde en lugar de
     cortarse en el margen. Para eso `ScreenSearch` ya no pone el margen en la columna sino en
     cada hijo (`ScreenMargin`). Tests en `ScreenSearchTest`: la fila ocupa todo el ancho (en rojo
     antes), el primer chip empieza a 20 dp, el último acaba a 20 dp al final del desplazamiento y
     el chip que asoma se corta en el borde de la pantalla.
  2. «Ver todas» tiene `role = Role.Button` (como `FiltersButton`) y TalkBack lo anuncia como
     botón. Test `show_all_is_read_as_a_button` (en rojo antes).
  - Un test que escribí para el punto 1 («no cabe → fuera del borde») estaba mal planteado y
    fallaba antes y después: `getBoundsInRoot` da los límites ya recortados por la fila. Se
    sustituyó por el de «se corta en el borde de la pantalla». `ScreenSearchTest` (44) y
    `FilterSheetTest` (28) pasan.

- 2026-11-22: **tarea 7d partida en dos pasos (aprobado):** 7d-1 la hoja en el ViewModel y 7d-2
  la extensión `actions()` y `MainNavGraph`. Tres reglas nuevas aprobadas: abrir con la hoja ya
  abierta no hace nada (un doble toque no borra el borrador); cerrar no cancela la espera al
  teclear; los tests de los filtros van en un fichero nuevo (`SearchViewModelFiltersTest.kt`).
- 2026-11-22: **tarea 7d-1 hecha.** `SearchViewModel` guarda `filters: FilterSheetState?` y tiene
  `onOpenFilters`, `onToggleTag`, `onResetFilters`, `onShowAllTags` (las tres con `editFilters`:
  con la hoja cerrada no hacen nada), `onApplyFilters`, `onCloseFilters` y `onRemoveFilter`.
  `SearchViewModelFiltersTest` (26 tests; primero 17 en rojo contra funciones vacías: los 9 que
  pasaban son de «no ocurre nada»). Además del criterio: abrir dos veces, desmarcar, «Ver todas»,
  las acciones con la hoja cerrada, aplicar sin cambios también cancela la espera, aplicar con la
  hoja cerrada no la cancela, cerrar no la cancela, y quitar el último chip vuelve al catálogo
  entero. `./gradlew :app:testDebugUnitTest` pasa.

- 2026-11-22: **tarea 7d-2 hecha.** `fun SearchViewModel.actions(onCourseClick): SearchActions` al
  final de `SearchViewModel.kt` (referencias `this::onX`; `onCourseClick` pasa tal cual) y
  `MainNavGraph` pasa `filters = searchViewModel.filters` y `actions = searchViewModel.actions(…)`
  (la navegación con `onlyWhenResumed` sigue allí). 7 tests nuevos en `SearchViewModelTest` (32),
  primero los 7 en rojo contra un `actions()` que devolvía `SearchActions()`. Uno pasaba sin motivo
  (abría la hoja con la acción vacía y la daba por cerrada); ahora abre con el ViewModel y solo
  cierra con la acción. `./gradlew :app:testDebugUnitTest :app:assembleDebug` pasa.
- 2026-11-22: **tarea 7d hecha y, con ella, la tarea 7.** Comprobada a mano en el emulador con el
  servidor (`gradus.sh up`): dos etiquetas reducen los resultados con «Filtros 2» y sus chips;
  quitar un chip los amplía; «Restablecer», ✕, deslizar y «atrás» se comportan como la decisión 7;
  girar el móvil no pierde la hoja; TalkBack lee «Filtros, 2 activos» y «Quitar filtro…».

- 2026-11-22: tarea 8 discutida y detallada, partida en 8a (recuento en `FilterSheetState` y
  `asksTheSameAs` a `CourseQuery`), 8b (texto y botón) y 8c (ViewModel). Decisiones A–G aprobadas;
  la C: con 0 resultados, «Ningún curso coincide» y el botón desactivado. Añadido a la tarea 9 el
  arreglo de rendimiento de `onCourseClick` visto al revisar la 7d.

- 2026-11-22: **tarea 8a hecha** (pendiente de revisión). `asksTheSameAs` pasa a `CourseQuery.kt`
  como `internal`, con 11 tests directos en `CourseQueryTest` (28). `FilterSheetState` gana `preview`,
  `previewQuery`, `previewNeeded`, `startPreview`, `receivePreview`, `count` y `previewToApply`, y
  `DraftCount` va en su propio fichero (`DraftCount.kt`). 46 tests nuevos en `FilterSheetStateTest`
  (82), primero en rojo contra esqueletos con `TODO()`. Los tests de `queryToApply` se quedan en
  `FilterSheetStateTest` (no se ha borrado ninguno). Aparte de lo del plan, hay tests de: respuesta
  tardía, un recuento fallido que no se reintenta, lo aplicado con 0 resultados, texto tecleado
  distinto y `toggle`/`reset` que conservan `preview`. Con una decisión fuera del plan (ver la nota
  para el revisor en el 8a). `./gradlew :sharedLogic:testAndroidHostTest` y `:app:compileDebugKotlin`
  pasan.

- 2026-11-22: **tarea 8b-1 hecha** (los textos; el 8b se hace en dos sub-pasos). `showCoursesText(count:
  DraftCount)` y `NO_COURSES_MATCH_TEXT` en `FilterTexts.kt`, con 7 tests nuevos en `FilterTextsTest`
  (17), primero en rojo (no compilaba). `SHOW_COURSES_TEXT` se queda como texto sin número.
  `./gradlew :app:testDebugUnitTest` pasa. Falta el 8b-2: botón, cableado, previews y `FilterSheetTest`.
- 2026-11-22: **tarea 8b-2 hecha** (pendiente de la prueba a mano en el emulador). `FilterSheet` y
  `FilterSheetContent` reciben `count: DraftCount` (sin valor por defecto); el botón usa
  `showCoursesText(count)` y está desactivado con `Known(0)`. `ScreenSearch` lo calcula con
  `it.count(text, search)`, así que `MainNavGraph` no cambia. `FilterSheetTest`: 10 tests nuevos (38, 0
  fallos en el emulador Pixel 9a): cada texto del botón, desactivado con 0 y pulsarlo no avisa,
  con número avisa a `onApplyFilters`, y con 0 siguen funcionando «Restablecer», las etiquetas y ✕;
  primero en rojo (no compilaba). Previews nuevas en claro y oscuro: contando y con 0 resultados;
  las demás de la hoja pasan a «Mostrar 8 cursos». `:app:testDebugUnitTest`, `:app:assembleDebug` y
  `:app:connectedDebugAndroidTest` (`FilterSheetTest`) pasan. Hasta el 8c, tras elegir una etiqueta
  el botón dice «Mostrar cursos» sin número (nadie pide todavía el recuento).
- 2026-11-22: **tarea 8b hecha.** Probada a mano en el emulador con el servidor (usuario) y revisada.
  La nota para el revisor del 8a quedó cerrada en la revisión del 8a («se mantiene»).

- 2026-11-22: **tarea 8c partida en dos pasos (aprobado)** por un hueco del plan (E frente a
  `previewNeeded`; ver la nota para el revisor en el 8c). Los tests del recuento en el ViewModel van
  en un fichero nuevo, `SearchViewModelPreviewTest.kt`.
- 2026-11-22: **tarea 8c-1 hecha** (pendiente de revisión). `FilterSheetState.cancelPreview()`: olvida
  un recuento a medias y conserva uno terminado o fallido. 6 tests nuevos en `FilterSheetStateTest`
  (88), primero en rojo contra un esqueleto con `TODO()`: olvida el recuento en curso y conserva lo
  demás (borrador, «Ver todas»); tras cancelarlo el mismo borrador vuelve a necesitar recuento y
  `count` da `Counting`; una respuesta tardía del recuento cancelado se ignora; terminado, fallido y
  sin recuento → mismo estado. `./gradlew :sharedLogic:testAndroidHostTest :app:compileDebugKotlin`
  pasa.
- 2026-11-22: **tarea 8c-2 hecha** (pendiente de la prueba a mano en el emulador y de revisión).
  `SearchViewModel` tiene un trabajo `preview`: `onToggleTag` y `onResetFilters` pasan por
  `editDraft` (cancela el trabajo, `cancelPreview()` y, si `previewNeeded`, cuenta tras 300 ms con
  `countDraft`); `onApplyFilters` reutiliza `previewToApply` sin pedir nada (`useCounted`, que además
  cancela la petición de la lista en camino) o busca como siempre; `onCloseFilters` cancela el
  recuento. `onShowAllTags` sigue sin contar.
  - Ayudantes de test movidos (con permiso) a `SearchTestCatalog.kt` (`object`, se importan sus
    miembros: así no se confunden con el `catalog(total)` privado de `SearchViewModelTest`).
    `SearchViewModelFiltersTest` sigue con 26 tests; solo cambia `toggling a tag marks it in the
    draft without searching` → `… without searching at once` (`runCurrent` en vez de
    `advanceUntilIdle`, que ahora vería el recuento).
  - `SearchViewModelPreviewTest` (25 tests): primero 19 en rojo; los 6 que pasaban son de «no
    ocurre nada» (abrir, antes de la pausa, «Ver todas», volver a lo aplicado en la pausa, aplicar
    en la pausa, cerrar en la pausa). Cinco que pasaban sin motivo (no comprobaban que se hubiera
    contado) ganaron esa comprobación y pasaron a rojo. Además del criterio: mientras la petición va
    en camino el número es `Counting`; contar no cambia la lista; usa el texto de la caja;
    «Restablecer» también cuenta; «Ver todas» no; un cambio con la petición en camino la cancela;
    volver a un borrador cancelado lo cuenta otra vez (el hueco del 8c-1), a uno ya contado no, y a
    uno fallido tampoco; aplicar reutilizando cancela la espera al teclear y la petición de la lista
    en camino; aplicar en la pausa o tras un fallo busca como siempre; reabrir empieza sin recuento.
  - `./gradlew :sharedLogic:testAndroidHostTest :app:testDebugUnitTest :app:assembleDebug` pasa
    (paquete `search` de `app`: 111 tests). `MainNavGraph.kt` no cambia.
- 2026-11-22: **tarea 8c hecha y, con ella, la tarea 8.** Probada a mano en el emulador con el
  servidor (usuario): el número cambia al elegir etiquetas y coincide con «N resultados» al aplicar;
  con 0, «Ningún curso coincide» y no se puede pulsar. Revisada: la nota de `cancelPreview` se
  cierra con «se mantiene».

## Siguiente paso

Tarea 9 (repaso final), con la que se cierra la fase 1: detallarla antes de empezar. Después, como
se acordó, un chat orquestador nuevo para la fase 2 (tareas 10 y 11).
