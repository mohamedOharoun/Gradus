package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.CoursePage
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.SearchOutcome

// Una petición al catálogo: qué consulta y qué página (empieza en 0).
data class SearchRequest(val query: CourseQuery, val page: Int)

// Por qué ha fallado una búsqueda. Los dos se pueden reintentar.
enum class SearchProblem { UNREACHABLE, SERVER_FAILURE }

// En qué punto está la lista. Las peticiones pendientes van dentro de Loading y LoadingMore: así no
// se puede escribir un estado imposible («listo, pero esperando respuesta»).
sealed interface SearchStatus {
    // Esperando la primera página de una búsqueda.
    data class Loading(val request: SearchRequest) : SearchStatus

    // Esperando la página siguiente («Cargar más»).
    data class LoadingMore(val request: SearchRequest) : SearchStatus

    data object Ready : SearchStatus

    // Falló la primera página: no hay resultados que enseñar.
    data class Failed(val problem: SearchProblem) : SearchStatus

    // Falló «Cargar más»: se conservan los resultados que ya había.
    data class MoreFailed(val problem: SearchProblem) : SearchStatus
}

// Lo que enseña la lista de Buscar. Máquina de estados inmutable, como ModuleStudy: cada acción
// devuelve un estado nuevo y, si no toca, el mismo. Sin corrutinas ni red: quien la usa (el
// ViewModel) lee `pending`, llama al gateway y devuelve la respuesta con `receive`.
data class CourseSearchState(
    val query: CourseQuery,               // la consulta aplicada (no lo que se está tecleando)
    val results: List<CourseSummary>,
    val total: Int,
    val nextPage: Int,
    val hasMore: Boolean,
    val status: SearchStatus,
) {
    // La petición que espera respuesta, o null si no se espera ninguna.
    val pending: SearchRequest?
        get() = when (status) {
            is SearchStatus.Loading -> status.request
            is SearchStatus.LoadingMore -> status.request
            else -> null
        }

    // Se enseña «Cargar más».
    val canLoadMore: Boolean get() = status == SearchStatus.Ready && hasMore

    // Se enseña «sin resultados» (mientras carga o tras un fallo, no).
    val isEmpty: Boolean get() = status == SearchStatus.Ready && results.isEmpty()

    private val isLoadingMore: Boolean get() = status is SearchStatus.LoadingMore

    // Siempre vuelve a empezar, aunque la consulta sea la misma. Mientras carga se conservan los
    // resultados, el total y si hay más, para que la lista no parpadee.
    fun search(query: CourseQuery): CourseSearchState =
        copy(query = query, status = SearchStatus.Loading(SearchRequest(query, 0)))

    fun loadMore(): CourseSearchState = if (canLoadMore) loadingNextPage() else this

    // Repite lo que falló: la primera página o la siguiente.
    fun retry(): CourseSearchState = when (status) {
        is SearchStatus.Failed -> search(query)
        is SearchStatus.MoreFailed -> loadingNextPage()
        else -> this
    }

    // Una respuesta que no es la de la petición pendiente (de una búsqueda ya superada, o sin nada
    // pendiente) llega tarde y se ignora.
    fun receive(request: SearchRequest, outcome: SearchOutcome): CourseSearchState {
        if (request != pending) return this
        return when (outcome) {
            is SearchOutcome.Found -> accept(request, outcome.page)
            SearchOutcome.Unreachable -> fail(SearchProblem.UNREACHABLE)
            SearchOutcome.ServerFailure -> fail(SearchProblem.SERVER_FAILURE)
        }
    }

    private fun loadingNextPage(): CourseSearchState =
        copy(status = SearchStatus.LoadingMore(SearchRequest(query, nextPage)))

    // La primera página sustituye los resultados; la siguiente se añade sin repetir ningún curso
    // (si se publica uno entre página y página, todo baja un puesto y uno volvería a llegar).
    private fun accept(request: SearchRequest, page: CoursePage): CourseSearchState = copy(
        results = if (isLoadingMore) (results + page.items).distinctBy { it.id } else page.items,
        total = page.total,
        nextPage = request.page + 1,
        hasMore = page.hasMore,
        status = SearchStatus.Ready,
    )

    // Si falla la primera página, los resultados que había eran de otra búsqueda: fuera.
    private fun fail(problem: SearchProblem): CourseSearchState =
        if (isLoadingMore) {
            copy(status = SearchStatus.MoreFailed(problem))
        } else {
            copy(results = emptyList(), total = 0, nextPage = 0, hasMore = false, status = SearchStatus.Failed(problem))
        }

    companion object {
        // Factory method: la lista recién abierta, esperando la primera página.
        fun start(query: CourseQuery = CourseQuery.EMPTY): CourseSearchState = CourseSearchState(
            query = query,
            results = emptyList(),
            total = 0,
            nextPage = 0,
            hasMore = false,
            status = SearchStatus.Loading(SearchRequest(query, 0)),
        )
    }
}
