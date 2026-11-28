package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.DeleteAccountForm
import es.ulpgc.pamn.dominio.model.DeleteProblem
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.Submission
import kotlin.test.Test
import kotlin.test.assertEquals

// La respuesta del servidor al eliminar la cuenta, aplicada al diálogo.
class DeleteApplyingOutcomeTest {

    private val sending = DeleteAccountForm().withPassword("Gradus2026!").sending()

    @Test
    fun `deleted is done`() {
        assertEquals(Submission.Succeeded, sending.applying(DeleteAccountOutcome.Deleted).submission)
    }

    @Test
    fun `a wrong password is told`() {
        assertEquals(DeleteProblem.WRONG_PASSWORD, sending.applying(DeleteAccountOutcome.WrongPassword).problem)
    }

    @Test
    fun `the author of courses is told`() {
        assertEquals(DeleteProblem.AUTHOR_OF_COURSES, sending.applying(DeleteAccountOutcome.AuthorOfCourses).problem)
    }

    // La app no envía la contraseña vacía: un 400 es un fallo de la app o del servidor, no del usuario.
    @Test
    fun `a rejection is a server failure`() {
        val rejected = DeleteAccountOutcome.Rejected(listOf(ServerFieldError("password", "REQUIRED")))
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), sending.applying(rejected).submission)
    }

    @Test
    fun `an expired session leaves the form as it was`() {
        assertEquals(sending, sending.applying(DeleteAccountOutcome.SessionExpired))
    }

    @Test
    fun `network and server failures can be retried`() {
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), sending.applying(DeleteAccountOutcome.Unreachable).submission)
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), sending.applying(DeleteAccountOutcome.ServerFailure).submission)
    }
}
