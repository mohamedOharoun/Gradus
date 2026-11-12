package es.ulpgc.pamn.dominio.model

// Estado del formulario de inicio de sesión. Inmutable: cada cambio devuelve una copia nueva.
// Los errores solo se muestran cuando ya se ha pulsado «Entrar» (isSubmitted).
data class LoginForm(
    val email: String = "",
    val password: String = "",
    val isSubmitted: Boolean = false,
) {
    val emailError: FieldError?
        get() = Validation.validateEmail(email).takeIf { isSubmitted }

    // Al entrar no se exigen los requisitos del registro: basta con que no esté vacía.
    val passwordError: FieldError?
        get() = passwordProblem.takeIf { isSubmitted }

    val isValid: Boolean
        get() = Validation.validateEmail(email) == null && passwordProblem == null

    private val passwordProblem: FieldError?
        get() = if (password.isEmpty()) FieldError.EMPTY else null

    fun withEmail(email: String) = copy(email = email)
    fun withPassword(password: String) = copy(password = password)
    fun submitted() = copy(isSubmitted = true)
}
