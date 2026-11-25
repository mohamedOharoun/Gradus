package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.study.PhotoStep
import es.ulpgc.pamn.dominio.course.study.PhotoTask
import es.ulpgc.pamn.dominio.ui.components.MarkdownText
import es.ulpgc.pamn.dominio.ui.components.NotePhoto
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import java.io.File

// El paso con foto en ScreenExercise (docs/tareas/ejercicio-foto.md, paso 5c y boceto del alumno).
// Sin estado: pinta el PhotoStep y avisa con las acciones. Los textos y qué botón toca, en PhotoStepTexts.kt.

// Lo que el paso con foto necesita además del PhotoStep: el aviso de la foto y si el curso admite entregas (Z).
data class PhotoStepUi(val notice: PhotoNotice? = null, val canSubmit: Boolean = true)

// Arriba: el enunciado, la foto (si la hay) y los botones para hacerla o elegirla.
@Composable
fun PhotoStepContent(moduleTitle: String, step: PhotoStep, ui: PhotoStepUi, actions: ExerciseActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(moduleTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        // MarkdownText pone el modificador en su contenedor: mergeDescendants junta el texto con él, y así
        // TalkBack lee el enunciado entero como encabezado.
        MarkdownText(
            step.photo.prompt,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics(mergeDescendants = true) { heading() },
        )
        Text(
            if (ui.canSubmit) "Resuélvelo en papel y hazle una foto para tu profesor." else NOT_PUBLISHED,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        step.photoPath?.let { path ->
            NotePhoto(
                File(path),
                maxSidePx = PREVIEW_SIDE_PX,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "Tu foto" },
            )
        }
        ui.notice?.let { notice ->
            Text(
                notice.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (notice == PhotoNotice.UNREADABLE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (step.acceptsPhoto && ui.canSubmit) {
            ChoosePhotoButtons(hasPhoto = step.photoPath != null, preparing = ui.notice == PhotoNotice.PREPARING, actions)
            if (step.photoPath != null) {
                TextButton(onClick = actions.onDiscardPhoto, modifier = Modifier.heightIn(min = 48.dp)) { Text("Quitar la foto") }
            }
        }
    }
}

// Abajo: el fallo o «Entregada» con la solución, y los dos botones que toquen.
@Composable
fun PhotoStepBottomBar(step: PhotoStep, ui: PhotoStepUi, actions: ExerciseActions) {
    BottomPanel(Modifier) {
        (step.task as? PhotoTask.Failed)?.let { failed ->
            Text(
                failed.problem.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (step.showsSolution) SubmittedMessage(step.photo.explanation)
        val main = step.mainButton(ui.canSubmit)
        PrimaryButton(
            text = main.text,
            enabled = main.enabled,
            onClick = if (step.task == PhotoTask.Sent) actions.onContinue else actions.onSubmitPhoto,
        )
        step.secondaryButton()?.let { secondary ->
            TextButton(
                onClick = when (secondary) {
                    SecondaryPhotoButton.LATER -> actions.onSubmitPhotoLater
                    SecondaryPhotoButton.AGAIN -> actions.onSubmitAgain
                    SecondaryPhotoButton.GO_ON -> actions.onContinue
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(secondary.text) }
        }
    }
}

// «Hacer foto» (cámara) y «De la galería», como en «Mis notas». Con foto ya hecha: «Hacer otra».
@Composable
private fun ChoosePhotoButtons(hasPhoto: Boolean, preparing: Boolean, actions: ExerciseActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val takePhoto = actions.onTakePhoto
        Button(
            onClick = { takePhoto?.invoke() },
            enabled = takePhoto != null && !preparing,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        ) { ButtonLabel(R.drawable.ic_photo_camera, if (hasPhoto) "Hacer otra" else "Hacer foto") }
        OutlinedButton(
            onClick = actions.onPickPhoto,
            enabled = !preparing,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        ) { ButtonLabel(R.drawable.ic_add, "De la galería") }
    }
}

@Composable
private fun ButtonLabel(icon: Int, text: String) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}

// Decisión C: entregada, el alumno ve la solución y sigue; no espera a la revisión. liveRegion: TalkBack
// lo lee al aparecer. Con una solución larga, el panel se desplaza en vez de tapar la foto.
@Composable
private fun SubmittedMessage(explanation: String) {
    Column(
        modifier = Modifier
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState())
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Entregada · pendiente de revisión", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        if (explanation.isNotBlank()) {
            Text("Solución", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            MarkdownText(explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun PhotoNotice.text(): String = when (this) {
    PhotoNotice.PREPARING -> "Preparando la foto…"
    PhotoNotice.UNREADABLE -> "No se ha podido leer la foto. Prueba con otra."
}

// Se puede poner foto sin foto, con una lista o tras un fallo; no mientras se entrega ni ya entregada.
internal val PhotoStep.acceptsPhoto: Boolean
    get() = task !is PhotoTask.Sending && task != PhotoTask.Sent

// Para verla en pantalla basta una versión reducida (la entregada ya tiene 2000 px como mucho).
private const val PREVIEW_SIDE_PX = 1080

private const val NOT_PUBLISHED =
    "Este curso no está publicado en el servidor, así que no se pueden entregar fotos. Puedes dejarlo para más tarde."
