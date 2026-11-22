package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.search.ActiveFilter
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.course.search.SearchProblem
import es.ulpgc.pamn.dominio.course.search.SearchRequest
import es.ulpgc.pamn.dominio.course.search.SearchStatus
import es.ulpgc.pamn.dominio.data.demoSearchResults
import es.ulpgc.pamn.dominio.data.demoSearchState
import es.ulpgc.pamn.dominio.ui.components.spokenText
import es.ulpgc.pamn.dominio.ui.screens.search.SearchActions
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Qué enseña la pantalla Buscar en cada estado de CourseSearchState y qué avisa al tocarla.
// Los textos en sí (SearchTexts.kt) se prueban en el ordenador (SearchTextsTest). Se ejecuta en el
// emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenSearchTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val request = SearchRequest(CourseQuery.EMPTY, 0)
    private val loading = SearchStatus.Loading(request)
    private val loadingMore = SearchStatus.LoadingMore(SearchRequest(CourseQuery.EMPTY, 1))

    private fun show(
        search: CourseSearchState,
        text: String = "",
        actions: SearchActions = SearchActions(),
        filters: FilterSheetState? = null,
    ) = composeRule.setContent {
        GradusTheme { ScreenSearch(text = text, search = search, filters = filters, actions = actions) }
    }

    private fun card(index: Int) = composeRule.onNodeWithContentDescription(demoSearchResults[index].spokenText())

    private fun searching() = composeRule.onNodeWithContentDescription("Buscando cursos")

    private fun loadingMoreIndicator() = composeRule.onNodeWithContentDescription("Cargando más cursos")

    private val liveRegionPolite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    // --- Cabecera ---

    @Test
    fun the_title_and_the_search_field_are_always_there() {
        show(demoSearchState(loading, results = emptyList(), total = 0))

        composeRule.onNodeWithText("Buscar cursos").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
    }

    @Test
    fun the_search_field_shows_the_text_it_receives() {
        show(demoSearchState(), text = "grafos")

        composeRule.onNode(hasSetTextAction()).assertTextEquals("grafos")
    }

    @Test
    fun typing_reports_the_new_text() {
        val received = mutableListOf<String>()
        show(demoSearchState(), actions = SearchActions(onTextChange = { received += it }))

        composeRule.onNode(hasSetTextAction()).performTextInput("g")

        assertEquals(listOf("g"), received)
    }

    @Test
    fun the_search_key_calls_onSearch() {
        var searches = 0
        show(demoSearchState(), text = "grafos", actions = SearchActions(onSearch = { searches++ }))

        composeRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(1, searches)
    }

    // --- Fila de filtros (bajo la caja) ---

    private fun filtersApplied(vararg tags: String) =
        demoSearchState(query = CourseQuery(tags = tags.toList()))

    @Test
    fun without_filters_the_button_has_no_count_and_there_are_no_chips() {
        show(demoSearchState())

        composeRule.onNodeWithContentDescription("Filtros").assertIsDisplayed()
        composeRule.onAllNodes(hasContentDescription("activo", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasContentDescription("Quitar filtro", substring = true)).assertCountEquals(0)
    }

    @Test
    fun with_two_tags_the_button_reads_two_active_and_there_is_a_chip_for_each() {
        show(filtersApplied("grafos", "algoritmos"))

        composeRule.onNodeWithContentDescription("Filtros, 2 activos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Quitar filtro grafos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Quitar filtro algoritmos").assertIsDisplayed()
        composeRule.onAllNodes(hasContentDescription("Quitar filtro", substring = true)).assertCountEquals(2)
    }

    @Test
    fun with_one_tag_the_button_reads_one_active() {
        show(filtersApplied("grafos"))

        composeRule.onNodeWithContentDescription("Filtros, 1 activo").assertIsDisplayed()
    }

    @Test
    fun a_chip_shows_its_tag_with_a_hash() {
        show(filtersApplied("grafos"))

        // El texto suelto solo se ve en el árbol sin fusionar: el chip se lee entero («Quitar filtro…»).
        composeRule.onNodeWithText("#grafos", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun tapping_a_chip_reports_its_filter() {
        val removed = mutableListOf<ActiveFilter>()
        show(filtersApplied("grafos", "algoritmos"), actions = SearchActions(onRemoveFilter = { removed += it }))

        composeRule.onNodeWithContentDescription("Quitar filtro algoritmos").performClick()

        assertEquals(listOf<ActiveFilter>(ActiveFilter.Tag("algoritmos")), removed)
    }

    @Test
    fun tapping_the_filters_button_calls_onOpenFilters() {
        var opened = 0
        show(demoSearchState(), actions = SearchActions(onOpenFilters = { opened++ }))

        composeRule.onNodeWithContentDescription("Filtros").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun the_row_scrolls_sideways_when_the_chips_do_not_fit() {
        val removed = mutableListOf<ActiveFilter>()
        val tags = (1..12).map { "etiqueta$it" }
        show(filtersApplied(*tags.toTypedArray()), actions = SearchActions(onRemoveFilter = { removed += it }))

        // La última no cabe en pantalla: hay que desplazar la fila para llegar a ella.
        composeRule.onNodeWithContentDescription("Quitar filtro etiqueta12").performScrollTo().performClick()

        assertEquals(listOf<ActiveFilter>(ActiveFilter.Tag("etiqueta12")), removed)
    }

    // La fila ocupa todo el ancho de la pantalla y el margen de 20 dp es relleno interior: los chips
    // que no caben pasan por debajo del borde en lugar de cortarse 20 dp antes.
    private val horizontalScrollRow = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

    @Test
    fun the_row_spans_the_full_width_of_the_screen() {
        show(filtersApplied("grafos"))

        val row = composeRule.onNode(horizontalScrollRow).getBoundsInRoot()
        val root = composeRule.onRoot().getBoundsInRoot()

        assertEquals(0f, row.left.value, 0.5f)
        assertEquals(root.right.value, row.right.value, 0.5f)
    }

    @Test
    fun the_first_chip_keeps_the_screen_margin_as_inner_padding() {
        show(filtersApplied("grafos"))

        val button = composeRule.onNodeWithContentDescription("Filtros, 1 activo").getBoundsInRoot()

        assertEquals(20f, button.left.value, 0.5f)
    }

    @Test
    fun a_chip_that_does_not_fit_is_cut_by_the_screen_edge_and_not_by_the_margin() {
        show(filtersApplied(*(1..12).map { "etiqueta$it" }.toTypedArray()))

        val root = composeRule.onRoot().getBoundsInRoot()
        // `getBoundsInRoot` da los límites ya recortados por la fila: el chip que asoma por la derecha
        // llega justo hasta el borde de la pantalla (antes se cortaba 20 dp antes).
        val rightmost = (1..12).maxOf { n ->
            composeRule.onNodeWithContentDescription("Quitar filtro etiqueta$n").getBoundsInRoot().right.value
        }

        assertEquals(root.right.value, rightmost, 0.5f)
    }

    @Test
    fun at_the_end_of_the_row_the_last_chip_keeps_the_screen_margin() {
        show(filtersApplied(*(1..12).map { "etiqueta$it" }.toTypedArray()))

        composeRule.onNode(horizontalScrollRow).performTouchInput { repeat(8) { swipeLeft() } }

        val root = composeRule.onRoot().getBoundsInRoot()
        val last = composeRule.onNodeWithContentDescription("Quitar filtro etiqueta12").getBoundsInRoot()
        assertEquals(root.right.value - 20f, last.right.value, 1f)
    }

    @Test
    fun the_row_sits_between_the_search_field_and_the_results() {
        show(demoSearchState(hasMore = false))

        val field = composeRule.onNode(hasSetTextAction()).getBoundsInRoot()
        val button = composeRule.onNodeWithContentDescription("Filtros").getBoundsInRoot()
        val firstCard = card(0).getBoundsInRoot()

        assertTrue("debajo de la caja", button.top >= field.bottom)
        assertTrue("encima de los resultados", button.bottom <= firstCard.top)
    }

    @Test
    fun the_row_is_there_during_the_first_load() {
        show(demoSearchState(loading, results = emptyList(), total = 0))
        composeRule.onNodeWithContentDescription("Filtros").assertIsDisplayed()
    }

    @Test
    fun the_row_is_there_when_the_search_failed() {
        show(demoSearchState(SearchStatus.Failed(SearchProblem.UNREACHABLE), results = emptyList(), total = 0))
        composeRule.onNodeWithContentDescription("Filtros").assertIsDisplayed()
    }

    // --- Hoja de filtros (su contenido se prueba en FilterSheetTest) ---

    private val openSheet = FilterSheetState.open(CourseQuery.EMPTY, demoSearchResults)

    @Test
    fun without_filters_state_there_is_no_sheet() {
        show(demoSearchState(), filters = null)

        composeRule.onNodeWithText("Etiquetas").assertDoesNotExist()
    }

    @Test
    fun with_filters_state_the_sheet_is_shown_over_the_screen() {
        show(demoSearchState(), filters = openSheet)

        composeRule.onNodeWithText("Etiquetas").assertIsDisplayed()
    }

    @Test
    fun the_sheet_reports_through_the_screen_actions() {
        val toggled = mutableListOf<String>()
        show(demoSearchState(), filters = openSheet, actions = SearchActions(onToggleTag = { toggled += it }))

        composeRule.onNodeWithText("grafos").performClick()

        assertEquals(listOf("grafos"), toggled)
    }

    // --- Primera carga ---

    @Test
    fun the_first_load_shows_only_a_progress_indicator() {
        show(demoSearchState(loading, results = emptyList(), total = 0))

        searching().assertIsDisplayed()
        composeRule.onNodeWithText("resultados", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Cargar más").assertDoesNotExist()
        card(0).assertDoesNotExist()
    }

    // --- Carga con resultados (búsqueda nueva) ---

    @Test
    fun a_new_search_keeps_the_list_and_the_count_and_adds_a_progress_bar() {
        show(demoSearchState(loading))

        searching().assertIsDisplayed()
        composeRule.onNodeWithText("8 resultados").assertIsDisplayed()
        card(0).assertIsDisplayed()
        card(2).assertIsDisplayed()
    }

    // --- Listo ---

    @Test
    fun ready_shows_the_count_and_one_card_per_course() {
        show(demoSearchState())

        composeRule.onNodeWithText("8 resultados").assertIsDisplayed()
        demoSearchResults.indices.forEach { card(it).assertIsDisplayed() }
        searching().assertDoesNotExist()
        loadingMoreIndicator().assertDoesNotExist()
    }

    @Test
    fun ready_without_more_pages_has_no_load_more_button() {
        show(demoSearchState(hasMore = false))

        composeRule.onNodeWithText("Cargar más").assertDoesNotExist()
    }

    @Test
    fun the_count_is_a_polite_live_region() {
        show(demoSearchState())

        composeRule.onNodeWithText("8 resultados").assert(liveRegionPolite)
    }

    @Test
    fun the_count_is_singular_for_one_result() {
        show(demoSearchState(results = demoSearchResults.take(1), total = 1))

        composeRule.onNodeWithText("1 resultado").assertIsDisplayed()
    }

    @Test
    fun touching_a_card_reports_the_id_of_its_course() {
        val ids = mutableListOf<String>()
        show(demoSearchState(), actions = SearchActions(onCourseClick = { ids += it }))

        card(1).performClick()

        assertEquals(listOf(demoSearchResults[1].id), ids)
    }

    // --- Cargar más ---

    @Test
    fun load_more_appears_when_there_are_more_pages_and_calls_onLoadMore() {
        var loads = 0
        show(demoSearchState(hasMore = true), actions = SearchActions(onLoadMore = { loads++ }))

        composeRule.onNodeWithText("Cargar más").performClick()

        assertEquals(1, loads)
    }

    @Test
    fun load_more_is_at_least_48dp_high() {
        show(demoSearchState(hasMore = true))

        composeRule.onNodeWithText("Cargar más").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun loading_more_replaces_the_button_with_a_small_indicator() {
        show(demoSearchState(loadingMore, hasMore = true))

        loadingMoreIndicator().assertIsDisplayed()
        composeRule.onNodeWithText("Cargar más").assertDoesNotExist()
        card(0).assertIsDisplayed()
        composeRule.onNodeWithText("8 resultados").assertIsDisplayed()
    }

    @Test
    fun a_failed_load_more_keeps_the_list_and_offers_to_retry() {
        var retries = 0
        show(
            demoSearchState(SearchStatus.MoreFailed(SearchProblem.UNREACHABLE), hasMore = true),
            actions = SearchActions(onRetry = { retries++ }),
        )

        card(0).assertIsDisplayed()
        composeRule.onNodeWithText("8 resultados").assertIsDisplayed()
        composeRule.onNodeWithText("No se han podido cargar más cursos.").assertIsDisplayed()
        composeRule.onNodeWithText("Cargar más").assertDoesNotExist()

        composeRule.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }

    // --- Fallo de la primera página ---

    @Test
    fun a_failure_shows_the_problem_and_nothing_else() {
        show(demoSearchState(SearchStatus.Failed(SearchProblem.UNREACHABLE), results = emptyList(), total = 0))

        composeRule.onNodeWithText("Sin conexión").assertIsDisplayed()
        composeRule.onNodeWithText("resultados", substring = true).assertDoesNotExist()
        card(0).assertDoesNotExist()
        searching().assertDoesNotExist()
    }

    @Test
    fun a_server_failure_shows_its_own_text() {
        show(demoSearchState(SearchStatus.Failed(SearchProblem.SERVER_FAILURE), results = emptyList(), total = 0))

        composeRule.onNodeWithText("Algo ha fallado").assertIsDisplayed()
    }

    @Test
    fun a_failure_is_announced_as_a_polite_live_region() {
        show(demoSearchState(SearchStatus.Failed(SearchProblem.UNREACHABLE), results = emptyList(), total = 0))

        composeRule.onNode(liveRegionPolite and hasAnyDescendant(hasText("Sin conexión"))).assertExists()
    }

    @Test
    fun retry_after_a_failure_calls_onRetry_and_is_at_least_48dp_high() {
        var retries = 0
        show(
            demoSearchState(SearchStatus.Failed(SearchProblem.UNREACHABLE), results = emptyList(), total = 0),
            actions = SearchActions(onRetry = { retries++ }),
        )

        composeRule.onNodeWithText("Reintentar").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, retries)
    }

    // --- Posición de la lista ---

    // Cursos distintos para cada prueba: 12 tarjetas no caben en pantalla y hay que bajar.
    private fun courses(prefix: String, count: Int) =
        List(count) { demoSearchResults[0].copy(id = "$prefix-$it", title = "$prefix curso $it") }

    private fun searchOf(prefix: String, count: Int, query: CourseQuery) =
        demoSearchState(results = courses(prefix, count), total = count, query = query)

    private fun courseCard(prefix: String, index: Int) =
        composeRule.onNodeWithContentDescription("$prefix curso $index", substring = true)

    private fun resultsList() = composeRule.onNode(hasScrollToIndexAction())

    @Test
    fun a_new_query_starts_at_the_top_of_the_list() {
        var state by mutableStateOf(searchOf("a", 12, CourseQuery(text = "a")))
        composeRule.setContent { GradusTheme { ScreenSearch(text = "", search = state, filters = null, actions = SearchActions()) } }
        resultsList().performScrollToIndex(11)
        courseCard("a", 11).assertIsDisplayed()

        composeRule.runOnIdle { state = searchOf("b", 12, CourseQuery(text = "b")) }

        courseCard("b", 0).assertIsDisplayed()
    }

    @Test
    fun more_results_of_the_same_query_keep_the_position() {
        val query = CourseQuery(text = "a")
        var state by mutableStateOf(searchOf("a", 12, query))
        composeRule.setContent { GradusTheme { ScreenSearch(text = "", search = state, filters = null, actions = SearchActions()) } }
        resultsList().performScrollToIndex(11)

        // «Cargar más»: los mismos 12 cursos y 6 más al final.
        composeRule.runOnIdle { state = searchOf("a", 18, query) }

        courseCard("a", 11).assertIsDisplayed()
        courseCard("a", 0).assertDoesNotExist()
    }

    @Test
    fun the_position_survives_leaving_the_screen_and_coming_back() {
        // Volver de un curso destruye y vuelve a crear la pantalla: emulateSavedInstanceStateRestore
        // hace lo mismo (guarda el estado y lo restaura), con la misma consulta.
        val restoration = StateRestorationTester(composeRule)
        val state = searchOf("a", 12, CourseQuery(text = "a"))
        restoration.setContent { GradusTheme { ScreenSearch(text = "", search = state, filters = null, actions = SearchActions()) } }
        resultsList().performScrollToIndex(11)

        restoration.emulateSavedInstanceStateRestore()

        courseCard("a", 11).assertIsDisplayed()
        courseCard("a", 0).assertDoesNotExist()
    }

    // --- Barra de progreso ---

    @Test
    fun the_progress_bar_has_a_gap_above_it() {
        show(demoSearchState(loading))

        val field = composeRule.onNode(hasSetTextAction()).getBoundsInRoot()
        val bar = searching().getBoundsInRoot()

        // El nodo de la barra mide 24 dp (Material amplía su área) aunque la barra dibujada es de
        // 4 dp, centrada en él: su borde de arriba visible es el centro menos 2 dp.
        val visibleTop = (bar.top + bar.bottom) / 2 - 2.dp
        assertTrue("caja=$field barra=$bar", visibleTop - field.bottom >= 8.dp)
    }

    // --- Sin resultados ---

    @Test
    fun an_empty_search_says_nothing_matches_and_has_no_retry_button() {
        show(demoSearchState(results = emptyList(), total = 0, query = CourseQuery(text = "zzz")))

        composeRule.onNodeWithText("No hay cursos que coincidan").assertIsDisplayed()
        composeRule.onNodeWithText("Prueba con otras palabras.").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertDoesNotExist()
        composeRule.onNodeWithText("Cargar más").assertDoesNotExist()
    }

    @Test
    fun an_empty_catalog_says_there_are_no_published_courses() {
        show(demoSearchState(results = emptyList(), total = 0, query = CourseQuery.EMPTY))

        composeRule.onNodeWithText("Todavía no hay cursos publicados").assertIsDisplayed()
        composeRule.onNodeWithText("Prueba con otras palabras.").assertDoesNotExist()
    }

    @Test
    fun an_empty_result_is_announced_as_a_polite_live_region() {
        show(demoSearchState(results = emptyList(), total = 0, query = CourseQuery(text = "zzz")))

        composeRule.onNode(liveRegionPolite and hasAnyDescendant(hasText("No hay cursos que coincidan"))).assertExists()
    }

    @Test
    fun an_empty_result_has_no_cards_or_buttons() {
        show(demoSearchState(results = emptyList(), total = 0, query = CourseQuery(text = "zzz")))

        // La caja de búsqueda también responde al toque (para enfocarse) y el botón «Filtros» siempre
        // está en la fila de debajo: ninguno de los dos es de la zona de resultados, no cuentan.
        composeRule.onAllNodes(
            hasClickAction() and hasSetTextAction().not() and hasContentDescription("Filtros").not(),
        ).assertCountEquals(0)
    }
}
