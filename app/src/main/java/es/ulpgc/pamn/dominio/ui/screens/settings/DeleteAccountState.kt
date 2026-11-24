package es.ulpgc.pamn.dominio.ui.screens.settings

import es.ulpgc.pamn.dominio.model.DeleteAccountForm

// En qué punto está el diálogo «Eliminar cuenta» de Ajustes (DeleteAccountViewModel).
sealed interface DeleteAccountState {

    // Sin diálogo.
    data object Closed : DeleteAccountState

    // El diálogo abierto, con la contraseña que confirma el borrado.
    data class Open(val form: DeleteAccountForm) : DeleteAccountState

    // Cuenta eliminada (o la sesión ya no valía): la sesión del móvil se ha borrado y la navegación
    // lleva a Bienvenida.
    data object LoggedOut : DeleteAccountState
}

// Lo que se puede hacer en el diálogo.
class DeleteAccountActions(
    val onPasswordChange: (String) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)
