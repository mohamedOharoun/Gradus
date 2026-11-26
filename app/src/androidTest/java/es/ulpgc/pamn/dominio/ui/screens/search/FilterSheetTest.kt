package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import es.ulpgc.pamn.dominio.course.search.TagSuggestions
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
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

    // Sin `count`, el botón dice «Mostrar cursos» (sin número), como los tests de antes del 8b.
    private fun show(
        state: FilterSheetState,
        actions: SearchActions = SearchActions(),
        count: DraftCount = DraftCount.Counting,
    ) = composeRule.setContentInSpanish { GradusTheme { FilterSheet(state = state, count = count, actions = actions) } }

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

    // --- El botón «Mostrar N cursos» (tarea 8b) ---
    // Los textos exactos se prueban en FilterTextsTest; aquí, que el botón los enseña y cuándo se puede pulsar.

    @Test
    fun the_button_shows_the_number_of_courses_of_the_draft() {
        show(state(), count = DraftCount.Known(8))

        composeRule.onNodeWithText("Mostrar 8 cursos").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun the_button_is_singular_with_one_course() {
        show(state(), count = DraftCount.Known(1))

        composeRule.onNodeWithText("Mostrar 1 curso").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun the_button_has_no_number_while_counting_and_can_be_tapped() {
        show(state(), count = DraftCount.Counting)

        composeRule.onNodeWithText("Mostrar cursos").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun the_button_has_no_number_when_the_count_is_unknown_and_can_be_tapped() {
        show(state(), count = DraftCount.Unknown)

        composeRule.onNodeWithText("Mostrar cursos").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun with_no_courses_the_button_says_none_match_and_is_disabled() {
        show(state(), count = DraftCount.Known(0))

        composeRule.onNodeWithText("Ningún curso coincide").assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText("Mostrar cursos").assertDoesNotExist()
    }

    @Test
    fun tapping_the_disabled_button_reports_nothing() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }), DraftCount.Known(0))

        composeRule.onNodeWithText("Ningún curso coincide").performClick()
        composeRule.waitForIdle()

        assertEquals(0, applied)
        assertEquals(0, closed)
    }

    @Test
    fun the_button_with_a_number_reports_onApplyFilters() {
        var applied = 0
        var closed = 0
        show(state(), SearchActions(onApplyFilters = { applied++ }, onCloseFilters = { closed++ }), DraftCount.Known(8))

        composeRule.onNodeWithText("Mostrar 8 cursos").performClick()
        waitUntil { applied == 1 }

        assertEquals(1, applied)
        assertEquals(0, closed)
    }

    // Decisión C: con 0 resultados se puede seguir cambiando el borrador y cerrar la hoja.
    @Test
    fun with_no_courses_reset_still_works() {
        var resets = 0
        show(state(selected = listOf("grafos")), SearchActions(onResetFilters = { resets++ }), DraftCount.Known(0))

        composeRule.onNodeWithText("Restablecer").assertIsEnabled().performClick()

        assertEquals(1, resets)
    }

    @Test
    fun with_no_courses_the_tags_can_still_be_toggled() {
        val toggled = mutableListOf<String>()
        show(state(), SearchActions(onToggleTag = { toggled += it }), DraftCount.Known(0))

        composeRule.onNodeWithText("algoritmos").performClick()

        assertEquals(listOf("algoritmos"), toggled)
    }

    @Test
    fun with_no_courses_the_close_button_still_reports_onCloseFilters() {
        var closed = 0
        show(state(), SearchActions(onCloseFilters = { closed++ }), DraftCount.Known(0))

        composeRule.onNodeWithContentDescription("Cerrar filtros").performClick()
        waitUntil { closed == 1 }

        assertEquals(1, closed)
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

    // --- Creador y fecha de publicación (tarea 11d-1) ---
    // El borrador con creador y fecha; las pruebas de los componentes sueltos están en CreatorFieldTest y
    // SingleChoiceRowTest, y aquí solo lo que pone la hoja: títulos, orden, qué se ve y a quién se avisa.

    private fun stateWith(
        author: String = "",
        published: PublishedWithin = PublishedWithin.ANY,
        selected: List<String> = emptyList(),
    ) = FilterSheetState(
        draft = CourseQuery(tags = selected, author = author, published = published),
        suggestions = TagSuggestions(listOf("grafos", "algoritmos", "árboles"), emptyList()),
    )

    private fun creatorField() = composeRule.onNode(hasSetTextAction())

    private fun centerY(text: String): Dp {
        val bounds = composeRule.onNodeWithText(text).getBoundsInRoot()
        return (bounds.top + bounds.bottom) / 2
    }

    @Test
    fun the_creator_and_published_titles_are_headings() {
        show(stateWith())

        composeRule.onNodeWithText("Creador").assertIsDisplayed().assert(heading)
        composeRule.onNodeWithText("Fecha de publicación").assertIsDisplayed().assert(heading)
    }

    @Test
    fun the_tags_title_is_still_a_heading() {
        show(stateWith())

        composeRule.onNodeWithText("Etiquetas").assert(heading)
    }

    @Test
    fun the_sections_go_in_order_tags_then_creator_then_published() {
        show(stateWith())

        assertTrue(centerY("Etiquetas") < centerY("Creador"))
        assertTrue(centerY("Creador") < centerY("Fecha de publicación"))
    }

    @Test
    fun the_published_choices_are_the_three_of_the_design() {
        show(stateWith())

        listOf("Cualquiera", "Último mes", "Último año").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun the_creator_field_shows_the_author_of_the_draft() {
        show(stateWith(author = "Ana Ruiz"))

        creatorField().assertTextEquals("Ana Ruiz")
    }

    @Test
    fun the_creator_field_shows_its_hint_without_an_author() {
        show(stateWith(author = ""))

        composeRule.onNodeWithText("Nombre del creador").assertIsDisplayed()
    }

    @Test
    fun typing_in_the_creator_field_reports_it_to_onAuthorChange() {
        val received = mutableListOf<String>()
        show(stateWith(), SearchActions(onAuthorChange = { received += it }))

        creatorField().performTextInput("Ana")

        assertEquals(listOf("Ana"), received)
    }

    @Test
    fun the_clear_button_of_the_creator_field_reports_an_empty_author() {
        val received = mutableListOf<String>()
        show(stateWith(author = "Ana"), SearchActions(onAuthorChange = { received += it }))

        composeRule.onNodeWithContentDescription("Borrar creador").performClick()

        assertEquals(listOf(""), received)
    }

    @Test
    fun the_published_of_the_draft_is_the_one_selected() {
        show(stateWith(published = PublishedWithin.LAST_YEAR))

        composeRule.onNodeWithText("Último año").assertIsSelected()
        composeRule.onNodeWithText("Último mes").assertIsNotSelected()
        composeRule.onNodeWithText("Cualquiera").assertIsNotSelected()
    }

    @Test
    fun without_a_published_choice_any_is_the_one_selected() {
        show(stateWith())

        composeRule.onNodeWithText("Cualquiera").assertIsSelected()
        composeRule.onNodeWithText("Último mes").assertIsNotSelected()
        composeRule.onNodeWithText("Último año").assertIsNotSelected()
    }

    @Test
    fun tapping_last_month_reports_it_to_onPublishedChange() {
        val received = mutableListOf<PublishedWithin>()
        show(stateWith(), SearchActions(onPublishedChange = { received += it }))

        composeRule.onNodeWithText("Último mes").performClick()

        assertEquals(listOf(PublishedWithin.LAST_MONTH), received)
    }

    @Test
    fun tapping_the_other_choices_reports_them_too() {
        val received = mutableListOf<PublishedWithin>()
        show(stateWith(published = PublishedWithin.LAST_MONTH), SearchActions(onPublishedChange = { received += it }))

        composeRule.onNodeWithText("Cualquiera").performClick()
        composeRule.onNodeWithText("Último año").performClick()

        assertEquals(listOf(PublishedWithin.ANY, PublishedWithin.LAST_YEAR), received)
    }

    @Test
    fun reset_is_enabled_with_only_an_author() {
        var resets = 0
        show(stateWith(author = "Ana"), SearchActions(onResetFilters = { resets++ }))

        composeRule.onNodeWithText("Restablecer").assertIsEnabled().performClick()

        assertEquals(1, resets)
    }

    @Test
    fun reset_is_enabled_with_only_a_published_choice() {
        var resets = 0
        show(stateWith(published = PublishedWithin.LAST_MONTH), SearchActions(onResetFilters = { resets++ }))

        composeRule.onNodeWithText("Restablecer").assertIsEnabled().performClick()

        assertEquals(1, resets)
    }

    @Test
    fun reset_is_disabled_when_the_draft_has_no_tags_no_author_and_any_date() {
        show(stateWith())

        composeRule.onNodeWithText("Restablecer").assertIsNotEnabled()
    }

    @Test
    fun an_author_made_of_spaces_does_not_enable_reset() {
        // El borrador guarda el creador tal como se escribe, pero en blanco no filtra (decisión B).
        show(stateWith(author = "   "))

        composeRule.onNodeWithText("Restablecer").assertIsNotEnabled()
    }

    @Test
    fun the_new_actions_are_not_reported_just_by_showing_the_sheet() {
        var calls = 0
        show(stateWith(author = "Ana", published = PublishedWithin.LAST_YEAR), SearchActions(onAuthorChange = { calls++ }, onPublishedChange = { calls++ }))

        composeRule.onNodeWithText("Creador").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(0, calls)
    }

    // Decisión C: con 0 resultados se puede seguir cambiando el borrador.
    @Test
    fun with_no_courses_the_creator_can_still_be_typed() {
        val authors = mutableListOf<String>()
        show(stateWith(), SearchActions(onAuthorChange = { authors += it }), DraftCount.Known(0))

        creatorField().performTextInput("Ana")

        assertEquals(listOf("Ana"), authors)
    }

    @Test
    fun with_no_courses_the_published_choice_can_still_be_changed() {
        val published = mutableListOf<PublishedWithin>()
        show(stateWith(), SearchActions(onPublishedChange = { published += it }), DraftCount.Known(0))

        composeRule.onNodeWithText("Último año").performClick()

        assertEquals(listOf(PublishedWithin.LAST_YEAR), published)
    }

    @Test
    fun the_three_sections_scroll_together_while_the_button_stays_in_view() {
        // Tantas etiquetas largas que la hoja no cabe: el contenido se desplaza y el botón no.
        val many = (1..60).map { "etiqueta de prueba muy larga $it" }
        show(
            FilterSheetState(draft = CourseQuery(), suggestions = TagSuggestions(many, emptyList())),
            count = DraftCount.Known(8),
        )
        composeRule.onNodeWithText("Mostrar 8 cursos").assertIsDisplayed()
        composeRule.onNodeWithText("Último año").assertIsNotDisplayed()

        composeRule.onNodeWithText("Último año").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Mostrar 8 cursos").assertIsDisplayed()
    }

    // Las secciones aceptan un `modifier` (AGENTS.md) y lo aplican a la sección entera, no solo a un elemento.
    private fun showSection(section: @Composable (Modifier) -> Unit) =
        composeRule.setContentInSpanish { GradusTheme { section(Modifier.width(200.dp).testTag("section")) } }

    @Test
    fun the_tags_section_applies_the_modifier_it_receives() {
        showSection { TagsSection(stateWith(), SearchActions(), it) }

        composeRule.onNodeWithTag("section").assertWidthIsEqualTo(200.dp)
    }

    @Test
    fun the_creator_section_applies_the_modifier_it_receives() {
        showSection { CreatorSection(stateWith(), SearchActions(), it) }

        composeRule.onNodeWithTag("section").assertWidthIsEqualTo(200.dp)
    }

    @Test
    fun the_published_section_applies_the_modifier_it_receives() {
        showSection { PublishedSection(stateWith(), SearchActions(), it) }

        composeRule.onNodeWithTag("section").assertWidthIsEqualTo(200.dp)
    }
}
