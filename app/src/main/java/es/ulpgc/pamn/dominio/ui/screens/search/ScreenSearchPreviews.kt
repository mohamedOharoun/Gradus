package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
import es.ulpgc.pamn.dominio.course.search.SearchProblem
import es.ulpgc.pamn.dominio.course.search.SearchRequest
import es.ulpgc.pamn.dominio.course.search.SearchStatus
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
    GradusTheme { ScreenSearch(text = text, search = state, actions = SearchActions()) }
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
