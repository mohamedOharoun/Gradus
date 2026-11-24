package es.ulpgc.pamn.dominio.ui.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraActions
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraStage
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraViewModel
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraXPreview
import es.ulpgc.pamn.dominio.ui.screens.camera.ScreenCamera
import es.ulpgc.pamn.dominio.ui.screens.camera.rememberCameraController
import es.ulpgc.pamn.dominio.ui.screens.camera.takePictureTo
import java.io.File

// La cámara de las notas (docs/tareas/notas-camara.md, tarea 5), dentro del grafo Main y sin barra.
// En su propio fichero para que MainNavGraph.kt, que también edita el compañero, solo tenga una línea más.
// Aquí va todo lo de Android: el permiso, CameraX, abrir los ajustes. La lógica está en CameraViewModel.
internal fun NavGraphBuilder.takeNoteDestination(navController: NavController) {
    composable<Route.TakeNote> { entry ->
        val route = entry.toRoute<Route.TakeNote>()
        val context = LocalContext.current
        val activity = LocalActivity.current
        val title = AppContainer.courseGateway.cached(route.courseId)?.module(route.moduleId)?.title
        val cameraViewModel: CameraViewModel = viewModel(
            factory = CameraViewModel.factory(route.courseId, route.moduleId, AppContainer.notesStore, AppContainer.notePhotos),
        )
        val state = cameraViewModel.state

        fun hasPermission() =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        // Decisión X. Tras un «no», si Android ya no va a volver a preguntar (shouldShowRequestPermissionRationale
        // devuelve false), solo quedan los ajustes del sistema.
        val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val canAskAgain = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            cameraViewModel.onPermission(granted, canAskAgain)
        }
        // Al entrar, se pide (o, si ya estaba dado, se enciende la cámara). Al volver de los ajustes del
        // sistema, se vuelve a mirar.
        LaunchedEffect(Unit) {
            if (hasPermission()) cameraViewModel.onPermission(granted = true) else askPermission.launch(Manifest.permission.CAMERA)
        }
        LifecycleResumeEffect(Unit) {
            if (hasPermission()) cameraViewModel.onPermission(granted = true)
            onPauseOrDispose {}
        }

        // Guardada o abandonada: de vuelta a «Mis notas» (que ya enseña la nota nueva).
        if (state.stage == CameraStage.Closed) {
            LaunchedEffect(Unit) { navController.popBackStack() }
            return@composable
        }
        BackHandler { cameraViewModel.onLeave() }

        val controller = rememberCameraController(active = state.stage == CameraStage.Ready || state.stage is CameraStage.Capturing)
        // Al disparar, el ViewModel dice dónde escribir la foto; aquí se la pide a CameraX una sola vez.
        (state.stage as? CameraStage.Capturing)?.let { capturing ->
            LaunchedEffect(capturing.capture) {
                controller.takePictureTo(
                    context, File(capturing.capture),
                    onDone = cameraViewModel::onCaptured,
                    onFailed = cameraViewModel::onCaptureFailed,
                )
            }
        }

        ScreenCamera(
            title = if (title == null) "Nueva nota" else "Nueva nota · $title",
            state = state,
            actions = CameraActions(
                onAllowCamera = { askPermission.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
                onShutter = cameraViewModel::onShutter,
                onRetake = cameraViewModel::onRetake,
                onCaptionChange = cameraViewModel::onCaptionChange,
                onSave = cameraViewModel::onSave,
                onClose = cameraViewModel::onLeave,
            ),
            preview = { modifier -> CameraXPreview(controller, modifier) },
            photoFile = ::File,
        )
    }
}
