package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.search.ActiveFilter
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.PublishedWithin.LAST_MONTH
import es.ulpgc.pamn.dominio.course.search.PublishedWithin.LAST_YEAR
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.applyDraft
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.catalog
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.draftCount
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.opened
import es.ulpgc.pamn.dominio.ui.screens.search.SearchViewModel.Companion.TYPING_PAUSE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

// Creador y fecha en la hoja de filtros (tarea 11d-2 de buscar-cursos.md). Mismo reloj simulado que
// SearchViewModelPreviewTest: nada ocurre hasta runCurrent, advanceTimeBy o advanceUntilIdle.
//
// El catálogo de prueba no filtra por autor ni por fecha (el total es siempre 4): lo que se comprueba
// es QUÉ se pide (`gateway.searches`) y cuándo, que es lo que decide el ViewModel. Filtrar es cosa
// del servidor (y de FakeCourseGateway, probado en el 11b).
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelAuthorPublishedTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val initial = CourseQuery.EMPTY to 0
    private val ana = CourseQuery(author = "Ana")

    // --- Con la hoja cerrada ---

    @Test
    fun `changing the author with the sheet closed does nothing`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        viewModel.onAuthorChange("Ana")
        advanceUntilIdle()
        assertNull(viewModel.filters)
        assertEquals(listOf(initial), gateway.searches)
    }

    @Test
    fun `changing the date with the sheet closed does nothing`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        viewModel.onPublishedChange(LAST_MONTH)
        advanceUntilIdle()
        assertNull(viewModel.filters)
        assertEquals(listOf(initial), gateway.searches)
    }

    // --- Autor: la pausa y el recuento ---

    @Test
    fun `changing the author asks for nothing before the pause`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("Ana")
        advanceTimeBy(TYPING_PAUSE - 1.milliseconds)
        runCurrent()
        assertEquals(listOf(initial), gateway.searches)
        assertEquals(DraftCount.Counting, viewModel.draftCount())
    }

    @Test
    fun `changing the author counts the draft after the pause`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("Ana")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()
        assertEquals(listOf(initial, ana to 0), gateway.searches)
    }

    @Test
    fun `typing several letters counts once with the final text`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        listOf("A", "An", "Ana").forEach { typed ->
            viewModel.onAuthorChange(typed)
            advanceTimeBy(TYPING_PAUSE / 3)
        }
        advanceUntilIdle()
        assertEquals(listOf(initial, ana to 0), gateway.searches)
    }

    // El primer recuento ya está pedido cuando llega el cambio: se cancela y solo responde el último.
    @Test
    fun `changing the author while a count is on its way keeps only the last one`() = runTest {
        val fake = FakeCourseGateway(searchAnswer = catalog())
        val gateway = DelayedCourseGateway(fake, latency = LATENCY)
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("Ana")
        advanceTimeBy(TYPING_PAUSE)
        runCurrent()
        viewModel.onAuthorChange("Luis")
        advanceUntilIdle()
        val luis = CourseQuery(author = "Luis")
        assertEquals(listOf(initial, ana to 0, luis to 0), fake.searches)
        assertEquals(listOf(initial, luis to 0), gateway.answered)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    // Decisión B: el borrador guarda el autor tal como se escribe (el campo lo enseña así).
    @Test
    fun `the author keeps what was typed in the draft`() = runTest {
        val viewModel = opened(FakeCourseGateway(searchAnswer = catalog()))
        viewModel.onAuthorChange("  Ana")
        assertEquals("  Ana", viewModel.filters!!.draft.author)
    }

    // --- Fecha ---

    @Test
    fun `choosing a date counts the draft with it`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onPublishedChange(LAST_MONTH)
        advanceUntilIdle()
        assertEquals(listOf(initial, CourseQuery(published = LAST_MONTH) to 0), gateway.searches)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    @Test
    fun `choosing the date and the author counts both together`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("Ana")
        advanceTimeBy(TYPING_PAUSE / 3)
        viewModel.onPublishedChange(LAST_MONTH)
        advanceUntilIdle()
        assertEquals(listOf(initial, ana.copy(published = LAST_MONTH) to 0), gateway.searches)
    }

    // --- Volver a lo aplicado (decisión B) ---

    @Test
    fun `going back to the applied author does not count`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("Ana")
        assertEquals(DraftCount.Counting, viewModel.draftCount())
        advanceTimeBy(TYPING_PAUSE / 3)
        viewModel.onAuthorChange("")
        advanceUntilIdle()
        assertEquals(listOf(initial), gateway.searches)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    @Test
    fun `an author with only spaces does not count`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("   ")
        advanceUntilIdle()
        assertEquals(listOf(initial), gateway.searches)
        assertEquals(DraftCount.Known(4), viewModel.draftCount())
    }

    @Test
    fun `an author that differs only in case or spaces from the applied one does not count`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        applyDraft(viewModel) { onAuthorChange("Ana") }
        viewModel.onOpenFilters()
        viewModel.onAuthorChange(" ana ")
        advanceUntilIdle()
        assertEquals(listOf(initial, ana to 0), gateway.searches)
    }

    @Test
    fun `choosing the applied date again does not count`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        applyDraft(viewModel) { onPublishedChange(LAST_YEAR) }
        viewModel.onOpenFilters()
        viewModel.onPublishedChange(LAST_YEAR)
        advanceUntilIdle()
        assertEquals(listOf(initial, CourseQuery(published = LAST_YEAR) to 0), gateway.searches)
    }

    // --- Aplicar ---

    // Sin esperar la pausa: una sola búsqueda con los dos, y el autor tal como se tecleó.
    @Test
    fun `applying asks for the author and the date`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        applyDraft(viewModel) {
            onAuthorChange("Ana ")
            onPublishedChange(LAST_YEAR)
        }
        advanceUntilIdle()
        val applied = CourseQuery(author = "Ana ", published = LAST_YEAR)
        assertEquals(listOf(initial, applied to 0), gateway.searches)
        assertEquals(applied, viewModel.search.query)
    }

    @Test
    fun `applying after the count reuses its results`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("Ana")
        viewModel.onPublishedChange(LAST_MONTH)
        advanceUntilIdle()
        val counted = viewModel.filters!!.preview
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertEquals(listOf(initial, ana.copy(published = LAST_MONTH) to 0), gateway.searches)
        assertSame(counted, viewModel.search)
    }

    // Pide lo mismo que lo aplicado: no se busca y se queda el autor aplicado, no el tecleado.
    @Test
    fun `applying an author that differs only in case does not search again`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        advanceUntilIdle()
        applyDraft(viewModel) { onAuthorChange("Ana") }
        applyDraft(viewModel) { onAuthorChange(" ana ") }
        advanceUntilIdle()
        assertEquals(listOf(initial, ana to 0), gateway.searches)
        assertEquals("Ana", viewModel.search.query.author)
    }

    @Test
    fun `reopening the sheet shows the applied author and date`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        advanceUntilIdle()
        applyDraft(viewModel) {
            onAuthorChange("Ana")
            onPublishedChange(LAST_YEAR)
        }
        viewModel.onOpenFilters()
        assertEquals(ana.copy(published = LAST_YEAR), viewModel.filters!!.draft)
    }

    // --- Cerrar y «Restablecer» ---

    // ✕ antes de la pausa: el borrador se descarta y su recuento no llega a pedirse.
    @Test
    fun `closing discards the author and date of the draft`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = opened(gateway)
        viewModel.onAuthorChange("Ana")
        viewModel.onPublishedChange(LAST_MONTH)
        viewModel.onCloseFilters()
        advanceUntilIdle()
        assertEquals(listOf(initial), gateway.searches)
        viewModel.onOpenFilters()
        assertEquals(CourseQuery.EMPTY, viewModel.filters!!.draft)
    }

    // Decisión D: vacía creador y fecha y conserva el texto; como pide algo nuevo, cuenta.
    @Test
    fun `reset empties the author and the date and keeps the text`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        viewModel.onTextChange("grafos")
        advanceUntilIdle()
        applyDraft(viewModel) {
            onAuthorChange("Ana")
            onPublishedChange(LAST_YEAR)
        }
        viewModel.onOpenFilters()
        viewModel.onResetFilters()
        val onlyText = CourseQuery(text = "grafos")
        assertEquals(onlyText, viewModel.filters!!.draft)
        advanceUntilIdle()
        assertEquals(onlyText to 0, gateway.searches.last())
    }

    // --- actions() ---

    @Test
    fun `actions pass the author change to the view model`() = runTest {
        val viewModel = opened(FakeCourseGateway(searchAnswer = catalog()))
        viewModel.actions {}.onAuthorChange("Ana")
        assertEquals("Ana", viewModel.filters!!.draft.author)
    }

    @Test
    fun `actions pass the date change to the view model`() = runTest {
        val viewModel = opened(FakeCourseGateway(searchAnswer = catalog()))
        viewModel.actions {}.onPublishedChange(LAST_MONTH)
        assertEquals(LAST_MONTH, viewModel.filters!!.draft.published)
    }

    // --- Quitar chips con creador y fecha aplicados (pendiente del 11a-2) ---

    @Test
    fun `removing the author chip keeps the tags the text and the date`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = withEverythingApplied(gateway)
        viewModel.onRemoveFilter(ActiveFilter.Author("Ana"))
        runCurrent()
        val expected = CourseQuery(text = "grafos", tags = listOf("grafos"), published = LAST_YEAR)
        assertEquals(expected, viewModel.search.query)
        assertEquals(expected to 0, gateway.searches.last())
    }

    @Test
    fun `removing the date chip keeps the tags the text and the author`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = withEverythingApplied(gateway)
        viewModel.onRemoveFilter(ActiveFilter.Published(LAST_YEAR))
        runCurrent()
        val expected = CourseQuery(text = "grafos", tags = listOf("grafos"), author = "Ana")
        assertEquals(expected, viewModel.search.query)
        assertEquals(expected to 0, gateway.searches.last())
    }

    // --- Ayudantes ---

    // Texto, una etiqueta, creador y fecha, todo aplicado.
    private fun TestScope.withEverythingApplied(gateway: CourseGateway): SearchViewModel {
        val viewModel = SearchViewModel(gateway)
        viewModel.onTextChange("grafos")
        advanceUntilIdle()
        applyDraft(viewModel) {
            onToggleTag("grafos")
            onAuthorChange("Ana")
            onPublishedChange(LAST_YEAR)
        }
        advanceUntilIdle()
        assertEquals(
            CourseQuery(text = "grafos", tags = listOf("grafos"), author = "Ana", published = LAST_YEAR),
            viewModel.search.query,
        )
        return viewModel
    }

    private companion object {
        val LATENCY = 100.milliseconds
    }
}
