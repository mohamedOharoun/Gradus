package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
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

// «Mis notas» de un módulo (docs/tareas/notas-camara.md, tarea 4): qué enseña y qué se toca.
// Las fotos no existen (recuadro gris): aquí se prueba la pantalla, no la lectura de ficheros.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenModuleNotesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = System.currentTimeMillis()
    private val withCaption = Note("n2", "g", "bfs", "notes/n2.jpg", "Dijkstra paso a paso", now)
    private val withoutCaption = Note("n1", "g", "bfs", "notes/n1.jpg", null, now - 86_400_000L)

    private fun show(state: ModuleNotesUiState = ModuleNotesUiState("BFS"), actions: ModuleNotesActions = ModuleNotesActions(onTakePhoto = {})) =
        composeRule.setContentInSpanish { GradusTheme { ScreenModuleNotes(state, actions, photoFile = { File("no-existe") }) } }

    private fun button(text: String) = composeRule.onNode(hasClickAction() and hasText(text))

    @Test
    fun the_title_says_whose_notes_they_are() {
        show()
        composeRule.onNodeWithText("Notas · BFS").assertIsDisplayed()
    }

    @Test
    fun without_notes_it_invites_to_take_a_photo() {
        show()
        composeRule.onNodeWithText("Aún no tienes notas de este módulo", substring = true).assertIsDisplayed()
    }

    // TalkBack lee cada foto con su fecha y, si lo tiene, su pie (decisión Y).
    @Test
    fun each_note_is_read_with_its_date_and_caption() {
        show(ModuleNotesUiState("BFS", listOf(withCaption, withoutCaption)))
        composeRule.onNode(hasContentDescription("Dijkstra paso a paso", substring = true)).assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Nota del", substring = true) and hasContentDescription("Dijkstra", substring = true).not())
            .assertIsDisplayed()
    }

    // Tarea 6: tocar una foto la abre.
    @Test
    fun touching_a_note_opens_it() {
        val opened = mutableListOf<String>()
        show(ModuleNotesUiState("BFS", listOf(withCaption, withoutCaption)), ModuleNotesActions(onOpenNote = { opened += it }))

        composeRule.onNode(hasClickAction() and hasContentDescription("Dijkstra paso a paso", substring = true))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(listOf("n2"), opened)
    }

    @Test
    fun from_the_gallery_asks_to_pick_a_photo() {
        var picks = 0
        show(actions = ModuleNotesActions(onTakePhoto = {}, onFromGallery = { picks++ }))

        button("De la galería").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, picks)
    }

    @Test
    fun take_photo_asks_to_open_the_camera() {
        var opens = 0
        show(actions = ModuleNotesActions(onTakePhoto = { opens++ }))

        button("Hacer foto").assertIsEnabled().performClick()

        assertEquals(1, opens)
    }

    // Sin cámara todavía (tarea 5): el botón se ve, desactivado.
    @Test
    fun without_camera_take_photo_is_disabled() {
        show(actions = ModuleNotesActions(onTakePhoto = null))
        button("Hacer foto").assertIsNotEnabled()
    }

    @Test
    fun while_adding_the_buttons_say_so_and_wait() {
        show(ModuleNotesUiState("BFS", adding = true))
        button("Añadiendo…").assertIsNotEnabled()
        button("Hacer foto").assertIsNotEnabled()
    }

    @Test
    fun a_photo_that_could_not_be_added_is_told() {
        show(ModuleNotesUiState("BFS", addFailed = true))
        composeRule.onNodeWithText("No se ha podido añadir la foto", substring = true).assertIsDisplayed()
    }

    @Test
    fun back_asks_to_go_back() {
        var backs = 0
        show(actions = ModuleNotesActions(onBack = { backs++ }))

        composeRule.onNode(hasClickAction() and hasContentDescription("Volver")).performClick()

        assertEquals(1, backs)
    }
}
