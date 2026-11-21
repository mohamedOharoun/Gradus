package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Lo que se ve y se toca en la caja de búsqueda: la ✕ solo con texto y de 48 dp, la tecla «Buscar»,
// el texto de ayuda y que el texto es del que la usa (campo controlado, como FormField).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class SearchFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clearDescription = "Borrar búsqueda"
    private val hint = "Busca por título o descripción"

    private fun show(
        value: String,
        onValueChange: (String) -> Unit = {},
        onSearch: () -> Unit = {},
    ) = composeRule.setContent {
        GradusTheme { SearchField(value, onValueChange, onSearch) }
    }

    private fun field() = composeRule.onNode(hasSetTextAction())

    // El texto de ayuda es un nodo suelto: se busca en el árbol sin fusionar.
    private fun hintNode() = composeRule.onNodeWithText(hint, useUnmergedTree = true)

    @Test
    fun without_text_there_is_no_clear_button() {
        show(value = "")

        composeRule.onNodeWithContentDescription(clearDescription).assertDoesNotExist()
    }

    @Test
    fun with_text_the_clear_button_is_at_least_48dp() {
        show(value = "grafos")

        composeRule.onNodeWithContentDescription(clearDescription)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun clicking_the_clear_button_reports_an_empty_text() {
        val received = mutableListOf<String>()
        show(value = "grafos", onValueChange = { received += it })

        composeRule.onNodeWithContentDescription(clearDescription).performClick()

        assertEquals(listOf(""), received)
    }

    @Test
    fun the_search_key_calls_onSearch() {
        var searches = 0
        show(value = "grafos", onSearch = { searches++ })

        field().performImeAction()

        assertEquals(1, searches)
    }

    @Test
    fun the_search_key_hides_the_keyboard_and_then_searches() {
        val events = mutableListOf<String>()
        val keyboard = object : SoftwareKeyboardController {
            override fun show() {}
            override fun hide() { events += "hide" }
        }
        composeRule.setContent {
            // CompositionLocalProvider: cambia, solo para lo de dentro, el teclado que ve la caja.
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                GradusTheme { SearchField("grafos", onValueChange = {}, onSearch = { events += "search" }) }
            }
        }

        field().performImeAction()

        assertEquals(listOf("hide", "search"), events)
    }

    @Test
    fun typing_reports_the_new_text() {
        val received = mutableListOf<String>()
        show(value = "", onValueChange = { received += it })

        field().performTextInput("grafos")

        assertEquals(listOf("grafos"), received)
    }

    @Test
    fun it_shows_the_value_it_receives() {
        show(value = "grafos")

        field().assertTextEquals("grafos")
    }

    @Test
    fun it_does_not_keep_the_text_by_itself() {
        // El que la usa ignora los cambios: la caja sigue enseñando su `value`.
        show(value = "grafos", onValueChange = {})

        field().performTextInput("x")

        field().assertTextEquals("grafos")
    }

    @Test
    fun it_follows_the_value_when_the_owner_changes_it() {
        var text by mutableStateOf("grafos")
        composeRule.setContent { GradusTheme { SearchField(text, onValueChange = {}, onSearch = {}) } }

        composeRule.runOnIdle { text = "árboles" }

        field().assertTextEquals("árboles")
    }

    @Test
    fun the_hint_is_shown_when_empty() {
        show(value = "")

        hintNode().assertIsDisplayed()
    }

    @Test
    fun the_hint_disappears_when_there_is_text() {
        show(value = "grafos")

        hintNode().assertDoesNotExist()
    }

    @Test
    fun the_magnifier_is_decorative_and_talkback_does_not_announce_it() {
        show(value = "")

        composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }
}
