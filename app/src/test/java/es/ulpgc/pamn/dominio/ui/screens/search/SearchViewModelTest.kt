package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CoursePage
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.SearchOutcome
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
import es.ulpgc.pamn.dominio.course.search.SearchProblem
import es.ulpgc.pamn.dominio.course.search.SearchRequest
import es.ulpgc.pamn.dominio.course.search.SearchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Tiempo simulado: StandardTestDispatcher no ejecuta nada hasta que el test avanza el reloj
// (advanceTimeBy) o ejecuta lo pendiente en este instante (runCurrent). `delay(300)` no espera de verdad.
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    // viewModelScope usa Dispatchers.Main: en los tests se sustituye por el reloj simulado.
    // runTest comparte el mismo reloj al ver que Main es un TestDispatcher.
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val grafos = CourseQuery(text = "grafos")

    // --- Al abrir Buscar ---

    @Test
    fun `starts by asking for the first page of the empty query without waiting`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        SearchViewModel(gateway)
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `starts with an empty text and loading the first page`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        assertEquals("", viewModel.text)
        assertEquals(SearchRequest(CourseQuery.EMPTY, 0), viewModel.search.pending)
    }

    @Test
    fun `the first page reaches the state`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog(total = 25)))
        runCurrent()
        assertEquals(SearchStatus.Ready, viewModel.search.status)
        assertEquals(20, viewModel.search.results.size)
        assertEquals(25, viewModel.search.total)
    }

    @Test
    fun `a failure reaches the state`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = always(SearchOutcome.ServerFailure)))
        runCurrent()
        assertEquals(SearchStatus.Failed(SearchProblem.SERVER_FAILURE), viewModel.search.status)
    }

    @Test
    fun `the factory builds a view model that uses the given gateway`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel.factory(gateway).create(SearchViewModel::class.java, MutableCreationExtras())
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
        assertEquals(SearchStatus.Ready, viewModel.search.status)
    }

    // --- Teclear (espera de 300 ms) ---

    @Test
    fun `typing keeps the text at once but does not apply it yet`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onTextChange("grafos")
        assertEquals("grafos", viewModel.text)
        assertEquals(CourseQuery.EMPTY, viewModel.search.query)
    }

    @Test
    fun `typing searches after 300 ms without typing, not before`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        advanceTimeBy(299.milliseconds)
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
    }

    @Test
    fun `after the wait the typed text is the applied query and its results arrive`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onTextChange("grafos")
        advanceUntilIdle()
        assertEquals(grafos, viewModel.search.query)
        assertEquals(SearchStatus.Ready, viewModel.search.status)
        assertTrue(viewModel.search.isAllFrom("grafos"))
    }

    @Test
    fun `typing again restarts the wait`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("gra")
        advanceTimeBy(200.milliseconds)
        viewModel.onTextChange("grafos")
        advanceTimeBy(299.milliseconds)
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
    }

    @Test
    fun `typing several times in a row only searches the last text`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("g")
        viewModel.onTextChange("gr")
        viewModel.onTextChange("grafos")
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
    }

    @Test
    fun `waiting for typing does not cancel the request in flight`() = runTest {
        val gateway = DelayedCourseGateway(FakeCourseGateway(searchAnswer = catalog()), latency = 100.milliseconds)
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        advanceTimeBy(101.milliseconds)
        // La primera página llega mientras se espera a que deje de teclear.
        assertEquals(CourseQuery.EMPTY, viewModel.search.query)
        assertEquals(SearchStatus.Ready, viewModel.search.status)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.answered)
    }

    // --- Tecla «Buscar» ---

    @Test
    fun `the search key searches at once with the current text`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onSearch()
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `the search key cancels the pending wait`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onSearch()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
    }

    @Test
    fun `the search key searches again even if the text has not changed`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onSearch()
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `a new search cancels the request in flight`() = runTest {
        val gateway = DelayedCourseGateway(FakeCourseGateway(searchAnswer = catalog()), latency = 100.milliseconds)
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onSearch()
        advanceUntilIdle()
        // La primera página se pidió, pero se canceló antes de responder.
        assertEquals(listOf(grafos to 0), gateway.answered)
        assertEquals(grafos, viewModel.search.query)
    }

    // --- «Cargar más» ---

    @Test
    fun `load more asks for the next page and adds it`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog(total = 25))
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onLoadMore()
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery.EMPTY to 1), gateway.searches)
        assertEquals(25, viewModel.search.results.size)
        assertEquals(false, viewModel.search.hasMore)
    }

    @Test
    fun `load more does nothing when there are no more`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog(total = 3))
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onLoadMore()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `load more does nothing while the first page is loading`() = runTest {
        val gateway = DelayedCourseGateway(FakeCourseGateway(searchAnswer = catalog(total = 25)), latency = 100.milliseconds)
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onLoadMore()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.answered)
    }

    @Test
    fun `load more does nothing while already loading more`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog(total = 25))
        val viewModel = SearchViewModel(DelayedCourseGateway(fake, latency = 100.milliseconds))
        advanceUntilIdle()
        viewModel.onLoadMore()
        runCurrent()
        viewModel.onLoadMore()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery.EMPTY to 1), fake.searches)
    }

    @Test
    fun `typing during load more does not add the next page to the new search`() = runTest {
        val gateway = DelayedCourseGateway(FakeCourseGateway(searchAnswer = catalog(total = 25)), latency = 1.seconds)
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        viewModel.onLoadMore()
        runCurrent()
        viewModel.onTextChange("grafos")
        advanceUntilIdle()
        // La página 1 de la búsqueda vacía se canceló al empezar la de «grafos».
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.answered)
        assertEquals(grafos, viewModel.search.query)
        assertEquals(20, viewModel.search.results.size)
        assertTrue(viewModel.search.isAllFrom("grafos"))
    }

    @Test
    fun `load more while waiting for typing still searches the typed text`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog(total = 25))
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onLoadMore()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery.EMPTY to 1, grafos to 0), gateway.searches)
        assertEquals(grafos, viewModel.search.query)
    }

    // --- «Reintentar» ---

    @Test
    fun `retry after a failed first page asks for it again`() = runTest {
        var answer = always(SearchOutcome.Unreachable)
        val gateway = FakeCourseGateway(searchAnswer = { query, page -> answer(query, page) })
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        assertEquals(SearchStatus.Failed(SearchProblem.UNREACHABLE), viewModel.search.status)
        answer = catalog()
        viewModel.onRetry()
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery.EMPTY to 0), gateway.searches)
        assertEquals(SearchStatus.Ready, viewModel.search.status)
    }

    @Test
    fun `retry after a failed load more asks for the next page again`() = runTest {
        var answer: (CourseQuery, Int) -> SearchOutcome = catalog(total = 25)
        val gateway = FakeCourseGateway(searchAnswer = { query, page -> answer(query, page) })
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        answer = always(SearchOutcome.ServerFailure)
        viewModel.onLoadMore()
        runCurrent()
        assertEquals(SearchStatus.MoreFailed(SearchProblem.SERVER_FAILURE), viewModel.search.status)
        answer = catalog(total = 25)
        viewModel.onRetry()
        runCurrent()
        assertEquals(CourseQuery.EMPTY to 1, gateway.searches.last())
        assertEquals(25, viewModel.search.results.size)
    }

    @Test
    fun `retry without a failure does nothing`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onRetry()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `retry while waiting for typing still searches the typed text`() = runTest {
        var answer = always(SearchOutcome.Unreachable)
        val gateway = FakeCourseGateway(searchAnswer = { query, page -> answer(query, page) })
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        answer = catalog()
        viewModel.onTextChange("grafos")
        viewModel.onRetry()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(grafos, viewModel.search.query)
    }

    // --- Ayudantes ---

    // Un catálogo de `total` cursos en páginas de 20, con ids «texto-n» para saber de qué búsqueda son.
    private fun catalog(total: Int = 25): (CourseQuery, Int) -> SearchOutcome =
        { query, page -> pageOf(query.text, page, total) }

    private fun pageOf(text: String, page: Int, total: Int): SearchOutcome {
        val first = page * PAGE_SIZE
        val items = (first until minOf(first + PAGE_SIZE, total)).map { summary("$text-$it") }
        return SearchOutcome.Found(CoursePage(items = items, page = page, size = PAGE_SIZE, total = total))
    }

    // ¿Todos los resultados son de la búsqueda de ese texto? (Los ids son «texto-n».)
    private fun CourseSearchState.isAllFrom(text: String) =
        results.all { course -> course.id.startsWith("$text-") }

    // El gateway responde siempre lo mismo, pida lo que pida (para provocar fallos).
    private fun always(outcome: SearchOutcome): (CourseQuery, Int) -> SearchOutcome = { _, _ -> outcome }

    private fun summary(id: String) = CourseSummary(
        id = id,
        version = 1,
        author = Author(id = "autor", name = "Autor de prueba"),
        publishedAt = "2026-10-03T08:35:00Z",
        title = id,
        description = "",
        language = "es",
        tags = emptyList(),
        moduleCount = 1,
    )

    private companion object {
        const val PAGE_SIZE = 20
    }
}
