package es.ulpgc.pamn.dominio.ui.screens.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.notes.MAX_CAPTION_LENGTH
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.FormField
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.NotePhoto
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import java.io.File

// Lo que se puede hacer en la cámara.
class CameraActions(
    val onAllowCamera: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onShutter: () -> Unit = {},
    val onRetake: () -> Unit = {},
    val onCaptionChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onClose: () -> Unit = {},
)

// La cámara de las notas (docs/tareas/notas-camara.md, boceto 3). Sin estado.
// - `preview`: la vista previa de la cámara (CameraX en la app; en tests y previews, un recuadro).
// - `photoFile`: el fichero de la foto temporal, para enseñarla al revisar.
@Composable
fun ScreenCamera(
    title: String,
    state: CameraUiState,
    actions: CameraActions,
    preview: @Composable (Modifier) -> Unit,
    photoFile: (String) -> File,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            FormTopBar(title = title, onBack = actions.onClose)
            Spacer(Modifier.height(16.dp))
            when (val stage = state.stage) {
                is CameraStage.NeedsPermission -> PermissionContent(stage, actions)
                CameraStage.Ready, is CameraStage.Capturing ->
                    ShootingContent(capturing = stage is CameraStage.Capturing, failed = state.captureFailed, actions, preview)
                is CameraStage.Reviewing -> ReviewContent(stage, actions, photoFile(stage.capture))
                CameraStage.Closed -> Unit
            }
        }
    }
}

// Decisión X: para qué hace falta el permiso; si se denegó para siempre, a los ajustes del sistema.
@Composable
private fun ColumnScope.PermissionContent(stage: CameraStage.NeedsPermission, actions: CameraActions) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(48.dp))
        Text(permissionText(stage), style = MaterialTheme.typography.bodyLarge)
        Text(
            "Si no, puedes añadir fotos «De la galería».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (stage.permanently) {
        PrimaryButton(text = "Abrir ajustes", onClick = actions.onOpenSettings)
    } else {
        PrimaryButton(text = "Permitir la cámara", onClick = actions.onAllowCamera)
    }
}

// Boceto 3, izquierda: la vista previa y el disparador. Mientras se hace la foto, el disparador espera.
@Composable
private fun ColumnScope.ShootingContent(capturing: Boolean, failed: Boolean, actions: CameraActions, preview: @Composable (Modifier) -> Unit) {
    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        preview(Modifier.fillMaxSize())
    }
    Spacer(Modifier.height(16.dp))
    if (failed) ErrorText(CAPTURE_FAILED, Modifier.padding(bottom = 8.dp))
    ShutterButton(enabled = !capturing, onClick = actions.onShutter, modifier = Modifier.align(Alignment.CenterHorizontally))
}

// El disparador: un círculo de 72 dp (más que los 48 dp táctiles). TalkBack lo lee «Hacer foto».
@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.size(72.dp).semantics { contentDescription = "Hacer foto" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

// Boceto 3, derecha: la foto hecha, el pie opcional y «Repetir» / «Guardar».
@Composable
private fun ColumnScope.ReviewContent(review: CameraStage.Reviewing, actions: CameraActions, photo: File) {
    val side = with(LocalDensity.current) { 720.dp.roundToPx() }
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NotePhoto(
            photo,
            maxSidePx = side,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(16.dp)),
        )
        FormField(
            label = "Pie de foto (opcional)",
            value = review.caption,
            onValueChange = actions.onCaptionChange,
            error = CAPTION_TOO_LONG.takeIf { review.captionTooLong },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        )
    }
    Spacer(Modifier.height(16.dp))
    if (review.saveFailed) ErrorText(SAVE_FAILED, Modifier.padding(bottom = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = actions.onRetake, enabled = !review.saving, modifier = Modifier.weight(1f).height(56.dp)) {
            Text("Repetir")
        }
        PrimaryButton(
            text = if (review.saving) "Guardando…" else "Guardar",
            onClick = actions.onSave,
            enabled = !review.saving,
            modifier = Modifier.weight(1f),
        )
    }
}

internal fun permissionText(stage: CameraStage.NeedsPermission): String = when {
    stage.permanently ->
        "Para fotografiar tus apuntes, activa el permiso de cámara en los ajustes del móvil. Las fotos se quedan en tu móvil."
    else -> "Para fotografiar tus apuntes, Gradus necesita usar la cámara. Las fotos se quedan en tu móvil."
}

private const val CAPTURE_FAILED = "No se ha podido hacer la foto. Inténtalo otra vez."
private const val SAVE_FAILED = "No se ha podido guardar la foto. Inténtalo otra vez."
private val CAPTION_TOO_LONG = "Como mucho $MAX_CAPTION_LENGTH caracteres"
