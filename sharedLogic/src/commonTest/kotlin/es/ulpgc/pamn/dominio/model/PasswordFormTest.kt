package es.ulpgc.pamn.dominio.model

import es.ulpgc.pamn.dominio.account.PasswordChange
import es.ulpgc.pamn.dominio.account.toChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// El formulario de «Cambiar contraseña» (docs/tareas/ajustes.md, fase 2, tarea 7, decisión G).
class PasswordFormTest {

    private val valid = PasswordForm()
        .withCurrent("Gradus2026!")
        .withNext("Nueva2026?")
        .withConfirmation("Nueva2026?")

    // ── Las reglas en vivo de la nueva (las del registro) ──

    @Test
    fun `the requirements of the new password are checked while typing`() {
        assertEquals(PasswordRequirement.entries.toSet(), PasswordForm().unmetRequirements)
        assertEquals(emptySet(), valid.unmetRequirements)
    }

    @Test
    fun `whether both new passwords match is only known once the repetition is written`() {
        assertNull(PasswordForm().withNext("Nueva2026?").passwordsMatch)
        assertEquals(true, valid.passwordsMatch)
        assertEquals(false, valid.withConfirmation("Otra2026?").passwordsMatch)
    }

    // ── Cuándo se puede enviar ──

    @Test
    fun `with the three fields right it can be sent`() {
        assertTrue(valid.canSend)
        assertEquals(PasswordChange("Gradus2026!", "Nueva2026?"), valid.toChange())
    }

    @Test
    fun `without the current password it cannot be sent`() {
        assertFalse(valid.withCurrent("").canSend)
    }

    @Test
    fun `a weak new password cannot be sent`() {
        assertFalse(valid.withNext("abc").withConfirmation("abc").canSend)
    }

    @Test
    fun `if the repetition does not match it cannot be sent`() {
        assertFalse(valid.withConfirmation("Nueva2026!").canSend)
    }

    // Las contraseñas no se recortan: un espacio forma parte de ellas (como en el registro).
    @Test
    fun `passwords are sent as typed`() {
        val spaced = valid.withCurrent(" Gradus2026! ")
        assertEquals(" Gradus2026! ", spaced.toChange().currentPassword)
    }

    // ── Errores: solo después de intentar guardar ──

    @Test
    fun `errors only show after trying to save`() {
        val empty = PasswordForm()
        assertNull(empty.currentError)
        assertNull(empty.nextError)
        assertNull(empty.confirmationError)
        val tried = empty.sending()
        assertEquals(FieldError.EMPTY, tried.currentError)
        assertEquals(FieldError.EMPTY, tried.nextError)
        assertEquals(FieldError.EMPTY, tried.confirmationError)
    }

    @Test
    fun `a weak new password and a different repetition say so`() {
        val tried = valid.withNext("abc").sending()
        assertEquals(FieldError.WEAK_PASSWORD, tried.nextError)
        assertEquals(FieldError.PASSWORDS_DONT_MATCH, tried.confirmationError)
        assertEquals(Submission.Idle, tried.submission)
    }

    // ── Envío ──

    @Test
    fun `saving a valid form goes to sending and cannot be sent twice`() {
        val sending = valid.sending()
        assertEquals(Submission.Sending, sending.submission)
        assertFalse(sending.canSend)
    }

    @Test
    fun `after a network failure it can be retried`() {
        val failed = valid.sending().failed(FailureReason.UNREACHABLE)
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), failed.submission)
        assertTrue(failed.canSend)
    }

    @Test
    fun `once changed it cannot be sent again`() {
        assertFalse(valid.sending().succeeded().canSend)
    }

    // ── Lo que rechaza el servidor ──

    @Test
    fun `a wrong current password is told under it and can be corrected`() {
        val rejected = valid.sending().rejected(PasswordProblem.WrongCurrent)
        assertTrue(rejected.isWrongCurrent)
        assertEquals(Submission.Idle, rejected.submission)
        assertFalse(rejected.withCurrent("Gradus2026?").isWrongCurrent)
    }

    @Test
    fun `an error of the server shows under its field`() {
        val rejected = valid.sending().rejected(PasswordProblem.InField(PasswordChangeField.NEW, FieldError.WEAK_PASSWORD))
        assertEquals(FieldError.WEAK_PASSWORD, rejected.nextError)
        assertNull(rejected.currentError)
    }

    // Editar cualquiera de los tres campos borra lo que dijo el servidor.
    @Test
    fun `editing any field clears the problem of the server`() {
        val rejected = valid.sending().rejected(PasswordProblem.WrongCurrent)
        assertFalse(rejected.withNext("Nueva2026?!").isWrongCurrent)
        assertFalse(rejected.withConfirmation("Nueva2026?").isWrongCurrent)
    }

    @Test
    fun `the passwords never appear in toString`() {
        val text = valid.toString()
        assertFalse(text.contains("Gradus2026!"))
        assertFalse(text.contains("Nueva2026?"))
    }
}
