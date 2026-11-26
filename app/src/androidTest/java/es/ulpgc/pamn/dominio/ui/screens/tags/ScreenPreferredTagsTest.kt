package es.ulpgc.pamn.dominio.ui.screens.tags

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.settings.TagAddition
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// «Etiquetas preferidas» (docs/tareas/ajustes.md, fase 3, tarea 3): qué enseña y qué se puede tocar.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenPreferredTagsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: PreferredTagsUiState = PreferredTagsUiState(), actions: PreferredTagsActions = PreferredTagsActions()) =
        composeRule.setContentInSpanish { GradusTheme { ScreenPreferredTags(state, actions) } }

    @Test
    fun without_tags_it_says_none_was_chosen() {
        show()
        composeRule.onNodeWithText("Aún no has elegido ninguna.").assertIsDisplayed()
        composeRule.onNodeWithText("0 de 10").assertIsDisplayed()
    }

    @Test
    fun the_chosen_tags_are_shown_with_a_hash_and_counted() {
        show(PreferredTagsUiState(tags = listOf("grafos", "kotlin")))
        composeRule.onNodeWithText("#grafos", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("#kotlin", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("2 de 10").assertIsDisplayed()
    }

    @Test
    fun typing_tells_the_draft() {
        val typed = mutableListOf<String>()
        show(actions = PreferredTagsActions(onDraftChange = { typed += it }))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("redes")

        assertEquals(listOf("redes"), typed)
    }

    @Test
    fun add_asks_to_add() {
        var adds = 0
        show(PreferredTagsUiState(draft = "redes"), PreferredTagsActions(onAdd = { adds++ }))

        composeRule.onNode(hasClickAction() and hasText("Añadir")).performClick()

        assertEquals(1, adds)
    }

    @Test
    fun why_a_tag_was_not_added_is_told_under_the_field() {
        show(PreferredTagsUiState(tags = listOf("grafos"), draft = "grafos", problem = TagAddition.AlreadyAdded))
        composeRule.onNodeWithText("Ya tienes esa etiqueta", useUnmergedTree = true).assertIsDisplayed()
    }

    // TalkBack lee cada chip como un botón «Quitar la etiqueta grafos».
    @Test
    fun each_tag_is_a_button_that_removes_it() {
        val removed = mutableListOf<String>()
        show(PreferredTagsUiState(tags = listOf("grafos", "kotlin")), PreferredTagsActions(onRemove = { removed += it }))

        composeRule.onNode(hasContentDescription("Quitar la etiqueta kotlin"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf("kotlin"), removed)
    }

    // Zona táctil de 48 dp: Material la amplía sin agrandar el dibujo, así que no sale en los límites
    // del nodo. Se comprueba como en FilterChipsTest: tocando a 22 dp del centro, fuera del dibujo.
    @Test
    fun each_tag_can_be_tapped_in_a_48dp_area() {
        val removed = mutableListOf<String>()
        show(PreferredTagsUiState(tags = listOf("grafos")), PreferredTagsActions(onRemove = { removed += it }))

        composeRule.onNode(hasContentDescription("Quitar la etiqueta grafos"))
            .performTouchInput { click(Offset(center.x, center.y - 22.dp.toPx())) }

        assertEquals(listOf("grafos"), removed)
    }

    @Test
    fun your_tags_is_a_heading() {
        show()
        composeRule.onNode(isHeading() and hasText("Tus etiquetas")).assertExists()
    }

    @Test
    fun back_asks_to_go_back() {
        var backs = 0
        show(actions = PreferredTagsActions(onBack = { backs++ }))

        composeRule.onNode(hasClickAction() and hasContentDescription("Volver")).performClick()

        assertEquals(1, backs)
    }
}
