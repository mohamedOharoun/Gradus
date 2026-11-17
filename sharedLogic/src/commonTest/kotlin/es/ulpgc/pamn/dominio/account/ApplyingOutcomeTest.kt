package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterStep
import es.ulpgc.pamn.dominio.model.Submission
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplyingOutcomeTest {

    private val sending = RegisterForm(
        name = "Ana",
        email = "ana@ulpgc.es",
        step = RegisterStep.PASSWORD,
        password = "secreta1!",
        confirmation = "secreta1!",
        acceptedTerms = true,
    ).sending()

    @Test
    fun `registered marks the form as succeeded`() {
        val account = Account("id-1", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z")
        assertEquals(Submission.Succeeded, sending.applying(RegistrationOutcome.Registered(account)).submission)
    }

    @Test
    fun `rejected goes to the earliest step with a problem`() {
        val outcome = RegistrationOutcome.Rejected(
            listOf(ServerFieldError("password", "MISSING_DIGIT"), ServerFieldError("name", "TOO_SHORT")),
        )
        val form = sending.applying(outcome)
        assertEquals(RegisterStep.NAME, form.step)
        assertEquals(FieldError.TOO_SHORT, form.nameError)
    }

    @Test
    fun `rejected without any field problem is a server failure`() {
        val outcome = RegistrationOutcome.Rejected(listOf(ServerFieldError("body", "MALFORMED_BODY")))
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), sending.applying(outcome).submission)
    }

    @Test
    fun `email taken goes back to the email step`() {
        val form = sending.applying(RegistrationOutcome.EmailTaken)
        assertEquals(RegisterStep.EMAIL, form.step)
        assertEquals(FieldError.EMAIL_TAKEN, form.emailError)
    }

    @Test
    fun `unreachable is a failure that can be retried`() {
        assertEquals(
            Submission.Failed(FailureReason.UNREACHABLE),
            sending.applying(RegistrationOutcome.Unreachable).submission,
        )
    }

    @Test
    fun `server failure is a failure of the server`() {
        assertEquals(
            Submission.Failed(FailureReason.SERVER_FAILURE),
            sending.applying(RegistrationOutcome.ServerFailure).submission,
        )
    }
}
