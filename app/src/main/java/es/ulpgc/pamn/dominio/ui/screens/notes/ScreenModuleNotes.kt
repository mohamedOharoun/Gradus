package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.NotePhoto
import es.ulpgc.pamn.dominio.ui.text.asString
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

// Lo que se puede hacer en «Mis notas».
class ModuleNotesActions(
    // Tarea 5: abrir la cámara. Hasta entonces, null (el botón sale desactivado).
    val onTakePhoto: (() -> Unit)? = null,
    val onFromGallery: () -> Unit = {},
    // Tarea 6: tocar una foto la abre.
    val onOpenNote: (noteId: String) -> Unit = {},
    val onBack: () -> Unit = {},
)

// «Mis notas» de un módulo (docs/tareas/notas-camara.md, boceto 2): las fotos en cuadrícula, la más
// reciente primero, con su fecha; y abajo, «Hacer foto» y «De la galería». Sin estado.
// `photoFile` dice dónde está la foto de cada nota (en las previews, un fichero que no existe: recuadro gris).
// Previews en ScreenModuleNotesPreviews.kt.
@Composable
fun ScreenModuleNotes(
    state: ModuleNotesUiState,
    actions: ModuleNotesActions,
    photoFile: (Note) -> File,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            FormTopBar(title = notesTitle(state.moduleTitle).asString(), onBack = actions.onBack)
            if (state.notes.isEmpty()) {
                EmptyNotes(Modifier.weight(1f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier.weight(1f).padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteCell(note, photoFile(note), today, zone, onOpen = { actions.onOpenNote(note.id) })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.addFailed) ErrorText(stringResource(R.string.notes_add_failed), Modifier.padding(bottom = 8.dp))
            AddButtons(state.adding, actions)
        }
    }
}

@Composable
private fun EmptyNotes(modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.notes_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Una foto y su fecha; se toca para verla. Para TalkBack, un solo botón: «Nota del 24 de noviembre: …»,
// con la acción «Ver la nota».
@Composable
private fun NoteCell(note: Note, file: File, today: LocalDate, zone: ZoneId, onOpen: () -> Unit) {
    val description = noteDescription(note.createdAt, note.caption, today, zone).asString()
    val openLabel = stringResource(R.string.notes_open)
    val side = with(LocalDensity.current) { 160.dp.roundToPx() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = openLabel) { onOpen(); true }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NotePhoto(file, maxSidePx = side, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
        Text(noteDateText(note.createdAt, today, zone).asString(), style = MaterialTheme.typography.bodySmall)
    }
}

// «Hacer foto» (cámara, tarea 5) y «De la galería». Mientras se copia una foto, «Añadiendo…».
@Composable
private fun AddButtons(adding: Boolean, actions: ModuleNotesActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val takePhoto = actions.onTakePhoto
        Button(
            onClick = { takePhoto?.invoke() },
            enabled = takePhoto != null && !adding,
            modifier = Modifier.weight(1f).height(56.dp),
        ) { ButtonLabel(R.drawable.ic_photo_camera, stringResource(R.string.photo_take)) }
        OutlinedButton(
            onClick = actions.onFromGallery,
            enabled = !adding,
            modifier = Modifier.weight(1f).height(56.dp),
        ) { ButtonLabel(R.drawable.ic_add, stringResource(if (adding) R.string.notes_adding else R.string.photo_from_gallery)) }
    }
}

@Composable
private fun ButtonLabel(icon: Int, text: String) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}
