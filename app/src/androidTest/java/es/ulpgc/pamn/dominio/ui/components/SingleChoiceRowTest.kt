package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import es.ulpgc.pamn.dominio.ui.screens.search.publishedLabel
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tarea 11c de buscar-cursos.md: las opciones de «Fecha de publicación» (y de lo que venga después).
// Se ejecutan en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class SingleChoiceRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private enum class Size { SMALL, LARGE }

    private fun showDates(
        selected: PublishedWithin = PublishedWithin.ANY,
        onSelect: (PublishedWithin) -> Unit = {},
    ) = composeRule.setContent {
        GradusTheme {
            SingleChoiceRow(PublishedWithin.entries, selected, ::publishedLabel, onSelect)
        }
    }

    private fun option(within: PublishedWithin) = composeRule.onNodeWithText(publishedLabel(within))

    @Test
    fun every_option_is_shown() {
        showDates()

        PublishedWithin.entries.forEach { option(it).assertIsDisplayed() }
    }

    @Test
    fun the_selected_option_is_selected_and_the_others_are_not() {
        showDates(selected = PublishedWithin.LAST_MONTH)

        option(PublishedWithin.LAST_MONTH).assertIsSelected()
        option(PublishedWithin.ANY).assertIsNotSelected()
        option(PublishedWithin.LAST_YEAR).assertIsNotSelected()
    }

    @Test
    fun clicking_an_option_reports_it() {
        val received = mutableListOf<PublishedWithin>()
        showDates(onSelect = { received += it })

        option(PublishedWithin.LAST_YEAR).performClick()

        assertEquals(listOf(PublishedWithin.LAST_YEAR), received)
    }

    @Test
    fun clicking_the_already_selected_option_reports_it_too() {
        val received = mutableListOf<PublishedWithin>()
        showDates(selected = PublishedWithin.ANY, onSelect = { received += it })

        option(PublishedWithin.ANY).performClick()

        assertEquals(listOf(PublishedWithin.ANY), received)
    }

    @Test
    fun it_does_not_change_the_selection_by_itself() {
        showDates(selected = PublishedWithin.ANY, onSelect = {})

        option(PublishedWithin.LAST_YEAR).performClick()

        option(PublishedWithin.ANY).assertIsSelected()
        option(PublishedWithin.LAST_YEAR).assertIsNotSelected()
    }

    @Test
    fun it_follows_the_selection_when_the_owner_changes_it() {
        var selected by mutableStateOf(PublishedWithin.ANY)
        composeRule.setContent {
            GradusTheme { SingleChoiceRow(PublishedWithin.entries, selected, ::publishedLabel, {}) }
        }

        composeRule.runOnIdle { selected = PublishedWithin.LAST_YEAR }

        option(PublishedWithin.LAST_YEAR).assertIsSelected()
        option(PublishedWithin.ANY).assertIsNotSelected()
    }

    @Test
    fun every_option_is_read_as_a_radio_button() {
        showDates()

        PublishedWithin.entries.forEach {
            option(it).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
    }

    @Test
    fun every_option_is_at_least_48dp() {
        showDates()

        PublishedWithin.entries.forEach {
            option(it).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun all_the_options_have_the_same_width() {
        showDates(selected = PublishedWithin.LAST_YEAR)

        val widths = PublishedWithin.entries.map { option(it).fetchSemanticsNode().size.width }

        assertTrue("anchos distintos: $widths", widths.distinct().size == 1)
    }

    @Test
    fun it_works_with_any_type_of_option() {
        val received = mutableListOf<Size>()
        composeRule.setContent {
            GradusTheme {
                SingleChoiceRow(Size.entries, Size.SMALL, { it.name.lowercase() }, { received += it })
            }
        }

        composeRule.onNodeWithText("small").assertIsSelected()
        composeRule.onNodeWithText("large").performClick()

        assertEquals(listOf(Size.LARGE), received)
    }
}
