package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CoursePage
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.SearchOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Tarea 3 de buscar-cursos.md: la máquina de estados de la lista de Buscar, sin corrutinas ni red.
// Las páginas de prueba son de 2 cursos.
class CourseSearchStateTest {

    private val graphs = CourseQuery(text = "grafos")
    private val trees = CourseQuery(text = "árboles")

    // Primera página recibida: cursos a y b de 5 (quedan más).
    private val ready = CourseSearchState.start().answer(found(page = 0, total = 5, "a", "b"))

    // ── Empezar ──

    @Test
    fun `starting loads the first page with no results`() {
        val state = CourseSearchState.start(graphs)

        assertEquals(graphs, state.query)
        assertEquals(SearchStatus.Loading(SearchRequest(graphs, 0)), state.status)
        assertEquals(SearchRequest(graphs, 0), state.pending)
        assertTrue(state.results.isEmpty())
        assertFalse(state.canLoadMore)
        assertFalse(state.isEmpty)
    }

    // ── Primera página ──

    @Test
    fun `the first page replaces the results and the search is ready`() {
        assertEquals(listOf("a", "b"), ready.ids)
        assertEquals(5, ready.total)
        assertTrue(ready.hasMore)
        assertEquals(1, ready.nextPage)
        assertEquals(SearchStatus.Ready, ready.status)
        assertNull(ready.pending)
        assertTrue(ready.canLoadMore)
        assertFalse(ready.isEmpty)
    }

    @Test
    fun `an empty first page leaves the search empty`() {
        val state = CourseSearchState.start().answer(found(page = 0, total = 0))

        assertTrue(state.isEmpty)
        assertFalse(state.hasMore)
        assertFalse(state.canLoadMore)
    }

    // ── Cargar más ──

    @Test
    fun `loading more asks for the next page and keeps the results`() {
        val state = ready.loadMore()

        assertEquals(SearchStatus.LoadingMore(SearchRequest(CourseQuery.EMPTY, 1)), state.status)
        assertEquals(listOf("a", "b"), state.ids)
        assertFalse(state.canLoadMore)
    }

    @Test
    fun `loading more does nothing when there are no more pages`() {
        val lastPage = CourseSearchState.start().answer(found(page = 0, total = 2, "a", "b"))

        assertFalse(lastPage.canLoadMore)
        assertEquals(lastPage, lastPage.loadMore())
    }

    @Test
    fun `loading more does nothing while the first page is loading`() {
        val loading = CourseSearchState.start()

        assertEquals(loading, loading.loadMore())
    }

    @Test
    fun `loading more does nothing while more is loading`() {
        val loadingMore = ready.loadMore()

        assertEquals(loadingMore, loadingMore.loadMore())
    }

    // ── Página siguiente ──

    @Test
    fun `the next page is added after the results with the total of that page`() {
        val state = ready.loadMore().answer(found(page = 1, total = 4, "c", "d"))

        assertEquals(listOf("a", "b", "c", "d"), state.ids)
        assertEquals(4, state.total)
        assertFalse(state.hasMore)
        assertEquals(2, state.nextPage)
        assertEquals(SearchStatus.Ready, state.status)
    }

    @Test
    fun `a course already shown is not repeated when the next page arrives`() {
        // Se publicó un curso entre página y página: «b» baja un puesto y vuelve a llegar.
        val state = ready.loadMore().answer(found(page = 1, total = 6, "b", "c"))

        assertEquals(listOf("a", "b", "c"), state.ids)
    }

    // ── Búsqueda nueva ──

    @Test
    fun `a new search starts again from the first page and keeps the results while loading`() {
        val state = ready.search(trees)

        assertEquals(trees, state.query)
        assertEquals(SearchStatus.Loading(SearchRequest(trees, 0)), state.status)
        assertEquals(listOf("a", "b"), state.ids)
        assertEquals(5, state.total)
        assertFalse(state.canLoadMore)
    }

    @Test
    fun `searching the same query again starts again`() {
        val state = ready.search(CourseQuery.EMPTY)

        assertEquals(SearchStatus.Loading(SearchRequest(CourseQuery.EMPTY, 0)), state.status)
    }

