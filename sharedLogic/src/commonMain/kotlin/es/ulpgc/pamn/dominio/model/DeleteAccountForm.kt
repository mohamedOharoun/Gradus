package es.ulpgc.pamn.dominio.model

// Lo que el servidor puede rechazar al eliminar la cuenta (account/DeleteApplyingOutcome.kt).
enum class DeleteProblem {
    // 403: la contraseña no es la de la cuenta. Se puede corregir.
    WRONG_PASSWORD,

    // 409: ha publicado cursos (decisión I). No depende de la contraseña: no se puede volver a intentar.
    AUTHOR_OF_COURSES,
}

// Estado del diálogo «Eliminar cuenta» (docs/tareas/ajustes.md, fase 2, decisión H): la contraseña
// confirma que es su dueño quien lo pide. Inmutable, como los demás formularios.
data class DeleteAccountForm(
    val password: String = "",
    val isSubmitted: Boolean = false,
    val submission: Submission = Submission.Idle,
    val problem: DeleteProblem? = null,
) {
    val passwordError: FieldError? get() = FieldError.EMPTY.takeIf { isSubmitted && password.isEmpty() }

    val isAuthorOfCourses: Boolean get() = problem == DeleteProblem.AUTHOR_OF_COURSES

    // Con la contraseña escrita, sin un envío en curso o terminado, y sin ser autora (eso no cambia).
    val canSend: Boolean get() = password.isNotEmpty() && submission.allowsSending && !isAuthorOfCourses

    // Escribir borra «contraseña incorrecta», pero no «es autora»: eso no depende de la contraseña.
    fun withPassword(password: String) =
        copy(password = password, problem = problem.takeIf { it == DeleteProblem.AUTHOR_OF_COURSES })

    // Pulsar «Eliminar»: si se puede, pasa a enviando; si no, solo muestra los errores.
    fun sending(): DeleteAccountForm =
        if (canSend) copy(isSubmitted = true, submission = Submission.Sending) else copy(isSubmitted = true)

    fun succeeded(): DeleteAccountForm = copy(submission = Submission.Succeeded)

    fun failed(reason: FailureReason): DeleteAccountForm = copy(submission = Submission.Failed(reason))

    fun rejected(problem: DeleteProblem): DeleteAccountForm = copy(submission = Submission.Idle, problem = problem)

    // El toString generado mostraría la contraseña en los logs.
    override fun toString() = "DeleteAccountForm(password=****, submission=$submission, problem=$problem)"
}
