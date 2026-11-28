package es.ulpgc.pamn.dominio.ui.navigation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.tags.PreferredTagsActions
import es.ulpgc.pamn.dominio.ui.screens.tags.PreferredTagsViewModel
import es.ulpgc.pamn.dominio.ui.screens.tags.ScreenPreferredTags

// «Etiquetas preferidas», dentro del grafo Main y sin barra. En su propio fichero para que
// MainNavGraph.kt, que también edita el compañero, solo tenga una línea más. Se llega desde Ajustes;
// los cambios ya están guardados, así que «atrás» solo vuelve.
internal fun NavGraphBuilder.preferredTagsDestination(navController: NavController) {
    composable<Route.PreferredTags> {
        val tagsViewModel: PreferredTagsViewModel = viewModel(factory = PreferredTagsViewModel.factory(AppContainer.settingsStore))
        ScreenPreferredTags(
            state = tagsViewModel.state,
            actions = PreferredTagsActions(
                onDraftChange = tagsViewModel::onDraftChange,
                onAdd = tagsViewModel::onAdd,
                onRemove = tagsViewModel::onRemove,
                onBack = dropUnlessResumed { navController.popBackStack() },
            ),
        )
    }
}
