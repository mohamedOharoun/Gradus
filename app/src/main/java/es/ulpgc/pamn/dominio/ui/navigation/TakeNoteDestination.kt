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

// La cámara de las notas (docs/tareas/notas-camara.md, tarea 5), dentro del grafo Main y sin barra.
// En su propio fichero para que MainNavGraph.kt, que también edita el compañero, solo tenga una línea más.
// Lo de Android (permiso, CameraX) está en CameraRoute, compartido con la foto de las entregas.
internal fun NavGraphBuilder.takeNoteDestination(navController: NavController) {
    composable<Route.TakeNote> { entry ->
        val route = entry.toRoute<Route.TakeNote>()
        val title = AppContainer.courseGateway.cached(route.courseId)?.module(route.moduleId)?.title
        val cameraViewModel: CameraViewModel = viewModel(
            factory = CameraViewModel.factory(CameraPurpose.Note(route.courseId, route.moduleId, AppContainer.notesStore), AppContainer.notePhotos),
        )
        // Guardada o abandonada: de vuelta a «Mis notas» (que ya enseña la nota nueva).
        CameraRoute(
            cameraViewModel,
            title = if (title == null) stringResource(R.string.camera_new_note) else stringResource(R.string.camera_new_note_of, title),
            onFinished = { navController.popBackStack() },
        )
    }
}
