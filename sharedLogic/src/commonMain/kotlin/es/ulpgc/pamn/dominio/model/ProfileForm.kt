package es.ulpgc.pamn.dominio.model

// Estado del formulario «Editar perfil» (docs/tareas/ajustes.md, fase 2): solo el nombre (decisión F).
// Inmutable, como LoginForm: cada cambio devuelve una copia nueva.
// `original` es el nombre actual de la cuenta: sin cambios no hay nada que guardar.
// Los errores solo se muestran después de intentar guardar (isSubmitted); los del servidor, en serverError.
data class ProfileForm(
    val original: String,
    val name: String = original,
    val isSubmitted: Boolean = false,
    val submission: Submission = Submission.Idle,
    // Lo último que rechazó el servidor. Se borra al editar el nombre.
    val serverError: FieldError? = null,
) {
    // Las mismas reglas que el registro (Validation.validateName): de 2 a 100, sin contar espacios alrededor.
    val nameError: FieldError?
        get() = Validation.validateName(name).takeIf { isSubmitted } ?: serverError

    // Poner o quitar espacios alrededor no cuenta: el servidor los quita igual.
    val hasChanges: Boolean get() = nameToSend != original.trim()

    val isValid: Boolean get() = Validation.validateName(name) == null

    // Válido, distinto del actual y sin un envío en curso o terminado.
    val canSend: Boolean get() = isValid && hasChanges && submission.allowsSending

    val nameToSend: String get() = name.trim()

    fun withName(name: String) = copy(name = name, serverError = null)
    fun submitted() = copy(isSubmitted = true)

    // ── Envío al servidor ──

    // Pulsar «Guardar»: si se puede, pasa a enviando; si no, solo muestra los errores.
    fun sending(): ProfileForm = if (canSend) copy(isSubmitted = true, submission = Submission.Sending) else submitted()

    fun succeeded(): ProfileForm = copy(submission = Submission.Succeeded)

    fun failed(reason: FailureReason): ProfileForm = copy(submission = Submission.Failed(reason))

    // El servidor no acepta el nombre: se muestra debajo y se puede corregir.
    fun rejected(error: FieldError): ProfileForm = copy(submission = Submission.Idle, serverError = error)
}
