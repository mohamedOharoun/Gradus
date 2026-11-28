package es.ulpgc.pamn.dominio.ui.navigation

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.notes.ModuleNotesActions
import es.ulpgc.pamn.dominio.ui.screens.notes.ModuleNotesViewModel
import es.ulpgc.pamn.dominio.ui.screens.notes.ScreenModuleNotes

// «Mis notas» de un módulo (docs/tareas/notas-camara.md, tarea 4), dentro del grafo Main y sin barra.
// En su propio fichero para que MainNavGraph.kt, que también edita el compañero, solo tenga una línea más.
// Se llega desde la hoja del módulo en el camino; «atrás» vuelve al camino.
internal fun NavGraphBuilder.moduleNotesDestination(navController: NavController) {
    composable<Route.ModuleNotes> { entry ->
        val route = entry.toRoute<Route.ModuleNotes>()
        // El título del módulo sale del curso ya descargado (el camino lo dejó en la caché).
        val title = AppContainer.courseGateway.cached(route.courseId)?.module(route.moduleId)?.title ?: stringResource(R.string.notes_this_module)
        val notesViewModel: ModuleNotesViewModel = viewModel(
            factory = ModuleNotesViewModel.factory(route.courseId, route.moduleId, title, AppContainer.notesStore, AppContainer.notePhotos),
        )
        // Decisión U: el selector de fotos de Android. No pide permisos: el usuario elige una foto y la
        // app solo puede leer esa. Si cierra el selector sin elegir, llega null y no pasa nada.
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { notesViewModel.onPhotoPicked(it.toString()) }
        }
        val context = LocalContext.current
        val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
        ScreenModuleNotes(
            state = notesViewModel.state,
            actions = ModuleNotesActions(
                // Sin cámara en el móvil (se puede instalar sin ella), solo «De la galería».
                onTakePhoto = if (hasCamera) dropUnlessResumed { navController.navigate(Route.TakeNote(route.courseId, route.moduleId)) } else null,
                onFromGallery = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                // Solo con la pantalla activa: dos toques rápidos no abren la nota dos veces.
                onOpenNote = LocalLifecycleOwner.current.onlyWhenResumed { noteId: String -> navController.navigate(Route.NoteViewer(noteId)) },
                onBack = dropUnlessResumed { navController.popBackStack() },
            ),
            photoFile = { AppContainer.notePhotos.fileOf(it.file) },
        )
    }
}
