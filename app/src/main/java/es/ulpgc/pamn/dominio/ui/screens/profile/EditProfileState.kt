package es.ulpgc.pamn.dominio.ui.screens.profile

import es.ulpgc.pamn.dominio.model.ProfileForm

// En qué punto está «Editar perfil» (EditProfileViewModel).
sealed interface EditProfileState {

    // Leyendo la sesión guardada (un instante).
    data object Loading : EditProfileState

    // El formulario. El correo se enseña, pero no se puede cambiar (decisión F).
    data class Editing(val email: String, val form: ProfileForm) : EditProfileState

    // Sin sesión, o la sesión ya no vale: la navegación lleva a Bienvenida.
    data object LoggedOut : EditProfileState
}

// Lo que se puede hacer en la pantalla.
class EditProfileActions(
    val onNameChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onBack: () -> Unit = {},
)
