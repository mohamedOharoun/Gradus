package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Ver una nota (docs/tareas/notas-camara.md, tarea 6): qué enseña y eliminarla con confirmación.
// La foto no existe (recuadro gris): aquí se prueba la pantalla, no la lectura del fichero.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenNoteViewerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = System.currentTimeMillis()
    private val note = Note("n1", "g", "bfs", "notes/n1.jpg", "Dijkstra paso a paso", now)

    private fun show(state: NoteViewerState.Showing = NoteViewerState.Showing(note), actions: NoteViewerActions = NoteViewerActions()) =
        composeRule.setContentInSpanish { GradusTheme { ScreenNoteViewer(state, actions, photoFile = { File("no-existe") }) } }

    private fun deleteButton() = composeRule.onNode(hasClickAction() and hasContentDescription("Eliminar nota"))

    @Test
    fun the_date_is_the_title_and_the_caption_is_shown() {
        show()
        composeRule.onNode(isHeading()).assertExists()
        composeRule.onNodeWithText("Dijkstra paso a paso").assertIsDisplayed()
    }

    // TalkBack lee la foto con su fecha y su pie (decisión Y).
    @Test
    fun the_photo_is_read_with_its_date_and_caption() {
        show()
        composeRule.onNode(hasContentDescription("Nota del", substring = true) and hasContentDescription("Dijkstra", substring = true))
            .assertExists()
    }

    @Test
    fun a_note_without_caption_shows_only_the_photo() {
        show(NoteViewerState.Showing(note.copy(caption = null)))
        composeRule.onNodeWithText("Dijkstra paso a paso").assertDoesNotExist()
    }

    @Test
    fun delete_is_a_48dp_button_that_asks() {
        var asks = 0
        show(actions = NoteViewerActions(onDelete = { asks++ }))

        deleteButton().assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, asks)
    }

    // Decisión Z: se pregunta antes de borrar.
    @Test
    fun confirming_asks_to_delete_and_cancel_to_keep() {
        val asked = mutableListOf<String>()
        show(
            NoteViewerState.Showing(note, confirmingDelete = true),
            NoteViewerActions(onDeleteConfirm = { asked += "eliminar" }, onDeleteCancel = { asked += "cancelar" }),
        )

        composeRule.onNodeWithText("¿Eliminar esta nota?").assertIsDisplayed()
        composeRule.onNodeWithText("No se puede deshacer", substring = true).assertIsDisplayed()
        composeRule.onNode(hasClickAction() and hasText("Cancelar")).performClick()
        composeRule.onNode(hasClickAction() and hasText("Eliminar")).performClick()

        assertEquals(listOf("cancelar", "eliminar"), asked)
    }

    @Test
    fun while_deleting_both_buttons_wait() {
        show(NoteViewerState.Showing(note, confirmingDelete = true, deleting = true))
        composeRule.onNode(hasClickAction() and hasText("Eliminando…")).assertIsNotEnabled()
        composeRule.onNode(hasClickAction() and hasText("Cancelar")).assertIsNotEnabled()
    }

    @Test
    fun back_asks_to_go_back() {
        var backs = 0
        show(actions = NoteViewerActions(onBack = { backs++ }))

        composeRule.onNode(hasClickAction() and hasContentDescription("Volver")).performClick()

        assertEquals(1, backs)
    }
}
