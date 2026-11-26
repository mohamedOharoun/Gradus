package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// La hoja de un módulo del camino (docs/tareas/notas-camara.md, boceto 1, decisión S): su título, su
// estado, «Estudiar» (o «Repasar») y «Mis notas · N». Se cierra deslizándola o tocando fuera.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleSheet(
    title: String,
    state: ModuleState,
    noteCount: Int,
    actions: ModuleSheetActions,
) {
    ModalBottomSheet(onDismissRequest = actions.onDismiss) {
        ModuleSheetContent(title, state, noteCount, actions)
    }
}

// Lo que se puede hacer en la hoja.
class ModuleSheetActions(
    val onStudy: () -> Unit = {},
    val onOpenNotes: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

// El contenido, aparte del ModalBottomSheet: así se prueba y se previsualiza sin abrir la hoja.
@Composable
fun ModuleSheetContent(
    title: String,
    state: ModuleState,
    noteCount: Int,
    actions: ModuleSheetActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(moduleStateText(state).asString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = studyButtonText(state).asString(), onClick = actions.onStudy)
        NotesButton(noteCount, actions.onOpenNotes)
    }
}

// «📷 Mis notas · 3». TalkBack lo lee con palabras: «Mis notas, 3 fotos».
@Composable
private fun NotesButton(count: Int, onClick: () -> Unit) {
    val description = notesButtonDescription(count).asString()
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { contentDescription = description },
    ) {
        Row(Modifier.clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(notesButtonText(count).asString())
        }
    }
}

@LightDarkPreviews
@Composable
private fun ModuleSheetFrontierPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ModuleSheetContent("Representar un grafo", ModuleState.FRONTIER, noteCount = 0, actions = ModuleSheetActions())
        }
    }
}

@LightDarkPreviews
@Composable
private fun ModuleSheetMasteredPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ModuleSheetContent("BFS", ModuleState.MASTERED, noteCount = 3, actions = ModuleSheetActions())
        }
    }
}
