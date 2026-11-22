package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.search.ActiveFilter
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.applyTags
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.catalog
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.catalogOf
import es.ulpgc.pamn.dominio.ui.screens.search.SearchTestCatalog.course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// La hoja de filtros en SearchViewModel (tarea 7d). Va aparte de SearchViewModelTest para que
// ningún fichero de tests sea demasiado largo. Mismo reloj simulado: nada ocurre hasta runCurrent,
// advanceTimeBy o advanceUntilIdle. El recuento de «Mostrar N cursos» (tarea 8c) se prueba en
// SearchViewModelPreviewTest; el catálogo de los dos está en SearchTestCatalog.
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelFiltersTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Abrir la hoja ---

    @Test
    fun `starts with the sheet closed`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        assertNull(viewModel.filters)
    }

    @Test
    fun `opening suggests the tags of the loaded results, most repeated first`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onOpenFilters()
        assertEquals(listOf("grafos", "algoritmos", "redes", "árboles"), viewModel.filters?.visibleTags)
    }

    @Test
    fun `opening starts the draft from the applied query`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onSearch()
        runCurrent()
        viewModel.onOpenFilters()
        assertEquals(CourseQuery(text = "grafos"), viewModel.filters?.draft)
    }

    @Test
    fun `opening with tags applied shows them first and selected`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        applyTags(viewModel, "redes")
        viewModel.onOpenFilters()
        // Con «redes» aplicada solo quedan grafos-2 y redes-1: «grafos» es la única otra etiqueta.
        assertEquals(listOf("redes", "grafos"), viewModel.filters?.visibleTags)
        assertTrue(viewModel.filters!!.isSelected("redes"))
    }

    @Test
    fun `opening does not search`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onOpenFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `opening again while the sheet is open keeps the draft`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        viewModel.onOpenFilters()  // doble toque rápido en «Filtros»
        assertTrue(viewModel.filters!!.isSelected("grafos"))
    }

    // --- El borrador ---

    // Sin pedir nada al momento: el recuento del borrador espera la pausa (SearchViewModelPreviewTest).
    @Test
    fun `toggling a tag marks it in the draft without searching at once`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        runCurrent()
        assertTrue(viewModel.filters!!.isSelected("grafos"))
        assertEquals(CourseQuery.EMPTY, viewModel.search.query)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `toggling a marked tag unmarks it`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        viewModel.onToggleTag("grafos")
        assertFalse(viewModel.filters!!.isSelected("grafos"))
    }

    @Test
    fun `reset empties the tags of the draft without searching`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        viewModel.onToggleTag("redes")
        viewModel.onResetFilters()
        advanceUntilIdle()
        assertEquals(emptyList<String>(), viewModel.filters?.draft?.tags)
        assertFalse(viewModel.filters!!.canReset)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `show all shows every suggestion without searching`() = runTest {
        val manyTags = (1..9).map { "etiqueta$it" }.toTypedArray()
        val gateway = FakeCourseGateway(searchAnswer = catalogOf(course("muchas", *manyTags)))
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onOpenFilters()
        assertTrue(viewModel.filters!!.canShowAll)
        viewModel.onShowAllTags()
        advanceUntilIdle()
        assertEquals(9, viewModel.filters?.visibleTags?.size)
        assertFalse(viewModel.filters!!.canShowAll)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `toggle, reset and show all do nothing with the sheet closed`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onToggleTag("grafos")
        viewModel.onResetFilters()
        viewModel.onShowAllTags()
        advanceUntilIdle()
        assertNull(viewModel.filters)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    // --- Aplicar («Mostrar cursos») ---

    @Test
    fun `applying searches once with the tags of the draft`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        viewModel.onToggleTag("algoritmos")
        viewModel.onApplyFilters()
        advanceUntilIdle()
        val applied = CourseQuery(tags = listOf("grafos", "algoritmos"))
        assertEquals(listOf(CourseQuery.EMPTY to 0, applied to 0), gateway.searches)
        assertEquals(applied, viewModel.search.query)
    }

    @Test
    fun `after applying the results and the active filters arrive`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        applyTags(viewModel, "grafos", "algoritmos")
        assertEquals(listOf("grafos-1", "arboles-1"), viewModel.search.results.map(CourseSummary::id))
        assertEquals(2, viewModel.search.query.activeFilters.size)  // «Filtros 2»
    }

    @Test
    fun `applying closes the sheet`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        viewModel.onApplyFilters()
        assertNull(viewModel.filters)
    }

    @Test
    fun `applying uses the text in the box, even before the typing wait ends`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onOpenFilters()
        viewModel.onToggleTag("redes")
        viewModel.onApplyFilters()
        runCurrent()
        assertEquals(CourseQuery(text = "grafos", tags = listOf("redes")) to 0, gateway.searches.last())
        assertEquals(0L, currentTime)
    }

    @Test
    fun `applying cancels the pending typing wait`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onOpenFilters()
        viewModel.onToggleTag("redes")
        viewModel.onApplyFilters()
        advanceUntilIdle()
        val applied = CourseQuery(text = "grafos", tags = listOf("redes"))
        assertEquals(listOf(CourseQuery.EMPTY to 0, applied to 0), gateway.searches)
        assertEquals(applied, viewModel.search.query)
    }

    @Test
    fun `applying without changes closes the sheet without searching`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertNull(viewModel.filters)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `applying without changes also cancels the pending typing wait`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        // « » pide lo mismo que la búsqueda vacía: la espera solo la repetiría.
        viewModel.onTextChange(" ")
        viewModel.onOpenFilters()
        viewModel.onApplyFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `applying with the sheet closed does nothing`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onApplyFilters()
        advanceUntilIdle()
        // Ni busca ni cancela la espera: la búsqueda tecleada llega igual.
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery(text = "grafos") to 0), gateway.searches)
    }

    // --- Cerrar (✕, deslizar, tocar fuera o «atrás») ---

    @Test
    fun `closing discards the draft without searching`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        viewModel.onCloseFilters()
        advanceUntilIdle()
        assertNull(viewModel.filters)
        assertEquals(CourseQuery.EMPTY, viewModel.search.query)
        assertEquals(listOf(CourseQuery.EMPTY to 0), gateway.searches)
    }

    @Test
    fun `opening after closing starts again from the applied query`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        viewModel.onOpenFilters()
        viewModel.onToggleTag("grafos")
        viewModel.onCloseFilters()
        viewModel.onOpenFilters()
        assertNotNull(viewModel.filters)
        assertFalse(viewModel.filters!!.isSelected("grafos"))
    }

    @Test
    fun `closing does not cancel the pending typing wait`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        viewModel.onTextChange("grafos")
        viewModel.onOpenFilters()
        viewModel.onCloseFilters()
        advanceUntilIdle()
        assertEquals(listOf(CourseQuery.EMPTY to 0, CourseQuery(text = "grafos") to 0), gateway.searches)
    }

    // --- Quitar un chip de la fila ---

    @Test
    fun `removing a chip searches at once with the other tags`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        applyTags(viewModel, "grafos", "algoritmos")
        val before = currentTime
        viewModel.onRemoveFilter(ActiveFilter.Tag("grafos"))
        runCurrent()
        val remaining = CourseQuery(tags = listOf("algoritmos"))
        assertEquals(remaining to 0, gateway.searches.last())
        assertEquals(remaining, viewModel.search.query)
        assertEquals(before, currentTime)
    }

    @Test
    fun `removing a chip uses the text in the box`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        applyTags(viewModel, "redes")
        viewModel.onTextChange("grafos")
        viewModel.onRemoveFilter(ActiveFilter.Tag("redes"))
        runCurrent()
        assertEquals(CourseQuery(text = "grafos") to 0, gateway.searches.last())
    }

    @Test
    fun `removing a chip cancels the pending typing wait`() = runTest {
        val gateway = FakeCourseGateway(searchAnswer = catalog())
        val viewModel = SearchViewModel(gateway)
        runCurrent()
        applyTags(viewModel, "redes")
        viewModel.onTextChange("grafos")
        viewModel.onRemoveFilter(ActiveFilter.Tag("redes"))
        advanceUntilIdle()
        val expected = listOf(
            CourseQuery.EMPTY to 0,
            CourseQuery(tags = listOf("redes")) to 0,
            CourseQuery(text = "grafos") to 0,
        )
        assertEquals(expected, gateway.searches)
    }

    @Test
    fun `removing the last chip shows the whole catalog again`() = runTest {
        val viewModel = SearchViewModel(FakeCourseGateway(searchAnswer = catalog()))
        runCurrent()
        applyTags(viewModel, "redes")
        viewModel.onRemoveFilter(ActiveFilter.Tag("redes"))
        runCurrent()
        assertEquals(emptyList<ActiveFilter>(), viewModel.search.query.activeFilters)
        assertEquals(4, viewModel.search.results.size)
    }
}
