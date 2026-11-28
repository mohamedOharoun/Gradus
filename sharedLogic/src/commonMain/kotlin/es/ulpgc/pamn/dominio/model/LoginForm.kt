package es.ulpgc.pamn.dominio.model

// Estado del formulario de inicio de sesión. Inmutable: cada cambio devuelve una copia nueva.
// Los errores solo se muestran cuando ya se ha pulsado «Entrar» (isSubmitted).
// Los del servidor van en serverProblem: debajo de su campo, o en generalError si son del formulario.
data class LoginForm(
    val email: String = "",
    val password: String = "",
    val isSubmitted: Boolean = false,
    val submission: Submission = Submission.Idle,
    // Lo último que rechazó el servidor. Se borra al editar cualquiera de los dos campos.
    val serverProblem: LoginProblem? = null,
) {
    // Primero el error que detecta la app; si no hay, el que haya devuelto el servidor para ese campo.
    val emailError: FieldError?
        get() = Validation.validateEmail(email).takeIf { isSubmitted } ?: serverErrorFor(LoginField.EMAIL)

    // Al entrar no se exigen los requisitos del registro: basta con que no esté vacía.
    val passwordError: FieldError?
        get() = passwordProblem.takeIf { isSubmitted } ?: serverErrorFor(LoginField.PASSWORD)

    // Error de todo el formulario (p. ej. correo o contraseña incorrectos).
    val generalError: FieldError? get() = serverProblem?.takeIf { it.field == null }?.error

    val isValid: Boolean
        get() = Validation.validateEmail(email) == null && passwordProblem == null

    // Válido y sin un envío en curso o terminado.
    val canSend: Boolean get() = isValid && submission.allowsSending

    private val passwordProblem: FieldError?
        get() = if (password.isEmpty()) FieldError.EMPTY else null

    fun withEmail(email: String) = copy(email = email, serverProblem = null)
    fun withPassword(password: String) = copy(password = password, serverProblem = null)
    fun submitted() = copy(isSubmitted = true)

    // ── Envío al servidor ──

    // Pulsar «Entrar»: si se puede, pasa a enviando; si no, solo muestra los errores.
    fun sending(): LoginForm = if (canSend) copy(isSubmitted = true, submission = Submission.Sending) else submitted()

    fun succeeded(): LoginForm = copy(submission = Submission.Succeeded)

    fun failed(reason: FailureReason): LoginForm = copy(submission = Submission.Failed(reason))

    // El servidor no acepta los datos: se muestra el problema y se puede volver a intentar.
    fun rejected(problem: LoginProblem): LoginForm = copy(submission = Submission.Idle, serverProblem = problem)

    private fun serverErrorFor(field: LoginField): FieldError? =
        serverProblem?.takeIf { it.field == field }?.error
}
