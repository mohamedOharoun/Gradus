package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// La hoja de un módulo del camino (docs/tareas/notas-camara.md, tarea 3): qué enseña y qué se toca.
// Se prueba su contenido (sin el ModalBottomSheet, que es de Material).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ModuleSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: ModuleState = ModuleState.FRONTIER, count: Int = 0, actions: ModuleSheetActions = ModuleSheetActions()) =
        composeRule.setContentInSpanish { GradusTheme { ModuleSheetContent("BFS", state, count, actions) } }

    @Test
    fun the_title_is_a_heading_and_the_state_is_said() {
        show(ModuleState.MASTERED)
        composeRule.onNode(isHeading() and hasText("BFS")).assertExists()
        composeRule.onNodeWithText("Dominado").assertIsDisplayed()
    }

    @Test
    fun a_frontier_module_is_studied() {
        var studies = 0
        show(ModuleState.FRONTIER, actions = ModuleSheetActions(onStudy = { studies++ }))

        composeRule.onNode(hasClickAction() and hasText("Estudiar")).assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, studies)
    }

    @Test
    fun a_mastered_module_is_reviewed() {
        show(ModuleState.MASTERED)
        composeRule.onNode(hasClickAction() and hasText("Repasar")).assertExists()
    }

    @Test
    fun my_notes_shows_how_many_and_opens_them() {
        var opens = 0
        show(count = 3, actions = ModuleSheetActions(onOpenNotes = { opens++ }))

        composeRule.onNodeWithText("Mis notas · 3", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(hasClickAction() and hasContentDescription("Mis notas, 3 fotos"))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, opens)
    }

    @Test
    fun without_notes_talkback_hears_there_are_none() {
        show(count = 0)
        composeRule.onNode(hasClickAction() and hasContentDescription("Mis notas, ninguna todavía")).assertExists()
    }
}
