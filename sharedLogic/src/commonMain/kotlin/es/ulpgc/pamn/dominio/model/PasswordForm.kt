package es.ulpgc.pamn.dominio.model

// Los dos campos que el servidor puede rechazar al cambiar la contraseña.
enum class PasswordChangeField { CURRENT, NEW }

// Lo que ha rechazado el servidor, ya traducido a la app (account/PasswordApplyingOutcome.kt).
sealed interface PasswordProblem {

    // 403: la contraseña actual no es la de la cuenta.
    data object WrongCurrent : PasswordProblem

    // 400: un campo vacío o una nueva que no cumple las reglas.
    data class InField(val field: PasswordChangeField, val error: FieldError) : PasswordProblem
}

// Estado del formulario «Cambiar contraseña» (docs/tareas/ajustes.md, fase 2, decisión G): la actual,
// la nueva y su repetición. Inmutable, como RegisterForm y LoginForm.
// - Los requisitos de la nueva y si coinciden se ven en vivo (como en el registro).
// - Los errores en rojo, solo después de intentar guardar (isSubmitted).
// - Las contraseñas no se recortan: un espacio forma parte de ellas.
data class PasswordForm(
    val current: String = "",
    val next: String = "",
    val confirmation: String = "",
    val isSubmitted: Boolean = false,
    val submission: Submission = Submission.Idle,
    // Lo último que rechazó el servidor. Se borra al editar cualquier campo.
    val serverProblem: PasswordProblem? = null,
) {
    val unmetRequirements: Set<PasswordRequirement> get() = Validation.unmetRequirements(next)

    // null mientras no se ha escrito la repetición: todavía no hay nada que comparar.
    val passwordsMatch: Boolean? get() = if (confirmation.isEmpty()) null else confirmation == next

    val currentError: FieldError?
        get() = (if (current.isEmpty()) FieldError.EMPTY else null).takeIf { isSubmitted }
            ?: serverErrorFor(PasswordChangeField.CURRENT)

    val nextError: FieldError?
        get() = Validation.validatePassword(next).takeIf { isSubmitted } ?: serverErrorFor(PasswordChangeField.NEW)

    val confirmationError: FieldError?
        get() = Validation.validatePasswordConfirmation(next, confirmation).takeIf { isSubmitted }

    // La actual no es la de la cuenta (lo dice el servidor). La pantalla lo pone bajo «Contraseña actual».
    val isWrongCurrent: Boolean get() = serverProblem == PasswordProblem.WrongCurrent

    val isValid: Boolean
        get() = current.isNotEmpty() && Validation.validatePassword(next) == null &&
            Validation.validatePasswordConfirmation(next, confirmation) == null

    // Válido y sin un envío en curso o terminado.
    val canSend: Boolean get() = isValid && submission.allowsSending

    fun withCurrent(current: String) = copy(current = current, serverProblem = null)
    fun withNext(next: String) = copy(next = next, serverProblem = null)
    fun withConfirmation(confirmation: String) = copy(confirmation = confirmation, serverProblem = null)

    // ── Envío al servidor ──

    // Pulsar «Cambiar contraseña»: si se puede, pasa a enviando; si no, solo muestra los errores.
    fun sending(): PasswordForm =
        if (canSend) copy(isSubmitted = true, submission = Submission.Sending) else copy(isSubmitted = true)

    fun succeeded(): PasswordForm = copy(submission = Submission.Succeeded)

    fun failed(reason: FailureReason): PasswordForm = copy(submission = Submission.Failed(reason))

    fun rejected(problem: PasswordProblem): PasswordForm = copy(submission = Submission.Idle, serverProblem = problem)

    private fun serverErrorFor(field: PasswordChangeField): FieldError? =
        (serverProblem as? PasswordProblem.InField)?.takeIf { it.field == field }?.error

    // El toString generado mostraría las tres contraseñas en los logs.
    override fun toString() =
        "PasswordForm(current=****, next=****, confirmation=****, submission=$submission, serverProblem=$serverProblem)"
}
