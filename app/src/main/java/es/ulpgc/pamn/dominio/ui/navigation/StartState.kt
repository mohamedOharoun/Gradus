package es.ulpgc.pamn.dominio.ui.navigation

import es.ulpgc.pamn.dominio.data.session.SessionUpdate

// Destino al salir de Main cuando desaparece la sesión:
// - ToWelcome: salida voluntaria del usuario («Cerrar sesión» o cuenta eliminada) → pila [Welcome].
// - ToLogin: sesión caducada (401) o restaurada sin sesión → pila [Welcome, Login].
enum class MainExit { ToWelcome, ToLogin }

// Qué sabe la app al arrancar (y durante la ejecución) sobre la sesión guardada.
// Lo decide StartViewModel; la interfaz solo reacciona.
sealed interface StartState {

    // Todavía se está leyendo la sesión guardada: solo se pinta el fondo.
    data object Loading : StartState

    // Hay sesión guardada. Sin datos a propósito: ni el token puede salir por aquí.
    data object SignedIn : StartState

    // Sin sesión. `exit` solo importa si la app ya estaba dentro de Main; al arrancar siempre
    // empieza en Bienvenida (StartNavigation.startRoute).
    data class SignedOut(val exit: MainExit = MainExit.ToLogin) : StartState
}

// Función pura: lo guardado (o nada, con su motivo) → estado.
internal fun SessionUpdate.toStartState(): StartState = when (this) {
    is SessionUpdate.Active -> StartState.SignedIn
    is SessionUpdate.Missing -> StartState.SignedOut(if (voluntary) MainExit.ToWelcome else MainExit.ToLogin)
}
