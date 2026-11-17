package es.ulpgc.pamn.dominio.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegisterFormSubmissionTest {

    private val ready = RegisterForm(
        name = "Ana",
        email = "ana@ulpgc.es",
        step = RegisterStep.PASSWORD,
        password = "secreta1!",
        confirmation = "secreta1!",
        acceptedTerms = true,
    )
    private val emailTaken = RegisterProblem(RegisterStep.EMAIL, FieldError.EMAIL_TAKEN)

    // ── Enviar ──

    @Test
    fun `a new form has not been sent`() {
        assertEquals(Submission.Idle, RegisterForm().submission)
    }

    @Test
    fun `sending a ready form marks it as sending and it cannot be sent again`() {
        val sending = ready.sending()
        assertEquals(Submission.Sending, sending.submission)
        assertFalse(sending.canRegister)
    }

    @Test
    fun `a form that is not ready is not sent`() {
        val notReady = ready.withTermsAccepted(false)
        assertSame(notReady, notReady.sending())
    }

    @Test
    fun `after succeeding it cannot be sent again`() {
        val done = ready.sending().succeeded()
        assertEquals(Submission.Succeeded, done.submission)
        assertFalse(done.canRegister)
    }

    // ── Fallos de red o del servidor ──

    @Test
    fun `a failure keeps the step and the data and allows trying again`() {
        val failed = ready.sending().failed(FailureReason.UNREACHABLE)
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), failed.submission)
        assertEquals(RegisterStep.PASSWORD, failed.step)
        assertEquals("ana@ulpgc.es", failed.email)
        assertTrue(failed.canRegister)
    }

    // ── El servidor rechaza un dato ──

    @Test
    fun `a rejection goes back to the step of the problem and shows it`() {
        val rejected = ready.sending().rejected(emailTaken)
        assertEquals(RegisterStep.EMAIL, rejected.step)
        assertEquals(FieldError.EMAIL_TAKEN, rejected.emailError)
        assertEquals(FieldError.EMAIL_TAKEN, rejected.serverError)
        assertEquals(Submission.Idle, rejected.submission)
    }

    @Test
    fun `with the server problem unsolved the step does not move forward`() {
        val rejected = ready.sending().rejected(emailTaken)
        assertEquals(RegisterStep.EMAIL, rejected.next().step)
    }

    @Test
    fun `editing the field of the problem clears it and lets the form go on`() {
        val edited = ready.sending().rejected(emailTaken).withEmail("ana2@ulpgc.es")
        assertNull(edited.serverError)
        assertNull(edited.emailError)
        assertEquals(RegisterStep.PASSWORD, edited.next().step)
    }

    @Test
    fun `editing another field keeps the server problem`() {
        val rejected = ready.sending().rejected(emailTaken)
        assertEquals(FieldError.EMAIL_TAKEN, rejected.withName("Ana María").serverError)
    }

    @Test
    fun `a problem in the password step is shown there and cleared by editing that step`() {
        val terms = RegisterProblem(RegisterStep.PASSWORD, FieldError.TERMS_NOT_ACCEPTED)
        val rejected = ready.sending().rejected(terms)
        assertEquals(RegisterStep.PASSWORD, rejected.step)
        assertEquals(FieldError.TERMS_NOT_ACCEPTED, rejected.serverError)
        assertFalse(rejected.canRegister)
        assertNull(rejected.withTermsAccepted(true).serverError)
        assertNull(rejected.withPassword("secreta2!").serverError)
    }

    @Test
    fun `the server error is only shown on its own step`() {
        val rejected = ready.sending().rejected(emailTaken)
        assertNull(rejected.goBackTo(RegisterStep.NAME).serverError)
    }
}
