package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraActions
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraStage
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraUiState
import es.ulpgc.pamn.dominio.ui.screens.camera.ScreenCamera
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

// Las notas y la cámara con la app en inglés (docs/tareas/ajustes.md, fase 3, paso 8c): la fecha en orden
// inglés («Nov 24») y el pie, que escribe el alumno, tal cual. Se ejecuta en el emulador:
// ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class NotesInEnglishTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val longMonths = listOf(
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December",
    )
    private val today = LocalDate.now()
    private val note = Note("n1", "g", "bfs", "notes/n1.jpg", "Dijkstra paso a paso", System.currentTimeMillis())

    @Test
    fun my_notes_in_english_with_the_date_in_english_order() {
        composeRule.setContentInEnglish {
            GradusTheme { ScreenModuleNotes(ModuleNotesUiState("BFS", listOf(note)), ModuleNotesActions(onTakePhoto = {}), photoFile = { File("no-existe") }) }
        }
        composeRule.onNodeWithText("Notes · BFS").assertIsDisplayed()
        // La fecha, en orden inglés. La celda junta todo para TalkBack (clearAndSetSemantics): el texto suelto
        // está en el árbol sin fusionar.
        composeRule.onNodeWithText("${months[today.monthValue - 1]} ${today.dayOfMonth}", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Take photo").assertIsDisplayed()
        composeRule.onNodeWithText("From gallery").assertIsDisplayed()
        // TalkBack: la fecha completa y el pie tal cual.
        composeRule.onNodeWithContentDescription("Note from ${longMonths[today.monthValue - 1]} ${today.dayOfMonth}: Dijkstra paso a paso").assertExists()
    }

    @Test
    fun an_empty_list_in_english() {
        composeRule.setContentInEnglish {
            GradusTheme { ScreenModuleNotes(ModuleNotesUiState("BFS"), ModuleNotesActions(onTakePhoto = {}), photoFile = { File("no-existe") }) }
        }
        composeRule.onNodeWithText("You don't have notes for this module yet. Take a photo of your notes.").assertIsDisplayed()
    }

    @Test
    fun deleting_a_note_in_english() {
        composeRule.setContentInEnglish {
            GradusTheme { ScreenNoteViewer(NoteViewerState.Showing(note, confirmingDelete = true), NoteViewerActions(), photoFile = { File("no-existe") }) }
        }
        composeRule.onNodeWithText("Delete this note?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun the_camera_in_english() {
        composeRule.setContentInEnglish {
            GradusTheme {
                ScreenCamera(
                    title = "New note · BFS",
                    state = CameraUiState(CameraStage.Reviewing("captura.jpg")),
                    actions = CameraActions(),
                    preview = { modifier -> Box(modifier) },
                    photoFile = { File("no-existe") },
                )
            }
        }
        composeRule.onNodeWithText("Caption (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("Retake").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun the_camera_permission_in_english() {
        composeRule.setContentInEnglish {
            GradusTheme {
                ScreenCamera(
                    title = "New note · BFS",
                    state = CameraUiState(CameraStage.NeedsPermission()),
                    actions = CameraActions(),
                    preview = { modifier -> Box(modifier) },
                    photoFile = { File("no-existe") },
                )
            }
        }
        composeRule.onNodeWithText("To photograph your notes, Gradus needs to use the camera.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Allow the camera").assertIsDisplayed()
    }
}
