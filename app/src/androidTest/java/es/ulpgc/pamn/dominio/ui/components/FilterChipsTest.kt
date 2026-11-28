package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tarea 7b-2 de buscar-cursos.md: lo que se ve y lo que lee TalkBack en los tres chips de los
// filtros. Se ejecutan en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class FilterChipsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = composeRule.setContentInSpanish { GradusTheme { content() } }

    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    // La zona táctil de 48 dp (Material la amplía sin agrandar el dibujo) no se ve en los límites del
    // nodo, así que se comprueba tocando fuera del dibujo pero dentro de los 48 dp: el centro del
    // chip mide 24 dp hasta el borde de esa zona, y los dibujos miden menos de 40 dp.
    private fun SemanticsNodeInteraction.tapAbove(distance: Dp) =
        performTouchInput { click(Offset(center.x, center.y - distance.toPx())) }

    private fun SemanticsNodeInteraction.tapLeftOf(distance: Dp) =
        performTouchInput { click(Offset(center.x - distance.toPx(), center.y)) }

    // ── FiltersButton («Filtros 2») ──

    @Test
    fun filters_button_without_filters_has_no_number_and_is_read_as_Filtros() {
        show { FiltersButton(count = 0, onClick = {}) }

        composeRule.onNodeWithContentDescription("Filtros").assertIsDisplayed()
        composeRule.onAllNodesWithText("0", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun filters_button_with_filters_shows_the_number() {
        show { FiltersButton(count = 2, onClick = {}) }

        composeRule.onNodeWithText("Filtros", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun filters_button_reads_the_number_of_active_filters() {
        show { FiltersButton(count = 2, onClick = {}) }

        composeRule.onNodeWithContentDescription("Filtros, 2 activos").assertIsDisplayed()
    }

    @Test
    fun filters_button_reads_one_active_filter_in_singular() {
        show { FiltersButton(count = 1, onClick = {}) }

        composeRule.onNodeWithContentDescription("Filtros, 1 activo").assertIsDisplayed()
    }

    @Test
    fun filters_button_does_not_read_the_loose_number() {
        show { FiltersButton(count = 2, onClick = {}) }

        // Con el árbol fusionado, que es el que lee TalkBack, el «2» no es un nodo aparte.
        composeRule.onAllNodesWithText("2").assertCountEquals(0)
    }

    @Test
    fun filters_button_is_a_button() {
        show { FiltersButton(count = 2, onClick = {}) }

        composeRule.onNodeWithContentDescription("Filtros, 2 activos").assert(role(Role.Button))
    }

    @Test
    fun clicking_the_filters_button_reports_it() {
        var clicks = 0
        show { FiltersButton(count = 2, onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Filtros, 2 activos").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun filters_button_can_be_tapped_in_a_48dp_area() {
        var clicks = 0
        show { FiltersButton(count = 0, onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Filtros").tapAbove(22.dp)

        assertEquals(1, clicks)
    }

    // ── RemovableFilterChip («#grafos ✕») ──

    @Test
    fun removable_chip_shows_its_label() {
        show { RemovableFilterChip(label = "#grafos", removeDescription = "Quitar filtro grafos", onRemove = {}) }

        composeRule.onNodeWithText("#grafos", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun removable_chip_is_read_as_a_button_that_removes_the_filter() {
        show { RemovableFilterChip(label = "#grafos", removeDescription = "Quitar filtro grafos", onRemove = {}) }

        composeRule.onNodeWithContentDescription("Quitar filtro grafos").assert(role(Role.Button))
    }

    @Test
    fun removable_chip_is_a_single_touch_target() {
        show { RemovableFilterChip(label = "#grafos", removeDescription = "Quitar filtro grafos", onRemove = {}) }

        // Todo el chip quita el filtro (decisión C): la ✕ no es un botón aparte.
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun clicking_the_removable_chip_reports_it_once() {
        var removed = 0
        show { RemovableFilterChip(label = "#grafos", removeDescription = "Quitar filtro grafos", onRemove = { removed++ }) }

        composeRule.onNodeWithContentDescription("Quitar filtro grafos").performClick()

        assertEquals(1, removed)
    }

    @Test
    fun removable_chip_can_be_tapped_in_a_48dp_area() {
        var removed = 0
        show { RemovableFilterChip(label = "#grafos", removeDescription = "Quitar filtro grafos", onRemove = { removed++ }) }

        composeRule.onNodeWithContentDescription("Quitar filtro grafos").tapAbove(22.dp)

        assertEquals(1, removed)
    }

    @Test
    fun removable_chip_ignores_a_tap_well_outside_its_48dp_area() {
        var removed = 0
        show { RemovableFilterChip(label = "#grafos", removeDescription = "Quitar filtro grafos", onRemove = { removed++ }) }

        composeRule.onNodeWithContentDescription("Quitar filtro grafos").tapAbove(30.dp)

        assertEquals(0, removed)
    }

    // ── SelectableTagChip («grafos», marcado o no) ──

    @Test
    fun a_selected_tag_chip_is_selected() {
        show { SelectableTagChip(tag = "grafos", selected = true, onToggle = {}) }

        composeRule.onNodeWithText("grafos").assertIsSelected()
    }

    @Test
    fun an_unselected_tag_chip_is_not_selected() {
        show { SelectableTagChip(tag = "grafos", selected = false, onToggle = {}) }

        composeRule.onNodeWithText("grafos").assertIsNotSelected()
    }

    @Test
    fun a_tag_chip_is_read_as_a_checkbox() {
        show { SelectableTagChip(tag = "grafos", selected = false, onToggle = {}) }

        composeRule.onNodeWithText("grafos").assert(role(Role.Checkbox))
    }

    @Test
    fun clicking_a_tag_chip_reports_it_once_whether_it_is_selected_or_not() {
        var toggles = 0
        show {
            SelectableTagChip(tag = "grafos", selected = false, onToggle = { toggles++ })
            SelectableTagChip(tag = "árboles", selected = true, onToggle = { toggles++ })
        }

        composeRule.onNodeWithText("grafos").performClick()
        assertEquals(1, toggles)
        composeRule.onNodeWithText("árboles").performClick()
        assertEquals(2, toggles)
    }

    @Test
    fun a_tag_chip_with_spaces_and_accents_shows_the_tag_as_it_is() {
        show { SelectableTagChip(tag = "Bases de datos", selected = false, onToggle = {}) }

        composeRule.onNodeWithText("Bases de datos").assertIsDisplayed()
    }

    @Test
    fun a_selected_tag_chip_can_be_tapped_in_a_48dp_area() {
        var toggles = 0
        show { SelectableTagChip(tag = "grafos", selected = true, onToggle = { toggles++ }) }

        composeRule.onNodeWithText("grafos").tapAbove(22.dp)

        assertEquals(1, toggles)
    }

    @Test
    fun a_short_unselected_tag_chip_can_be_tapped_in_a_48dp_area() {
        var toggles = 0
        show { SelectableTagChip(tag = "a", selected = false, onToggle = { toggles++ }) }

        // Mide unos 40 dp de ancho: 22 dp a la izquierda del centro queda fuera del dibujo.
        composeRule.onNodeWithText("a").tapLeftOf(22.dp)

        assertEquals(1, toggles)
    }
}
