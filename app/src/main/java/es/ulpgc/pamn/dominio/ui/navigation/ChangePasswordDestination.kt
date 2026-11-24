package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.password.ChangePasswordActions
import es.ulpgc.pamn.dominio.ui.screens.password.ChangePasswordState
import es.ulpgc.pamn.dominio.ui.screens.password.ChangePasswordViewModel
import es.ulpgc.pamn.dominio.ui.screens.password.ScreenChangePassword

// «Cambiar contraseña», dentro del grafo Main y sin barra. En su propio fichero para que MainNavGraph.kt,
// que también edita el compañero, solo tenga una línea más. Se llega desde Ajustes y vuelve a Ajustes
// con «atrás» o con «Volver a Ajustes» (tras cambiarla).
internal fun NavGraphBuilder.changePasswordDestination(navController: NavController) {
    composable<Route.ChangePassword> {
        val passwordViewModel: ChangePasswordViewModel = viewModel(
            factory = ChangePasswordViewModel.factory(AppContainer.accountGateway, AppContainer.sessionStore),
        )
        when (val state = passwordViewModel.state) {
            is ChangePasswordState.Editing -> ScreenChangePassword(
                form = state.form,
                actions = ChangePasswordActions(
                    onCurrentChange = passwordViewModel::onCurrentChange,
                    onNextChange = passwordViewModel::onNextChange,
                    onConfirmationChange = passwordViewModel::onConfirmationChange,
                    onSave = dropUnlessResumed { passwordViewModel.onSave() },
                    onBack = dropUnlessResumed { navController.popBackStack() },
                ),
            )
            ChangePasswordState.LoggedOut -> LaunchedEffect(Unit) { navController.leaveToWelcome() }
        }
    }
}
