package es.ulpgc.pamn.dominio.model

// Estado del registro por pasos. Inmutable: cada acción devuelve un formulario nuevo.
// Los errores de un paso solo se ven tras pulsar «Siguiente» en ese paso.
// En el paso de la contraseña no hay errores: los requisitos y la coincidencia se ven al escribir.
data class RegisterForm(
    val name: String = "",
    val email: String = "",
    val step: RegisterStep = RegisterStep.NAME,
    val isStepSubmitted: Boolean = false,
    val password: String = "",
    val confirmation: String = "",
    val acceptedTerms: Boolean = false,
) {
    val nameError: FieldError? get() = errorOf(RegisterStep.NAME)
    val emailError: FieldError? get() = errorOf(RegisterStep.EMAIL)

    val unmetRequirements: Set<PasswordRequirement> get() = Validation.unmetRequirements(password)

    // null mientras no se ha escrito la repetición: todavía no hay nada que comparar.
    val passwordsMatch: Boolean? get() = if (confirmation.isEmpty()) null else confirmation == password

    // Todo válido, en el último paso y con los términos aceptados.
    val canRegister: Boolean
        get() = step == RegisterStep.PASSWORD &&
            RegisterStep.entries.all { problemIn(it) == null } &&
            acceptedTerms

    fun withName(name: String) = copy(name = name)
    fun withEmail(email: String) = copy(email = email)
    fun withPassword(password: String) = copy(password = password)
    fun withConfirmation(confirmation: String) = copy(confirmation = confirmation)
    fun withTermsAccepted(accepted: Boolean) = copy(acceptedTerms = accepted)

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

    private fun errorOf(target: RegisterStep): FieldError? =
        problemIn(target).takeIf { step == target && isStepSubmitted }

    private fun problemIn(target: RegisterStep): FieldError? = when (target) {
        RegisterStep.NAME -> Validation.validateName(name)
        RegisterStep.EMAIL -> Validation.validateEmail(email)
        RegisterStep.PASSWORD ->
            Validation.validatePassword(password) ?: Validation.validatePasswordConfirmation(password, confirmation)
    }
}
