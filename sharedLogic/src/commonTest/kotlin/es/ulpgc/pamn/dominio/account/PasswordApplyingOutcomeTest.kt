package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.PasswordForm
import es.ulpgc.pamn.dominio.model.Submission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// La respuesta del servidor al cambiar la contraseña, aplicada al formulario.
class PasswordApplyingOutcomeTest {

    private val sending = PasswordForm()
        .withCurrent("Gradus2026!").withNext("Nueva2026?").withConfirmation("Nueva2026?")
        .sending()

    private fun rejectedWith(vararg errors: Pair<String, String>) =
        sending.applying(ChangePasswordOutcome.Rejected(errors.map { (field, code) -> ServerFieldError(field, code) }))

    @Test
    fun `changed is done`() {
        assertEquals(Submission.Succeeded, sending.applying(ChangePasswordOutcome.Changed).submission)
    }

    @Test
    fun `a wrong current password is told under it`() {
        val form = sending.applying(ChangePasswordOutcome.WrongPassword)
        assertTrue(form.isWrongCurrent)
        assertEquals(Submission.Idle, form.submission)
    }

    @Test
    fun `a missing current password is required`() {
        assertEquals(FieldError.EMPTY, rejectedWith("currentPassword" to "REQUIRED").currentError)
    }

    // Cualquier regla de la nueva que no se cumpla es «no cumple los requisitos», como en el registro.
    @Test
    fun `any rule of the new password is a weak password`() {
        listOf("TOO_SHORT", "MISSING_LETTER", "MISSING_DIGIT", "MISSING_SPECIAL_CHARACTER").forEach { code ->
            assertEquals(FieldError.WEAK_PASSWORD, rejectedWith("newPassword" to code).nextError, code)
        }
        assertEquals(FieldError.EMPTY, rejectedWith("newPassword" to "REQUIRED").nextError)
    }

    // Se enseña el del primer campo de la pantalla (la actual va antes que la nueva).
    @Test
    fun `with errors in both fields the current one goes first`() {
        val form = rejectedWith("newPassword" to "TOO_SHORT", "currentPassword" to "REQUIRED")
        assertEquals(FieldError.EMPTY, form.currentError)
    }

    @Test
    fun `a rejection the app does not understand is a server failure`() {
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), rejectedWith("newPassword" to "SOMETHING_NEW").submission)
    }

    @Test
    fun `an expired session leaves the form as it was`() {
        assertEquals(sending, sending.applying(ChangePasswordOutcome.SessionExpired))
    }

    @Test
    fun `network and server failures can be retried`() {
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), sending.applying(ChangePasswordOutcome.Unreachable).submission)
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), sending.applying(ChangePasswordOutcome.ServerFailure).submission)
    }
}
