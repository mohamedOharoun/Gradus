package es.ulpgc.pamn.dominio.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// El diálogo «Eliminar cuenta» (docs/tareas/ajustes.md, fase 2, tarea 8, decisiones H e I).
class DeleteAccountFormTest {

    private val typed = DeleteAccountForm().withPassword("Gradus2026!")

    @Test
    fun `it starts empty and cannot be sent`() {
        assertEquals("", DeleteAccountForm().password)
        assertFalse(DeleteAccountForm().canSend)
    }

    @Test
    fun `with the password it can be sent`() {
        assertTrue(typed.canSend)
    }

    @Test
    fun `an empty password is required only after trying`() {
        assertNull(DeleteAccountForm().passwordError)
        val tried = DeleteAccountForm().sending()
        assertEquals(FieldError.EMPTY, tried.passwordError)
        assertEquals(Submission.Idle, tried.submission)
    }

    @Test
    fun `sending cannot be sent twice`() {
        val sending = typed.sending()
        assertEquals(Submission.Sending, sending.submission)
        assertFalse(sending.canSend)
    }

    @Test
    fun `a wrong password is told and can be corrected`() {
        val rejected = typed.sending().rejected(DeleteProblem.WRONG_PASSWORD)
        assertEquals(DeleteProblem.WRONG_PASSWORD, rejected.problem)
        assertEquals(Submission.Idle, rejected.submission)
        assertNull(rejected.withPassword("Otra2026!").problem)
        assertTrue(rejected.withPassword("Otra2026!").canSend)
    }

    // Decisión I: siendo autora, cambiar la contraseña no lo arregla; no se puede volver a enviar.
    @Test
    fun `the author of courses cannot try again`() {
        val author = typed.sending().rejected(DeleteProblem.AUTHOR_OF_COURSES)
        assertTrue(author.isAuthorOfCourses)
        assertFalse(author.canSend)
        assertFalse(author.withPassword("Otra2026!").canSend)
    }

    @Test
    fun `after a network failure it can be retried`() {
        val failed = typed.sending().failed(FailureReason.UNREACHABLE)
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), failed.submission)
        assertTrue(failed.canSend)
    }

    @Test
    fun `once deleted it cannot be sent again`() {
        assertFalse(typed.sending().succeeded().canSend)
    }

    @Test
    fun `the password never appears in toString`() {
        assertFalse(typed.toString().contains("Gradus2026!"))
    }
}
