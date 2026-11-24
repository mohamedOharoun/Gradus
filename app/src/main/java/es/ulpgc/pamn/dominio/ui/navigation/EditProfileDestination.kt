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
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.screens.profile.EditProfileActions
import es.ulpgc.pamn.dominio.ui.screens.profile.EditProfileState
import es.ulpgc.pamn.dominio.ui.screens.profile.EditProfileViewModel
import es.ulpgc.pamn.dominio.ui.screens.profile.ScreenEditProfile

// «Editar perfil», dentro del grafo Main y sin barra (como la ficha de un curso). En su propio fichero
// para que MainNavGraph.kt, que también edita el compañero, solo tenga una línea más.
// Se llega desde Ajustes; al guardar, o con «atrás», vuelve a Ajustes.
internal fun NavGraphBuilder.editProfileDestination(navController: NavController) {
    composable<Route.EditProfile> {
        val profileViewModel: EditProfileViewModel = viewModel(
            factory = EditProfileViewModel.factory(AppContainer.accountGateway, AppContainer.sessionStore),
        )
        when (val state = profileViewModel.state) {
            // Un instante, mientras se lee la sesión: solo el fondo.
            EditProfileState.Loading -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
            is EditProfileState.Editing -> {
                // Guardado (y la sesión del móvil ya tiene el nombre nuevo): de vuelta a Ajustes.
                if (state.form.submission == Submission.Succeeded) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
                ScreenEditProfile(
                    email = state.email,
                    form = state.form,
                    actions = EditProfileActions(
                        onNameChange = profileViewModel::onNameChange,
                        onSave = dropUnlessResumed { profileViewModel.onSave() },
                        onBack = dropUnlessResumed { navController.popBackStack() },
                    ),
                )
            }
            EditProfileState.LoggedOut -> LaunchedEffect(Unit) { navController.leaveToWelcome() }
        }
    }
}
