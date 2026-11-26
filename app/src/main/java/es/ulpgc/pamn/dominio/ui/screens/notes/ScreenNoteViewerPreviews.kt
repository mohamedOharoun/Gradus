package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import java.io.File

// En las previews no hay foto de verdad: recuadro gris.
private val note = Note("n1", "g", "bfs", "notes/n1.jpg", "Dijkstra paso a paso", System.currentTimeMillis())

@Composable
private fun NoteViewerPreview(state: NoteViewerState.Showing) {
    GradusTheme { ScreenNoteViewer(state, NoteViewerActions(), photoFile = { File("no-existe") }) }
}

@LightDarkPreviews
@Composable
private fun ScreenNoteViewerPreview() = NoteViewerPreview(NoteViewerState.Showing(note))

@LightDarkPreviews
@Composable
private fun ScreenNoteViewerDeletePreview() = NoteViewerPreview(NoteViewerState.Showing(note, confirmingDelete = true))
