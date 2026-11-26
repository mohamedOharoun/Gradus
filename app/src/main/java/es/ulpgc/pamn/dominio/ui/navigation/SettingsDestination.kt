package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.ScreenSettings
import es.ulpgc.pamn.dominio.ui.screens.settings.DeleteAccountActions
import es.ulpgc.pamn.dominio.ui.screens.settings.DeleteAccountDialog
import es.ulpgc.pamn.dominio.ui.screens.settings.DeleteAccountState
import es.ulpgc.pamn.dominio.ui.screens.settings.DeleteAccountViewModel
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsActions
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsScreenState
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsState
import es.ulpgc.pamn.dominio.ui.screens.settings.LanguageDialog
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsViewModel

// La pestaña Ajustes conectada (ajustes.md, tarea 6). Va en su propio fichero para que en
// MainNavGraph.kt, que también edita el compañero, solo cambie la línea de Ajustes.
@Composable
internal fun SettingsRoute(navController: NavController, modifier: Modifier = Modifier) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            AppContainer.accountGateway, AppContainer.sessionStore, AppContainer.settingsStore, AppContainer.languageSetting,
        ),
    )
    // El diálogo «Eliminar cuenta» tiene su propio ViewModel (vive con la pestaña, como el de Ajustes).
    val deleteViewModel: DeleteAccountViewModel = viewModel(
        factory = DeleteAccountViewModel.factory(AppContainer.accountGateway, AppContainer.sessionStore),
    )
    val appSettings by settingsViewModel.appSettings.collectAsStateWithLifecycle()
    // Decisión B: el interruptor enseña cómo está la app ahora (lo elegido o, si no, el sistema).
    val dark = appSettings.isDark(systemDark = isSystemInDarkTheme())
    // Al volver de «Editar perfil» (o de otra pestaña) se vuelve a leer la sesión: el nombre puede haber cambiado.
    LifecycleResumeEffect(settingsViewModel) {
        settingsViewModel.onResume()
        onPauseOrDispose {}
    }

    when (val state = settingsViewModel.state) {
        // Un instante, mientras se lee la sesión: solo el fondo.
        SettingsState.Loading -> Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        is SettingsState.Ready -> ScreenSettings(
            state = SettingsScreenState(
                state.account.name, state.account.email, darkMode = dark,
                preferredTags = appSettings.preferredTags.values,
                language = settingsViewModel.language,
                canChooseLanguage = settingsViewModel.canChooseLanguage,
            ),
            actions = SettingsActions(
                onDarkModeChange = settingsViewModel::onDarkModeChange,
                onLogOut = dropUnlessResumed { settingsViewModel.onLogOut() },
                onEditProfile = dropUnlessResumed { navController.navigate(Route.EditProfile) },
                onChangePassword = dropUnlessResumed { navController.navigate(Route.ChangePassword) },
                onDeleteAccount = deleteViewModel::onOpen,
                onEditTags = dropUnlessResumed { navController.navigate(Route.PreferredTags) },
                onChooseLanguage = settingsViewModel::onOpenLanguage,
            ),
            modifier = modifier,
        )
        SettingsState.LoggedOut -> LaunchedEffect(Unit) { navController.leaveToWelcome() }
    }

    // Encima de Ajustes: elegir el idioma (fase 3b). Al elegir otro, Android vuelve a crear la pantalla en él.
    if (settingsViewModel.languagePickerOpen) {
        LanguageDialog(
            selected = settingsViewModel.language,
            onChoose = settingsViewModel::onLanguageChosen,
            onDismiss = settingsViewModel::onDismissLanguage,
        )
    }

    // Encima de Ajustes. Cuenta eliminada: a Bienvenida sin poder volver atrás (decisión H).
    when (val delete = deleteViewModel.state) {
        DeleteAccountState.Closed -> Unit
        is DeleteAccountState.Open -> DeleteAccountDialog(
            form = delete.form,
            actions = DeleteAccountActions(
                onPasswordChange = deleteViewModel::onPasswordChange,
                onConfirm = deleteViewModel::onConfirm,
                onDismiss = deleteViewModel::onDismiss,
            ),
        )
        DeleteAccountState.LoggedOut -> LaunchedEffect(Unit) { navController.leaveToWelcome() }
    }
}

// Sesión cerrada: a Bienvenida y se borra todo lo de dentro (Inicio, pestañas, cursos), así que
// «atrás» desde Bienvenida cierra la app en vez de volver a Ajustes.
internal fun NavController.leaveToWelcome() {
    navigate(Route.Welcome) {
        popUpTo<Route.Main> { inclusive = true }
    }
}
