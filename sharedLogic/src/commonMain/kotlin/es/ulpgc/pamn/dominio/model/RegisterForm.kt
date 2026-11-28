package es.ulpgc.pamn.dominio.model

// Estado del registro por pasos. Inmutable: cada acción devuelve un formulario nuevo.
// Los errores de un paso solo se ven tras pulsar «Siguiente» en ese paso.
// En el paso de la contraseña no hay errores propios: los requisitos y la coincidencia se ven al
// escribir; solo se muestra ahí lo que rechace el servidor (serverError).
data class RegisterForm(
    val name: String = "",
    val email: String = "",
    val step: RegisterStep = RegisterStep.NAME,
    val isStepSubmitted: Boolean = false,
    val password: String = "",
    val confirmation: String = "",
    val acceptedTerms: Boolean = false,
    val submission: Submission = Submission.Idle,
    // Lo último que rechazó el servidor. Se borra al editar un dato de su paso.
    val serverProblem: RegisterProblem? = null,
) {
    val nameError: FieldError? get() = errorOf(RegisterStep.NAME)
    val emailError: FieldError? get() = errorOf(RegisterStep.EMAIL)

    // El error del servidor, solo mientras se está en su paso.
    val serverError: FieldError? get() = serverProblem?.takeIf { it.step == step }?.error

    val unmetRequirements: Set<PasswordRequirement> get() = Validation.unmetRequirements(password)

    // null mientras no se ha escrito la repetición: todavía no hay nada que comparar.
    val passwordsMatch: Boolean? get() = if (confirmation.isEmpty()) null else confirmation == password

    // Todo válido, en el último paso, con los términos aceptados y sin un envío en curso o terminado.
    val canRegister: Boolean
        get() = step == RegisterStep.PASSWORD &&
            RegisterStep.entries.all { problemIn(it) == null } &&
            acceptedTerms &&
            submission.allowsSending

    fun withName(name: String) = copy(name = name).clearingServerProblemIn(RegisterStep.NAME)
    fun withEmail(email: String) = copy(email = email).clearingServerProblemIn(RegisterStep.EMAIL)
    fun withPassword(password: String) = copy(password = password).clearingServerProblemIn(RegisterStep.PASSWORD)
    fun withConfirmation(confirmation: String) =
        copy(confirmation = confirmation).clearingServerProblemIn(RegisterStep.PASSWORD)
    fun withTermsAccepted(accepted: Boolean) =
        copy(acceptedTerms = accepted).clearingServerProblemIn(RegisterStep.PASSWORD)

    // Si el paso es válido avanza (con los errores ocultos); si no, se queda y muestra el error.
    fun next(): RegisterForm {
        val nextStep = step.next ?: return this
        return if (problemIn(step) == null) copy(step = nextStep, isStepSubmitted = false)
        else copy(isStepSubmitted = true)
    }

    // null significa «no hay paso anterior»: quien lo use decide salir del registro.
    fun back(): RegisterForm? = step.previous?.let { goBackTo(it) }

    // Vuelve a un paso ya hecho (para editar su dato). Hacia delante solo se va con next().
    fun goBackTo(target: RegisterStep): RegisterForm =
        if (target < step) copy(step = target, isStepSubmitted = false) else this

    // ── Envío al servidor ──

    // Solo se envía un formulario listo; si no, no cambia nada.
    fun sending(): RegisterForm = if (canRegister) copy(submission = Submission.Sending) else this

    fun succeeded(): RegisterForm = copy(submission = Submission.Succeeded)

    fun failed(reason: FailureReason): RegisterForm = copy(submission = Submission.Failed(reason))

    // Vuelve al paso del problema y lo muestra, como si se hubiera pulsado «Siguiente» en él.
    fun rejected(problem: RegisterProblem): RegisterForm = copy(
        step = problem.step,
        isStepSubmitted = true,
        submission = Submission.Idle,
        serverProblem = problem,
    )

    private fun clearingServerProblemIn(target: RegisterStep): RegisterForm =
        if (serverProblem?.step == target) copy(serverProblem = null) else this

    private fun errorOf(target: RegisterStep): FieldError? =
        problemIn(target).takeIf { step == target && isStepSubmitted }

    // Primero los errores que detecta la app; si no hay, el que haya devuelto el servidor para ese paso.
    private fun problemIn(target: RegisterStep): FieldError? =
        localProblemIn(target) ?: serverProblem?.takeIf { it.step == target }?.error

    private fun localProblemIn(target: RegisterStep): FieldError? = when (target) {
        RegisterStep.NAME -> Validation.validateName(name)
        RegisterStep.EMAIL -> Validation.validateEmail(email)
        RegisterStep.PASSWORD ->
            Validation.validatePassword(password) ?: Validation.validatePasswordConfirmation(password, confirmation)
    }
}
