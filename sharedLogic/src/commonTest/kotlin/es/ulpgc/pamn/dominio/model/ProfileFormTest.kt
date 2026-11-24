package es.ulpgc.pamn.dominio.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// El formulario de «Editar perfil» (docs/tareas/ajustes.md, fase 2, tarea 6). Solo el nombre (decisión F).
class ProfileFormTest {

    private val form = ProfileForm(original = "Ana")

    // ── Empieza con el nombre de la cuenta ──

    @Test
    fun `it starts with the name of the account and no errors`() {
        assertEquals("Ana", form.name)
        assertNull(form.nameError)
        assertEquals(Submission.Idle, form.submission)
    }

    // ── Cambios ──

    @Test
    fun `without changes there is nothing to save`() {
        assertFalse(form.hasChanges)
        assertFalse(form.canSend)
    }

    @Test
    fun `a different name can be saved`() {
        val changed = form.withName("Ana Ruiz")
        assertTrue(changed.hasChanges)
        assertTrue(changed.canSend)
    }

    // Quitar o poner espacios alrededor no es un cambio (el servidor los quita).
    @Test
    fun `surrounding spaces are not a change`() {
        assertFalse(form.withName("  Ana  ").hasChanges)
    }

    @Test
    fun `the name to send has no surrounding spaces`() {
        assertEquals("Ana Ruiz", form.withName("  Ana Ruiz  ").nameToSend)
    }

    // ── Validación (las reglas del registro) ──

    @Test
    fun `errors only show after trying to save`() {
        val empty = form.withName("")
        assertNull(empty.nameError)
        assertEquals(FieldError.EMPTY, empty.submitted().nameError)
    }

    @Test
    fun `a one letter name is too short`() {
        assertEquals(FieldError.TOO_SHORT, form.withName("A").submitted().nameError)
        assertFalse(form.withName("A").canSend)
    }

    @Test
    fun `a name longer than the maximum is too long`() {
        val long = form.withName("a".repeat(Validation.MAX_NAME_LENGTH + 1))
        assertEquals(FieldError.TOO_LONG, long.submitted().nameError)
    }

    // ── Envío ──

    @Test
    fun `saving a valid change goes to sending`() {
        assertEquals(Submission.Sending, form.withName("Ana Ruiz").sending().submission)
    }

    @Test
    fun `saving an invalid name only shows the error`() {
        val tried = form.withName("A").sending()
        assertEquals(Submission.Idle, tried.submission)
        assertEquals(FieldError.TOO_SHORT, tried.nameError)
    }

    @Test
    fun `while sending it cannot be sent again`() {
        assertFalse(form.withName("Ana Ruiz").sending().canSend)
    }

    @Test
    fun `after a network failure it can be retried`() {
        val failed = form.withName("Ana Ruiz").sending().failed(FailureReason.UNREACHABLE)
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), failed.submission)
        assertTrue(failed.canSend)
    }

    @Test
    fun `once saved it cannot be sent again`() {
        assertFalse(form.withName("Ana Ruiz").sending().succeeded().canSend)
    }

    // ── Errores del servidor ──

    @Test
    fun `an error from the server shows under the name`() {
        val rejected = form.withName("Ana Ruiz").sending().rejected(FieldError.TOO_SHORT)
        assertEquals(FieldError.TOO_SHORT, rejected.nameError)
        assertEquals(Submission.Idle, rejected.submission)
    }

    @Test
    fun `editing the name clears the error of the server`() {
        val rejected = form.withName("Ana Ruiz").sending().rejected(FieldError.TOO_SHORT)
        assertNull(rejected.withName("Ana María").nameError)
    }
}