    @Test
    fun `the first page of a new search replaces the old results`() {
        val state = ready.search(trees).answer(found(page = 0, total = 1, "x"))

        assertEquals(listOf("x"), state.ids)
        assertEquals(1, state.total)
        assertEquals(1, state.nextPage)
    }

    // ── Respuestas tardías ──

    @Test
    fun `a late answer to an older search is ignored`() {
        val first = CourseSearchState.start(graphs)
        val olderRequest = first.pending!!
        val second = first.search(trees)

        assertEquals(second, second.receive(olderRequest, found(page = 0, total = 1, "g")))
    }

    @Test
    fun `a late answer to load more is ignored after a new search`() {
        val loadingMore = ready.loadMore()
        val moreRequest = loadingMore.pending!!
        val newSearch = loadingMore.search(trees)

        assertEquals(newSearch, newSearch.receive(moreRequest, found(page = 1, total = 5, "c", "d")))
    }

    @Test
    fun `a late failure of an older search is ignored`() {
        val older = ready.search(graphs)
        val olderRequest = older.pending!!
        val newer = older.search(trees)

        val state = newer.receive(olderRequest, SearchOutcome.Unreachable)

        assertEquals(newer, state)
        assertEquals(listOf("a", "b"), state.ids)
    }

    @Test
    fun `an answer with nothing pending is ignored`() {
        val answer = found(page = 0, total = 1, "z")

        assertEquals(ready, ready.receive(SearchRequest(CourseQuery.EMPTY, 0), answer))
    }

    // ── Fallos ──

    @Test
    fun `an unreachable first page fails without results`() {
        val state = ready.search(trees).answer(SearchOutcome.Unreachable)

        assertEquals(SearchStatus.Failed(SearchProblem.UNREACHABLE), state.status)
        assertEquals(trees, state.query)
        assertTrue(state.results.isEmpty())
        assertEquals(0, state.total)
        assertFalse(state.hasMore)
        assertEquals(0, state.nextPage)
        assertNull(state.pending)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `a server failure on the first page fails without results`() {
        val state = CourseSearchState.start().answer(SearchOutcome.ServerFailure)

        assertEquals(SearchStatus.Failed(SearchProblem.SERVER_FAILURE), state.status)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `a failed load more keeps the results`() {
        val state = ready.loadMore().answer(SearchOutcome.Unreachable)

        assertEquals(SearchStatus.MoreFailed(SearchProblem.UNREACHABLE), state.status)
        assertEquals(listOf("a", "b"), state.ids)
        assertEquals(5, state.total)
        assertTrue(state.hasMore)
        assertEquals(1, state.nextPage)
        assertFalse(state.canLoadMore)
        assertEquals(state, state.loadMore())
    }

    // ── Reintentar ──

    @Test
    fun `retrying after a failed first page loads it again`() {
        val failed = CourseSearchState.start(graphs).answer(SearchOutcome.ServerFailure)

        assertEquals(SearchStatus.Loading(SearchRequest(graphs, 0)), failed.retry().status)
    }

    @Test
    fun `retrying after a failed load more asks for the same page again`() {
        val retried = ready.loadMore().answer(SearchOutcome.ServerFailure).retry()

        assertEquals(SearchStatus.LoadingMore(SearchRequest(CourseQuery.EMPTY, 1)), retried.status)
        assertEquals(listOf("a", "b"), retried.ids)
    }

    @Test
    fun `retrying without a failure does nothing`() {
        val loading = CourseSearchState.start()

        assertEquals(ready, ready.retry())
        assertEquals(loading, loading.retry())
    }
}

// Responde a la petición pendiente, como hará el ViewModel.
private fun CourseSearchState.answer(outcome: SearchOutcome): CourseSearchState = receive(pending!!, outcome)

private val CourseSearchState.ids: List<String> get() = results.map { it.id }

private fun found(page: Int, total: Int, vararg ids: String): SearchOutcome =
    SearchOutcome.Found(CoursePage(items = ids.map(::course), page = page, size = 2, total = total))

private fun course(id: String) = CourseSummary(
    id = id,
    version = 1,
    author = Author(id = "u1", name = "Ana Ruiz"),
    publishedAt = "2026-10-03T08:35:00Z",
    title = "Curso $id",
    description = "",
    language = "es",
    tags = emptyList(),
    moduleCount = 3,
)
