package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraPurpose
import es.ulpgc.pamn.dominio.ui.screens.camera.CameraViewModel

// La cámara de la foto de una entrega (docs/tareas/ejercicio-foto.md, decisión W), dentro del grafo Main y
// sin barra. «Usar foto» devuelve la ruta de la foto a la pantalla del ejercicio, la anterior, con su
// savedStateHandle (la forma de Navigation de devolver un resultado); la recoge rememberExercisePhotoSources.
internal fun NavGraphBuilder.takeSubmissionPhotoDestination(navController: NavController) {
    composable<Route.TakeSubmissionPhoto> { entry ->
        val route = entry.toRoute<Route.TakeSubmissionPhoto>()
        val title = AppContainer.courseGateway.cached(route.courseId)?.module(route.moduleId)?.title
        // La cámara escribe en cache/captures/, como la de las notas (AppContainer.notePhotos).
        val cameraViewModel: CameraViewModel = viewModel(
            factory = CameraViewModel.factory(CameraPurpose.Submission, AppContainer.notePhotos),
        )
        CameraRoute(
            cameraViewModel,
            title = if (title == null) stringResource(R.string.camera_submission_photo)
            else stringResource(R.string.camera_submission_photo_of, title),
            onFinished = { used ->
                if (used != null) navController.previousBackStackEntry?.savedStateHandle?.set(TAKEN_PHOTO_KEY, used)
                navController.popBackStack()
            },
        )
    }
}

// La clave con la que la cámara deja la foto en el savedStateHandle del ejercicio.
internal const val TAKEN_PHOTO_KEY = "submission_photo"
