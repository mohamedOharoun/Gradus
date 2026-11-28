package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.notes.NoteViewerActions
import es.ulpgc.pamn.dominio.ui.screens.notes.NoteViewerState
import es.ulpgc.pamn.dominio.ui.screens.notes.NoteViewerViewModel
import es.ulpgc.pamn.dominio.ui.screens.notes.ScreenNoteViewer

// Ver una nota (docs/tareas/notas-camara.md, tarea 6), dentro del grafo Main y sin barra. En su propio
// fichero para que MainNavGraph.kt, que también edita el compañero, solo tenga una línea más.
// Se llega tocando una foto en «Mis notas»; al eliminarla, o con «atrás», vuelve a «Mis notas».
internal fun NavGraphBuilder.noteViewerDestination(navController: NavController) {
    composable<Route.NoteViewer> { entry ->
        val route = entry.toRoute<Route.NoteViewer>()
        val viewerViewModel: NoteViewerViewModel = viewModel(
            factory = NoteViewerViewModel.factory(route.noteId, AppContainer.notesStore, AppContainer.notePhotos),
        )
        when (val state = viewerViewModel.state) {
            NoteViewerState.Loading -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
            is NoteViewerState.Showing -> ScreenNoteViewer(
                state = state,
                actions = NoteViewerActions(
                    onDelete = viewerViewModel::onDelete,
                    onDeleteConfirm = viewerViewModel::onDeleteConfirm,
                    onDeleteCancel = viewerViewModel::onDeleteCancel,
                    onBack = dropUnlessResumed { navController.popBackStack() },
                ),
                photoFile = { AppContainer.notePhotos.fileOf(it.file) },
            )
            NoteViewerState.Gone -> LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }
}
