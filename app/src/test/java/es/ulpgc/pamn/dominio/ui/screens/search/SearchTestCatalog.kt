package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CoursePage
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.SearchOutcome
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.DraftCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent

// Solo para tests. Un catálogo pequeño con etiquetas y los ayudantes de la hoja de filtros,
// compartidos por SearchViewModelFiltersTest, SearchViewModelPreviewTest y
// SearchViewModelAuthorPublishedTest. Va en un `object` para que cada test importe lo que usa
// (`import ….SearchTestCatalog.catalog`): así no se confunde con el `catalog(total)` privado de
// SearchViewModelTest, que es otro.
@OptIn(ExperimentalCoroutinesApi::class)
object SearchTestCatalog {

    const val PAGE_SIZE = 20

    // «grafos» está en 3 cursos, «algoritmos» y «redes» en 2 y «árboles» en 1. Juntas: «grafos» y
    // «algoritmos» en 2, «grafos» y «redes» en 1, «redes» y «algoritmos» en ninguno.
    fun catalog() = catalogOf(
        course("grafos-1", "grafos", "algoritmos"),
        course("grafos-2", "grafos", "redes"),
        course("arboles-1", "grafos", "algoritmos", "árboles"),
        course("redes-1", "redes"),
    )

    // Busca el texto en el título (el id) y exige todas las etiquetas, sin distinguir mayúsculas.
    fun catalogOf(vararg courses: CourseSummary): (CourseQuery, Int) -> SearchOutcome = { (text, tags), page ->
        val wanted = tags.map(String::lowercase)
        val matches = courses.filter { course ->
            course.title.contains(text.trim(), ignoreCase = true) &&
                wanted.all { tag -> course.tags.any { it.lowercase() == tag } }
        }
        val items = matches.chunked(PAGE_SIZE).getOrElse(page) { emptyList() }
        SearchOutcome.Found(CoursePage(items = items, page = page, size = PAGE_SIZE, total = matches.size))
    }

    fun course(id: String, vararg tags: String) = CourseSummary(
        id = id,
        version = 1,
        author = Author(id = "autor", name = "Autor de prueba"),
        publishedAt = "2026-10-03T08:35:00Z",
        title = id,
        description = "",
        language = "es",
        tags = tags.toList(),
        moduleCount = 1,
    )

    // Buscar con el catálogo cargado y la hoja abierta. Extensión de TestScope (el `this` de runTest)
    // para poder llamar a advanceUntilIdle.
    fun TestScope.opened(gateway: CourseGateway): SearchViewModel {
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        viewModel.onOpenFilters()
        return viewModel
    }

    // Lo que dice el botón, calculado como en ScreenSearch.
    fun SearchViewModel.draftCount(): DraftCount = filters!!.count(text, search)

    // Abre la hoja, edita el borrador con cualquier acción del ViewModel (lambda con receptor), aplica
    // y deja que llegue la respuesta:
    // `applyDraft(viewModel) { onAuthorChange("Ana"); onPublishedChange(LAST_YEAR) }`.
    fun TestScope.applyDraft(viewModel: SearchViewModel, edit: SearchViewModel.() -> Unit) {
        viewModel.onOpenFilters()
        viewModel.edit()
        viewModel.onApplyFilters()
        runCurrent()
    }

    // Atajo de applyDraft que solo marca etiquetas.
    fun TestScope.applyTags(viewModel: SearchViewModel, vararg tags: String) =
        applyDraft(viewModel) { tags.forEach { onToggleTag(it) } }
}
