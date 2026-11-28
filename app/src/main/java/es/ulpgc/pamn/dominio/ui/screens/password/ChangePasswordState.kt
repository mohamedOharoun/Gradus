package es.ulpgc.pamn.dominio.ui.screens.password

import es.ulpgc.pamn.dominio.model.PasswordForm

// En qué punto está «Cambiar contraseña» (ChangePasswordViewModel). No hay «cargando»: el formulario
// empieza vacío y la sesión solo hace falta al enviar.
sealed interface ChangePasswordState {

    // El formulario; con submission = Succeeded, la pantalla enseña que se ha cambiado.
    data class Editing(val form: PasswordForm) : ChangePasswordState

    // Sin sesión, o la sesión ya no vale: la navegación lleva a Bienvenida.
    data object LoggedOut : ChangePasswordState
}

// Lo que se puede hacer en la pantalla.
class ChangePasswordActions(
    val onCurrentChange: (String) -> Unit = {},
    val onNextChange: (String) -> Unit = {},
    val onConfirmationChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onBack: () -> Unit = {},
)
