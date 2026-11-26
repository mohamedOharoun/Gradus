package es.ulpgc.pamn.dominio.ui.navigation

import es.ulpgc.pamn.dominio.account.Session

// Qué sabe la app al arrancar sobre la sesión. Lo decide StartViewModel; la interfaz solo reacciona.
sealed interface StartState {

    // Todavía se está leyendo la sesión guardada: solo se pinta el fondo.
    data object Loading : StartState

    // Hay sesión guardada. Sin datos a propósito: ni el token puede salir por aquí.
    data object SignedIn : StartState

    data object SignedOut : StartState
}

// Función pura: lo guardado (o nada) → estado.
internal fun Session?.toStartState(): StartState =
    if (this != null) StartState.SignedIn else StartState.SignedOut
