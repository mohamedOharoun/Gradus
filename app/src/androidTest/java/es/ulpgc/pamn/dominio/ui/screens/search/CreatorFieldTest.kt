package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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

// Tarea 11c de buscar-cursos.md: el campo «Creador» de la hoja de filtros.
// Se ejecutan en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class CreatorFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(value: String, onValueChange: (String) -> Unit = {}) =
        composeRule.setContent { GradusTheme { CreatorField(value, onValueChange) } }

    private fun field() = composeRule.onNode(hasSetTextAction())

    // La ayuda es un nodo suelto: se busca en el árbol sin fusionar.
    private fun hintNode() = composeRule.onNodeWithText(CREATOR_HINT, useUnmergedTree = true)

    private fun hasFieldDescription() =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(CREATOR_FIELD_DESCRIPTION))

    @Test
    fun the_hint_is_shown_when_empty() {
        show(value = "")

        hintNode().assertIsDisplayed()
    }

    @Test
    fun the_hint_disappears_when_there_is_text() {
        show(value = "Ana")

        hintNode().assertDoesNotExist()
    }

    @Test
    fun without_text_there_is_no_clear_button() {
        show(value = "")

        composeRule.onNodeWithContentDescription(CLEAR_CREATOR_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun with_text_the_clear_button_is_at_least_48dp() {
        show(value = "Ana")

        composeRule.onNodeWithContentDescription(CLEAR_CREATOR_DESCRIPTION)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun clicking_the_clear_button_reports_an_empty_text() {
        val received = mutableListOf<String>()
        show(value = "Ana", onValueChange = { received += it })

        composeRule.onNodeWithContentDescription(CLEAR_CREATOR_DESCRIPTION).performClick()

        assertEquals(listOf(""), received)
    }

    @Test
    fun typing_reports_the_new_text() {
        val received = mutableListOf<String>()
        show(value = "", onValueChange = { received += it })

        field().performTextInput("Ana")

        assertEquals(listOf("Ana"), received)
    }

    @Test
    fun it_shows_the_value_it_receives() {
        show(value = "Ana Ruiz")

        field().assertTextEquals("Ana Ruiz")
    }

    @Test
    fun it_does_not_keep_the_text_by_itself() {
        show(value = "Ana", onValueChange = {})

        field().performTextInput("x")

        field().assertTextEquals("Ana")
    }

    @Test
    fun it_follows_the_value_when_the_owner_changes_it() {
        var text by mutableStateOf("Ana")
        composeRule.setContent { GradusTheme { CreatorField(text, onValueChange = {}) } }

        composeRule.runOnIdle { text = "Jorge" }

        field().assertTextEquals("Jorge")
    }

    @Test
    fun talkback_knows_what_the_field_is_for_when_it_is_empty() {
        show(value = "")

        field().assert(hasFieldDescription())
    }

    @Test
    fun talkback_knows_what_the_field_is_for_when_it_already_has_text() {
        show(value = "Ana")

        field().assert(hasFieldDescription())
        field().assertTextEquals("Ana")
    }

    @Test
    fun the_person_icon_is_decorative_and_talkback_does_not_announce_it() {
        show(value = "")

        // Solo el propio campo lleva descripción: ni el icono ni la ayuda añaden otra.
        composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).assertCountEquals(1)
    }

    @Test
    fun the_done_key_hides_the_keyboard_and_reports_nothing() {
        val events = mutableListOf<String>()
        val keyboard = object : SoftwareKeyboardController {
            override fun show() {}
            override fun hide() { events += "hide" }
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                GradusTheme { CreatorField("Ana", onValueChange = { events += "change:$it" }) }
            }
        }

        field().performImeAction()

        assertEquals(listOf("hide"), events)
    }
}
