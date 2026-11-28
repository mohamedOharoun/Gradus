package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.SearchOutcome
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.SearchStatus
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.applyTags
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.catalog
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.draftCount
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.opened
import es.ulpgc.pamn.dominio.ui.screens.search.SearchViewModel.Companion.TYPING_PAUSE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

// El recuento de «Mostrar N cursos» en SearchViewModel (tarea 8c de buscar-cursos.md). Mismo reloj
// simulado que SearchViewModelTest: nada ocurre hasta runCurrent, advanceTimeBy o advanceUntilIdle.
// Con DelayedCourseGateway las respuestas tardan LATENCY: así se puede cambiar algo mientras una
// petición está en camino (`searches` del falso apunta lo pedido; `answered`, lo que llegó).
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelPreviewTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val grafos = CourseQuery(tags = listOf("grafos"))
    private val grafosRedes = CourseQuery(tags = listOf("grafos", "redes"))

    // --- Al abrir ---

    @Test
    fun `opening asks for nothing and the number is the applied total`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    // --- Marcar: la pausa y el recuento ---

    @Test
    fun `toggling asks for nothing before the pause`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE - 1.milliseconds)
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
        assertEquals(DraftCount.Counting, viewModel.draftCount())
    }

    @Test
    fun `toggling counts the draft after the pause`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(DraftCount.Known(3), viewModel.draftCount())
    }

    @Test
    fun `while the count is on its way the number is counting`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(DelayedCourseGateway(fake, LATENCY))
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()
        assertEquals(grafos to 0, fake.searches.last())
        assertEquals(DraftCount.Counting, viewModel.draftCount())
        advanceTimeBy(LATENCY)
        runCurrent()
        assertEquals(DraftCount.Known(3), viewModel.draftCount())
    }

    @Test
    fun `counting does not change the applied list`() = runTest {
        val viewModel = opened(FakeCourseGateway(searchAnswer = catalog()))
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        assertEquals(DraftCount.Known(3), viewModel.draftCount())  // sí se ha contado
        assertEquals(CourseQuery.EMPTY, viewModel.search.query)
        assertEquals(4, viewModel.search.results.size)
    }

    @Test
    fun `the count uses the text in the box`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        viewModel.onTextChange("grafos")
        viewModel.onSearch()
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("redes")
        advanceUntilIdle()
        assertEquals(CourseQuery(text = "grafos", tags = listOf("redes")) to 0, gateway.searches.last())
        assertEquals(DraftCount.Known(1), viewModel.draftCount())
    }

    @Test
    fun `reset counts the emptied draft too`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        applyTags(viewModel, "redes")
        viewModel.onOpenFilters()
        viewModel.onResetFilters()
        advanceUntilIdle()
        assertEquals(CourseQuery.EMPTY to 0, gateway.searches.last())
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    @Test
    fun `show all does not count`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onShowAllTags()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    // --- Varios cambios (decisión E: un solo trabajo de recuento) ---

    @Test
    fun `two changes in a row only count the last one`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE - 1.milliseconds)
        viewModel.onToggleTag("redes")
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafosRedes to 0), gateway.searches)
        assertEquals(DraftCount.Known(1), viewModel.draftCount())
    }

    @Test
    fun `a change while the count is on its way cancels it and counts the new draft`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val gateway = DelayedCourseGateway(fake, LATENCY)
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()  // el recuento de «grafos» está en camino
        viewModel.onToggleTag("redes")
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0, grafosRedes to 0), fake.searches)
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafosRedes to 0), gateway.answered)
        assertEquals(DraftCount.Known(1), viewModel.draftCount())
    }

    @Test
    fun `going back to the applied draft during the pause asks for nothing`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    @Test
    fun `going back to the applied draft cancels the count on its way`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val gateway = DelayedCourseGateway(fake, LATENCY)
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), fake.searches)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.answered)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    // El hueco del plan que arregla cancelPreview (8c-1): sin él, el número no llegaba nunca.
    @Test
    fun `going back to a draft whose count was cancelled counts it again`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val gateway = DelayedCourseGateway(fake, LATENCY)
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()  // el recuento de «grafos» está en camino
        viewModel.onToggleTag("redes")  // lo cancela
        viewModel.onToggleTag("redes")  // el borrador vuelve a ser «grafos»
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0, grafos to 0), fake.searches)
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.answered)
        assertEquals(DraftCount.Known(3), viewModel.draftCount())
    }

    @Test
    fun `going back to a draft already counted does not count it again`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        viewModel.onToggleTag("redes")
        viewModel.onToggleTag("redes")
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(DraftCount.Known(3), viewModel.draftCount())
    }

    // --- Recuento fallido ---

    @Test
    fun `a failed count gives unknown`() = runTest {
        val viewModel = opened(FakeCourseGateway(searchAnswer = failingWithTags()))
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        assertEquals(DraftCount.Unknown, viewModel.draftCount())
    }

    @Test
    fun `going back to a draft whose count failed does not retry it on its own`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = failingWithTags())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        viewModel.onToggleTag("redes")
        viewModel.onToggleTag("redes")
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(DraftCount.Unknown, viewModel.draftCount())
    }

    // --- Aplicar (decisión D) ---

    @Test
    fun `applying with the count done asks for nothing and keeps its results`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        assertEquals(DraftCount.Known(3), viewModel.draftCount())  // el recuento ha terminado
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertNull(viewModel.filters)
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(grafos, viewModel.search.query)
        assertEquals(SearchStatus.Ready, viewModel.search.status)
        assertEquals(3, viewModel.search.total)
        assertEquals(listOf("grafos-1", "grafos-2", "arboles-1"), viewModel.search.results.map(CourseSummary::id))
    }

    @Test
    fun `applying with the count done cancels the typing wait`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        viewModel.onTextChange(" ")  // pide lo mismo que el texto vacío del recuento
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(grafos, viewModel.search.query)
    }

    // La lista sigue cargando (p. ej. la tecla «Buscar» justo antes) cuando se aplica un recuento
    // hecho: esa respuesta ya no es de lo aplicado y no debe pisar los resultados.
    @Test
    fun `applying with the count done drops the answer of the list on its way`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val gateway = DelayedCourseGateway(fake, LATENCY)
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        assertEquals(DraftCount.Known(3), viewModel.draftCount())  // el recuento ha terminado
        viewModel.onSearch()  // la lista vuelve a pedir el catálogo entero
        runCurrent()
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.answered)
        assertEquals(grafos, viewModel.search.query)
        assertEquals(3, viewModel.search.results.size)
    }

    @Test
    fun `applying with the count on its way searches as usual, only once`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val gateway = DelayedCourseGateway(fake, LATENCY)
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()  // el recuento está en camino
        viewModel.onApplyFilters()
        advanceUntilIdle()
        // El recuento se cancela (no llega) y la búsqueda normal se pide una vez.
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0, grafos to 0), fake.searches)
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.answered)
        assertEquals(grafos, viewModel.search.query)
        assertEquals(3, viewModel.search.results.size)
    }

    @Test
    fun `applying during the pause cancels the count and searches once`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0), gateway.searches)
        assertEquals(3, viewModel.search.results.size)
    }

    @Test
    fun `applying after a failed count searches as usual`() = runTest {
        var answer = failingWithTags()
        val gateway = FakeCourseGateway(searchAnswer = { query, page -> answer(query, page) })
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        answer = catalog()  // el servidor se recupera
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, grafos to 0, grafos to 0), gateway.searches)
        assertEquals(SearchStatus.Ready, viewModel.search.status)
        assertEquals(3, viewModel.search.total)
    }

    // --- Cerrar ---

    @Test
    fun `closing during the pause cancels the count`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        viewModel.onCloseFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `closing while the count is on its way cancels it`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val gateway = DelayedCourseGateway(fake, LATENCY)
        val viewModel = opened(gateway)
        viewModel.onToggleTag("grafos")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()
        assertEquals(grafos to 0, fake.searches.last())  // el recuento está en camino
        viewModel.onCloseFilters()
        advanceUntilIdle()
        assertNull(viewModel.filters)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.answered)
    }

    @Test
    fun `opening again after closing starts without a count`() = runTest {
        val viewModel = opened(FakeCourseGateway(searchAnswer = catalog()))
        viewModel.onToggleTag("grafos")
        advanceUntilIdle()
        assertEquals(DraftCount.Known(3), viewModel.draftCount())  // había un recuento
        viewModel.onCloseFilters()
        viewModel.onOpenFilters()
        assertNull(viewModel.filters?.preview)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    // --- Ayudantes ---

    // El catálogo entero responde; cualquier búsqueda con etiquetas falla (para el recuento).
    private fun failingWithTags(): (CourseQuery, Int) -> SearchOutcome {
        val ok = catalog()
        return { query, page -> if (query.tags.isEmpty()) ok(query, page) else SearchOutcome.ServerFailure }
    }

    private companion object {
        val LATENCY = 100.milliseconds
    }
}
