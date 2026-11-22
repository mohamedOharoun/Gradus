package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.course.search.TagSuggestions
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tarea 7c-2 de buscar-cursos.md: la hoja de filtros (sección Etiquetas): qué enseña y cuándo avisa.
// El estado (qué etiquetas se ofrecen, qué se marca) se prueba en sharedLogic (FilterSheetStateTest);
// aquí solo lo que depende de Compose. Se ejecutan en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class FilterSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Un estado con las etiquetas que se quiera (`hidden` son las que esconde «Ver todas»).
    private fun state(
        shown: List<String> = listOf("grafos", "algoritmos", "árboles"),
        hidden: List<String> = emptyList(),
        selected: List<String> = emptyList(),
    ) = FilterSheetState(draft = CourseQuery(tags = selected), suggestions = TagSuggestions(shown, hidden))

    private fun show(state: FilterSheetState, actions: SearchActions = SearchActions()) =
        composeRule.setContent { GradusTheme { FilterSheet(state = state, actions = actions) } }

    private val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    // La zona táctil de 48 dp no se ve en los límites del nodo (ver FilterChipsTest): se toca fuera del dibujo.
    private fun SemanticsNodeInteraction.tapAbove(distance: Dp) =
        performTouchInput { click(Offset(center.x, center.y - distance.toPx())) }

    // Lo que tarda la hoja en esconderse: se espera a que se cumpla la condición en vez de dormir.
    private fun waitUntil(condition: () -> Boolean) = composeRule.waitUntil(timeoutMillis = 5_000, condition)

    // --- Qué se ve ---

    @Test
    fun the_title_is_a_heading_and_the_tags_section_has_its_hint() {
        show(state())

        composeRule.onNodeWithText("Filtros").assertIsDisplayed().assert(heading)
        composeRule.onNodeWithText("Etiquetas").assertIsDisplayed()
        composeRule.onNodeWithText("· elige una o varias").assertIsDisplayed()
    }

    @Test
    fun there_is_a_chip_for_each_shown_tag() {
        show(state(shown = listOf("grafos", "algoritmos", "árboles")))

        listOf("grafos", "algoritmos", "árboles").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun the_selected_tags_are_marked_and_the_others_are_not() {
        show(state(selected = listOf("grafos")))

        composeRule.onNodeWithText("grafos").assertIsSelected()
        composeRule.onNodeWithText("algoritmos").assertIsNotSelected()
    }

    @Test
    fun the_selection_is_compared_without_caring_about_capitals() {
        show(state(selected = listOf("GRAFOS")))

        composeRule.onNodeWithText("grafos").assertIsSelected()
    }

    @Test
    fun the_tag_chips_are_one_row_apart_with_no_extra_vertical_gap() {
        // Cada chip ya reserva 48 dp de alto para el toque (minimumInteractiveComponentSize), así que
        // el FlowRow no añade espacio vertical: si alguien lo sube, las filas dejarían de estar a 48 dp.
        // Las etiquetas son tan largas que solo cabe una por fila.
        val long = listOf("etiqueta de prueba muy larga 1", "etiqueta de prueba muy larga 2", "etiqueta de prueba muy larga 3")
        show(state(shown = long))

        val centers = long.map { tag ->
            val bounds = composeRule.onNodeWithText(tag).getBoundsInRoot()
            (bounds.top + bounds.bottom) / 2
        }

        assertEquals(48f, (centers[1] - centers[0]).value, 0.5f)
        assertEquals(48f, (centers[2] - centers[1]).value, 0.5f)
    }

    // --- Etiquetas: tocar, «Ver todas» y sin etiquetas ---

    @Test
    fun tapping_a_tag_reports_it() {
        val toggled = mutableListOf<String>()
        show(state(), SearchActions(onToggleTag = { toggled += it }))

        composeRule.onNodeWithText("algoritmos").performClick()

        assertEquals(listOf("algoritmos"), toggled)
    }

    @Test
    fun show_all_is_offered_when_there_are_hidden_tags_and_they_are_not_shown_yet() {
        show(state(hidden = listOf("kotlin", "android")))

        composeRule.onNodeWithText("Ver todas").assertIsDisplayed()
        composeRule.onNodeWithText("kotlin").assertDoesNotExist()
        composeRule.onNodeWithText("android").assertDoesNotExist()
    }

    @Test
    fun tapping_show_all_reports_it() {
        var shownAll = 0
        show(state(hidden = listOf("kotlin")), SearchActions(onShowAllTags = { shownAll++ }))

        composeRule.onNodeWithText("Ver todas").performClick()

        assertEquals(1, shownAll)
    }

    @Test
    fun after_show_all_the_hidden_tags_appear_and_the_button_is_gone() {
        show(state(hidden = listOf("kotlin", "android")).showAll())

        composeRule.onNodeWithText("kotlin").assertIsDisplayed()
        composeRule.onNodeWithText("android").assertIsDisplayed()
        composeRule.onNodeWithText("Ver todas").assertDoesNotExist()
    }

    @Test
    fun show_all_is_not_offered_when_nothing_is_hidden() {
        show(state(hidden = emptyList()))

        composeRule.onNodeWithText("Ver todas").assertDoesNotExist()
    }

    @Test
    fun show_all_is_read_as_a_button() {
        show(state(hidden = listOf("kotlin")))

        composeRule.onNodeWithText("Ver todas")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun show_all_can_be_tapped_in_a_48dp_area() {
        var shownAll = 0
        show(state(hidden = listOf("kotlin")), SearchActions(onShowAllTags = { shownAll++ }))

        composeRule.onNodeWithText("Ver todas").tapAbove(22.dp)

        assertEquals(1, shownAll)
    }

    @Test
    fun without_any_tag_it_says_there_are_none_to_choose_and_offers_no_show_all() {
        show(state(shown = emptyList(), hidden = emptyList()))

        composeRule.onNodeWithText("No hay etiquetas que elegir").assertIsDisplayed()
        composeRule.onNodeWithText("Ver todas").assertDoesNotExist()
    }

    @Test
    fun with_tags_it_does_not_say_there_are_none() {
        show(state())

        composeRule.onNodeWithText("No hay etiquetas que elegir").assertDoesNotExist()
    }

    // --- Restablecer ---

    @Test
    fun reset_is_disabled_without_selected_tags_and_does_not_report() {
        var resets = 0
        show(state(selected = emptyList()), SearchActions(onResetFilters = { resets++ }))

        composeRule.onNodeWithText("Restablecer").assertIsNotEnabled().performClick()

        assertEquals(0, resets)
    }

    @Test
    fun reset_is_enabled_with_selected_tags_and_reports() {
        var resets = 0
        show(state(selected = listOf("grafos")), SearchActions(onResetFilters = { resets++ }))

        composeRule.onNodeWithText("Restablecer").assertIsEnabled().performClick()

        assertEquals(1, resets)
    }

    @Test
    fun reset_is_at_least_48dp_high() {
        show(state(selected = listOf("grafos")))

        composeRule.onNodeWithText("Restablecer").assertHeightIsAtLeast(48.dp)
    }

    // --- Cerrar: ✕, «Mostrar cursos», «atrás» y deslizar ---

    @Test
    fun nothing_is_reported_just_by_showing_the_sheet() {
        var calls = 0
        show(
            state(),
            SearchActions(
                onApplyFilters = { calls++ }, onCloseFilters = { calls++ }, onToggleTag = { calls++ },
                onResetFilters = { calls++ }, onShowAllTags = { calls++ },
            ),
        )

        composeRule.onNodeWithText("Etiquetas").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(0, calls)
    }

    @Test
    fun show_courses_reports_onApplyFilters_and_not_onCloseFilters() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }))

        composeRule.onNodeWithText("Mostrar cursos").performClick()
        waitUntil { applied == 1 }

        assertEquals(1, applied)
        assertEquals(0, closed)
    }

    @Test
    fun the_close_button_reports_onCloseFilters_and_not_onApplyFilters() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }))

        composeRule.onNodeWithContentDescription("Cerrar filtros").performClick()
        waitUntil { closed == 1 }

        assertEquals(0, applied)
        assertEquals(1, closed)
    }

    @Test
    fun the_close_button_is_at_least_48dp() {
        show(state())

        composeRule.onNodeWithContentDescription("Cerrar filtros").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun show_courses_reports_only_after_the_hide_animation_not_at_once() {
        var applied = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }))
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("Mostrar cursos").performClick()
        assertEquals("la hoja aún se está escondiendo", 0, applied)

        composeRule.mainClock.autoAdvance = true
        waitUntil { applied == 1 }
        assertEquals(1, applied)
    }

    @Test
    fun the_close_button_also_waits_for_the_hide_animation() {
        var closed = 0
        show(state(), SearchActions(onCloseFilters = { closed++ }))
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription("Cerrar filtros").performClick()
        assertEquals("la hoja aún se está escondiendo", 0, closed)

        composeRule.mainClock.autoAdvance = true
        waitUntil { closed == 1 }
        assertEquals(1, closed)
    }

    @Test
    fun a_second_tap_on_show_courses_while_it_hides_does_not_report_again() {
        var applied = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }))
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("Mostrar cursos").performClick()
        composeRule.onNodeWithText("Mostrar cursos").performClick()

        composeRule.mainClock.autoAdvance = true
        waitUntil { applied >= 1 }
        composeRule.waitForIdle()
        assertEquals(1, applied)
    }

    @Test
    fun once_it_is_hiding_a_tap_on_the_other_button_is_ignored() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }))
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription("Cerrar filtros").performClick()
        composeRule.onNodeWithText("Mostrar cursos").performClick()

        composeRule.mainClock.autoAdvance = true
        waitUntil { closed >= 1 }
        composeRule.waitForIdle()
        assertEquals(1, closed)
        assertEquals(0, applied)
    }

    @Test
    fun back_while_the_sheet_is_hiding_after_show_courses_is_ignored() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }))
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("Mostrar cursos").performClick()
        Espresso.pressBack()

        composeRule.mainClock.autoAdvance = true
        waitUntil { applied >= 1 }
        composeRule.waitForIdle()
        assertEquals(1, applied)
        assertEquals(0, closed)
    }

    @Test
    fun back_reports_onCloseFilters() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }))
        composeRule.onNodeWithText("Etiquetas").assertIsDisplayed()

        Espresso.pressBack()
        waitUntil { closed == 1 }

        assertEquals(1, closed)
        assertEquals(0, applied)
    }

    @Test
    fun swiping_the_sheet_down_reports_onCloseFilters() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }))

        composeRule.onNodeWithText("Filtros").performTouchInput { swipeDown() }
        waitUntil { closed == 1 }

        assertEquals(1, closed)
        assertEquals(0, applied)
    }
}
