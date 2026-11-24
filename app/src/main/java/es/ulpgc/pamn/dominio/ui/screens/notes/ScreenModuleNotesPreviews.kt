package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import java.io.File

// En las previews no hay fotos de verdad: el fichero no existe y se ve el recuadro gris.
private val day = 86_400_000L
private val now = System.currentTimeMillis()
private val someNotes = listOf(
    Note("n3", "g", "bfs", "notes/n3.jpg", "Dijkstra paso a paso", now),
    Note("n2", "g", "bfs", "notes/n2.jpg", null, now - 2 * day),
    Note("n1", "g", "bfs", "notes/n1.jpg", null, now - 400 * day),
)

@Composable
private fun ModuleNotesPreview(state: ModuleNotesUiState, actions: ModuleNotesActions = ModuleNotesActions(onTakePhoto = {})) {
    GradusTheme { ScreenModuleNotes(state, actions, photoFile = { File("no-existe") }) }
}

@LightDarkPreviews
@Composable
private fun ScreenModuleNotesEmptyPreview() = ModuleNotesPreview(ModuleNotesUiState("BFS"))

@LightDarkPreviews
@Composable
private fun ScreenModuleNotesSomePreview() = ModuleNotesPreview(ModuleNotesUiState("BFS", someNotes))

@LightDarkPreviews
@Composable
private fun ScreenModuleNotesFailedPreview() = ModuleNotesPreview(ModuleNotesUiState("BFS", someNotes, addFailed = true))
