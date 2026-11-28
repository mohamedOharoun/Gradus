package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import es.ulpgc.pamn.dominio.course.search.SearchProblem
import es.ulpgc.pamn.dominio.course.search.SearchRequest
import es.ulpgc.pamn.dominio.course.search.SearchStatus
import es.ulpgc.pamn.dominio.course.search.TagSuggestions
import es.ulpgc.pamn.dominio.data.demoSearchState
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.screens.ScreenSearch
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Una preview por estado de la pantalla Buscar, en claro y oscuro, con los cursos del Figma.
// Aparte de ScreenSearch.kt para que ese fichero no crezca con datos de ejemplo.

private val firstPage = SearchStatus.Loading(SearchRequest(CourseQuery.EMPTY, 0))
private val nextPage = SearchStatus.LoadingMore(SearchRequest(CourseQuery.EMPTY, 1))

@Composable
private fun Preview(text: String = "grafos", state: CourseSearchState) {
    GradusTheme { ScreenSearch(text = text, search = state, filters = null, actions = SearchActions()) }
}

@LightDarkPreviews
@Composable
private fun FirstLoadPreview() = Preview(text = "", state = demoSearchState(firstPage, results = emptyList(), total = 0))

@LightDarkPreviews
@Composable
private fun ResultsWithLoadMorePreview() = Preview(state = demoSearchState(hasMore = true))

@LightDarkPreviews
@Composable
private fun SearchingWithResultsPreview() = Preview(state = demoSearchState(firstPage))

@LightDarkPreviews
@Composable
private fun LoadingMorePreview() = Preview(state = demoSearchState(nextPage, hasMore = true))

@LightDarkPreviews
@Composable
private fun LoadMoreFailedPreview() =
    Preview(state = demoSearchState(SearchStatus.MoreFailed(SearchProblem.UNREACHABLE), hasMore = true))

@LightDarkPreviews
@Composable
private fun FailedPreview() = Preview(
    state = demoSearchState(SearchStatus.Failed(SearchProblem.UNREACHABLE), results = emptyList(), total = 0),
)

@LightDarkPreviews
@Composable
private fun EmptyPreview() = Preview(
    text = "zzz",
    state = demoSearchState(results = emptyList(), total = 0, query = CourseQuery(text = "zzz")),
)

// --- Filtros (tarea 7) ---

// Dos filtros aplicados: «Filtros 2» y un chip quitable por etiqueta (Figma «3 · Buscar»).
@LightDarkPreviews
@Composable
private fun FiltersAppliedPreview() =
    Preview(state = demoSearchState(query = CourseQuery(text = "grafos", tags = listOf("grafos", "algoritmos"))))

// La hoja se ve aquí sin ventana (FilterSheetContent): con ModalBottomSheet la preview no la dibuja.
// Figma «4 · Buscar con filtros»: 7 etiquetas y «Ver todas», con «grafos» marcada.
private val sevenTags = listOf("grafos", "algoritmos", "árboles", "kotlin", "android", "matemáticas", "bases de datos")

private fun sheetOf(
    shown: List<String>,
    hidden: List<String>,
    selected: List<String> = emptyList(),
    author: String = "",
    published: PublishedWithin = PublishedWithin.ANY,
) = FilterSheetState(
    draft = CourseQuery(tags = selected, author = author, published = published),
    suggestions = TagSuggestions(shown, hidden),
)

// Por defecto, como el Figma: «Mostrar 8 cursos».
@Composable
private fun SheetPreview(state: FilterSheetState, count: DraftCount = DraftCount.Known(8)) {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FilterSheetContent(state = state, count = count, actions = SearchActions(), onApply = {}, onClose = {})
        }
    }
}

@LightDarkPreviews
@Composable
private fun SheetWithShowAllPreview() =
    SheetPreview(sheetOf(shown = sevenTags, hidden = listOf("redes", "sql", "testing"), selected = listOf("grafos")))

// Como el Figma, con creador y fecha elegidos (11d-1): «Restablecer» activo y «Último año» marcado.
@LightDarkPreviews
@Composable
private fun SheetWithAuthorAndPublishedPreview() = SheetPreview(
    sheetOf(
        shown = sevenTags, hidden = listOf("redes", "sql", "testing"), selected = listOf("grafos"),
        author = "Ana", published = PublishedWithin.LAST_YEAR,
    ),
)

@LightDarkPreviews
@Composable
private fun SheetAfterShowAllPreview() =
    SheetPreview(sheetOf(shown = sevenTags, hidden = listOf("redes", "sql", "testing"), selected = listOf("grafos")).showAll())

@LightDarkPreviews
@Composable
private fun SheetWithoutShowAllPreview() = SheetPreview(sheetOf(shown = sevenTags.take(4), hidden = emptyList()))

@LightDarkPreviews
@Composable
private fun SheetWithoutTagsPreview() = SheetPreview(sheetOf(shown = emptyList(), hidden = emptyList()))

// El botón según el recuento del borrador (tarea 8b): contando (sin número) y sin resultados (desactivado).
@LightDarkPreviews
@Composable
private fun SheetCountingPreview() = SheetPreview(
    sheetOf(shown = sevenTags, hidden = listOf("redes", "sql", "testing"), selected = listOf("grafos", "kotlin")),
    count = DraftCount.Counting,
)

@LightDarkPreviews
@Composable
private fun SheetWithNoCoursesPreview() = SheetPreview(
    sheetOf(shown = sevenTags, hidden = listOf("redes", "sql", "testing"), selected = listOf("grafos", "kotlin")),
    count = DraftCount.Known(0),
)
