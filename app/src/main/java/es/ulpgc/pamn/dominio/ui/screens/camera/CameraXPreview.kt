package es.ulpgc.pamn.dominio.ui.screens.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

// Lo único de la app que habla con CameraX (docs/tareas/notas-camara.md, decisión T).
// LifecycleCameraController hace casi todo: abre la cámara trasera, enseña la vista previa y la apaga
// sola cuando la pantalla deja de verse (está atado al ciclo de vida de la pantalla).
// `active`: solo se enciende la cámara con permiso y mientras se usa (no al revisar la foto).
@Composable
fun rememberCameraController(active: Boolean): LifecycleCameraController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context).apply { setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE) } }
    DisposableEffect(lifecycleOwner, active) {
        if (active) controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    return controller
}

// La vista previa. PreviewView es una vista clásica de Android (no XML): AndroidView la mete en Compose.
@Composable
fun CameraXPreview(controller: LifecycleCameraController, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context -> PreviewView(context).apply { this.controller = controller } },
        modifier = modifier,
    )
}

// Hace la foto y la escribe en `target`. La respuesta llega en el hilo principal.
fun LifecycleCameraController.takePictureTo(context: Context, target: File, onDone: () -> Unit, onFailed: () -> Unit) {
    takePicture(
        ImageCapture.OutputFileOptions.Builder(target).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = onDone()
            override fun onError(exception: ImageCaptureException) = onFailed()
        },
    )
}
