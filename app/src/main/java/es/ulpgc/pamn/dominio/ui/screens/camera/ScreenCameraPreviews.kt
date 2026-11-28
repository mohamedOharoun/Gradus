package es.ulpgc.pamn.dominio.ui.screens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import java.io.File

// En las previews no hay cámara: la vista previa es un recuadro y la foto revisada, un fichero que no existe.
@Composable
private fun CameraPreview(state: CameraUiState) {
    GradusTheme {
        ScreenCamera(
            title = "Nueva nota · BFS",
            state = state,
            actions = CameraActions(),
            preview = { modifier -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) },
            photoFile = { File("no-existe") },
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenCameraPermissionPreview() = CameraPreview(CameraUiState(CameraStage.NeedsPermission(denied = true)))

@LightDarkPreviews
@Composable
private fun ScreenCameraSettingsPreview() =
    CameraPreview(CameraUiState(CameraStage.NeedsPermission(denied = true, permanently = true)))

@LightDarkPreviews
@Composable
private fun ScreenCameraReadyPreview() = CameraPreview(CameraUiState(CameraStage.Ready))

@LightDarkPreviews
@Composable
private fun ScreenCameraReviewPreview() =
    CameraPreview(CameraUiState(CameraStage.Reviewing("captura.jpg", caption = "Dijkstra paso a paso")))
