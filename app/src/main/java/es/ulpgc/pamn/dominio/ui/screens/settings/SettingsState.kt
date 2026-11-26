package es.ulpgc.pamn.dominio.ui.screens.settings

import es.ulpgc.pamn.dominio.account.Account

// En qué punto está Ajustes (SettingsViewModel).
sealed interface SettingsState {

    // Leyendo la sesión guardada (un instante).
    data object Loading : SettingsState

    // El perfil de la cuenta. `loggingOut`: ya se pidió cerrar la sesión y se espera al servidor.
    data class Ready(val account: Account, val loggingOut: Boolean = false) : SettingsState

    // Sin sesión: la navegación lleva a Bienvenida.
    data object LoggedOut : SettingsState
}
