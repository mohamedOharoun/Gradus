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
    └── TagSuggestions            qué etiquetas ofrecer en la hoja y en qué orden
app/.../ui/
├── screens/ScreenSearch.kt                 (ya existe; deja de ser provisional)
├── screens/search/SearchViewModel.kt       espera al teclear, cancela la búsqueda anterior
├── screens/search/SearchActions.kt         eventos de la pantalla (como LoginActions)
├── screens/search/SearchResultsContent.kt  lista, cargando, vacío, problema, «Cargar más»
├── screens/search/FilterSheet.kt           hoja inferior (ModalBottomSheet)
└── components/
    ├── SearchField.kt            caja redondeada con lupa y ✕
    ├── CourseCard.kt             tarjeta de un resultado
    ├── TagLabel.kt               etiqueta gris de la tarjeta
    └── FilterChips.kt            «Filtros n», chip quitable y chip seleccionable
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

5. **Componentes con previews**: `SearchField`, `CourseCard` (+ `TagLabel`).
   - Colores del tema (`surfaceVariant` para las etiquetas, `outlineVariant` para el borde).
   - La tarjeta se lee entera con TalkBack («Estructuras de datos, por Ana Ruiz, …»).
   - Criterio: previews en claro y oscuro iguales al Figma (sin ★ ni «Inscrito»).
6. **`SearchViewModel` + `ScreenSearch` con resultados** (sin filtros todavía).
   - Lista, «N resultados» (y «1 resultado»), cargando, vacío, problema con «Reintentar», «Cargar más».
   - `MainNavGraph`: tocar una tarjeta → `Route.Course(id)`.
   - Criterio (emulador, con `gradus.sh up`): Buscar lista los cursos; escribir filtra; abrir un
     curso y volver deja la búsqueda igual; con el servidor apagado sale el aviso y «Reintentar».
7. **Filtros: fila de chips + hoja con Etiquetas.**
   - `FilterChips` («Filtros n», «#grafos ✕», chip seleccionable con ✓) y `FilterSheet`.
   - Criterio (emulador): elegir dos etiquetas reduce los resultados; quitar un chip los amplía;
     «Restablecer» y ✕ funcionan como dice la decisión 7.
8. **«Mostrar N cursos»** (decisión 6).
   - Criterio: el número cambia al elegir etiquetas y coincide con «N resultados» al aplicar.
9. **Repaso final.**
   - Quitar el botón provisional de Inicio (decisión 9).
   - TalkBack: «Quitar filtro grafos», «Filtros, 2 activos», etiquetas seleccionadas como tales.
   - Modo oscuro, `especificacion.md` (qué filtros hay hoy; quitar «relevancia» de los órdenes,
     decisión 10) y `cursos-backend-app.md` (tarea 6 hecha).

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

## Siguiente paso

Tarea 5 (componentes `SearchField`, `CourseCard` y `TagLabel` con previews en claro y oscuro):
proponerla antes de empezar. Usará `publishedMonth` para el «oct 2026» de la tarjeta.
