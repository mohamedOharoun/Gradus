package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.ui.components.BackButton
import es.ulpgc.pamn.dominio.ui.components.ZoomablePhoto
import es.ulpgc.pamn.dominio.ui.text.asString
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

// Lo que se puede hacer al ver una nota.
class NoteViewerActions(
    val onDelete: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteCancel: () -> Unit = {},
    val onBack: () -> Unit = {},
)

// Ver una nota (docs/tareas/notas-camara.md, boceto 4): la foto con zoom, su fecha y su pie; «Eliminar»
// arriba a la derecha pide confirmación (decisión Z). Sin estado. Previews en ScreenNoteViewerPreviews.kt.
@Composable
fun ScreenNoteViewer(
    state: NoteViewerState.Showing,
    actions: NoteViewerActions,
    photoFile: (Note) -> File,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val note = state.note
    val side = with(LocalDensity.current) { 1_200.dp.roundToPx() }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = actions.onBack)
                Text(
                    noteFullDate(note.createdAt, today, zone).asString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp).semantics { heading() },
                )
                // IconButton mide 40 dp: se agranda a 48 dp (zona táctil mínima).
                IconButton(onClick = actions.onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.note_delete_description), tint = MaterialTheme.colorScheme.error)
                }
            }
            val description = noteDescription(note.createdAt, note.caption, today, zone).asString()
            ZoomablePhoto(
                photoFile(note),
                maxSidePx = side,
                modifier = Modifier.weight(1f).fillMaxWidth().clearAndSetSemantics { contentDescription = description },
            )
            note.caption?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
    if (state.confirmingDelete) DeleteNoteDialog(state.deleting, actions)
}

// Decisión Z: no hay papelera, así que se pregunta.
@Composable
private fun DeleteNoteDialog(deleting: Boolean, actions: NoteViewerActions) {
    AlertDialog(
        onDismissRequest = actions.onDeleteCancel,
        title = { Text(stringResource(R.string.note_delete_title)) },
        text = { Text(stringResource(R.string.note_delete_body)) },
        confirmButton = {
            TextButton(
                onClick = actions.onDeleteConfirm,
                enabled = !deleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(if (deleting) R.string.action_deleting else R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = actions.onDeleteCancel, enabled = !deleting) { Text(stringResource(R.string.action_cancel)) } },
    )
}
