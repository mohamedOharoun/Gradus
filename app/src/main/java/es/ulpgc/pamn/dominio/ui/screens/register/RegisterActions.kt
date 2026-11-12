package es.ulpgc.pamn.dominio.ui.screens.register

import es.ulpgc.pamn.dominio.model.RegisterStep

// Todo lo que el registro puede avisar, junto en un objeto (patrón «objeto parámetro»).
// Así ScreenRegister no recibe once lambdas sueltas, y las previews usan RegisterActions().
data class RegisterActions(
    val onNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onConfirmationChange: (String) -> Unit = {},
    val onTermsChange: (Boolean) -> Unit = {},
    val onNext: () -> Unit = {},
    val onRegister: () -> Unit = {},
    val onEdit: (RegisterStep) -> Unit = {},
    val onBack: () -> Unit = {},
    val onLogin: () -> Unit = {},
)
