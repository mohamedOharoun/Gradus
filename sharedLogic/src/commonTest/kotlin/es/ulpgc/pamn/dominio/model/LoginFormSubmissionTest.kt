package es.ulpgc.pamn.dominio.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginFormSubmissionTest {

    private val valid = LoginForm(email = "ana@ulpgc.es", password = "Gradus2026!")
    private val wrongCredentials = LoginProblem(field = null, error = FieldError.INVALID_CREDENTIALS)

    // ── Enviar ──

    @Test
    fun `a new form is idle and can be sent once valid`() {
        assertEquals(Submission.Idle, LoginForm().submission)
        assertFalse(LoginForm().canSend)
        assertTrue(valid.canSend)
    }

    @Test
    fun `sending a valid form marks it as sending`() {
        assertEquals(Submission.Sending, valid.sending().submission)
    }

    @Test
    fun `sending an invalid form does not send and shows its errors`() {
        val form = valid.withEmail("ana@").sending()
        assertEquals(Submission.Idle, form.submission)
        assertEquals(FieldError.INVALID_EMAIL, form.emailError)
    }

    @Test
    fun `while sending it cannot be sent again`() {
        val sending = valid.sending()
        assertFalse(sending.canSend)
        assertEquals(sending, sending.sending())
    }

    @Test
    fun `after logging in it cannot be sent again`() {
        assertFalse(valid.sending().succeeded().canSend)
    }

    @Test
    fun `a failed attempt keeps the data and can be retried`() {
        val failed = valid.sending().failed(FailureReason.UNREACHABLE)
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), failed.submission)
        assertEquals("ana@ulpgc.es", failed.email)
        assertTrue(failed.canSend)
    }

    // ── Errores del servidor ──

    @Test
    fun `wrong credentials are a general error, not an error of one field`() {
        val form = valid.sending().rejected(wrongCredentials)
        assertEquals(FieldError.INVALID_CREDENTIALS, form.generalError)
        assertNull(form.emailError)
        assertNull(form.passwordError)
        assertEquals(Submission.Idle, form.submission)
    }

    @Test
    fun `a field error from the server is shown under that field`() {
        val form = valid.sending().rejected(LoginProblem(LoginField.PASSWORD, FieldError.EMPTY))
        assertEquals(FieldError.EMPTY, form.passwordError)
        assertNull(form.emailError)
        assertNull(form.generalError)
    }

    @Test
    fun `the local error wins over the one from the server`() {
        val form = valid.withEmail("ana@").submitted().rejected(LoginProblem(LoginField.EMAIL, FieldError.EMPTY))
        assertEquals(FieldError.INVALID_EMAIL, form.emailError)
    }

    @Test
    fun `editing the email clears the error from the server`() {
        assertNull(valid.rejected(wrongCredentials).withEmail("otra@ulpgc.es").generalError)
    }

    @Test
    fun `editing the password clears the error from the server`() {
        assertNull(valid.rejected(wrongCredentials).withPassword("Otra2026!").generalError)
    }

    // Con las credenciales incorrectas se puede volver a intentar (por ejemplo, tras corregir una errata).
    @Test
    fun `after wrong credentials the form can be sent again`() {
        assertTrue(valid.rejected(wrongCredentials).canSend)
    }
}
