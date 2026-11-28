package es.ulpgc.pamn.dominio.ui.screens.login

// Todo lo que el inicio de sesión puede avisar, junto en un objeto (patrón «objeto parámetro»),
// como RegisterActions. Así ScreenLogin no recibe seis lambdas sueltas, y las previews usan LoginActions().
data class LoginActions(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onCreateAccount: () -> Unit = {},
)
