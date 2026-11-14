package es.ulpgc.pamn.dominio.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginFormTest {

    private val valid = LoginForm(email = "mohamed@ejemplo.com", password = "abc")

    // ── Los errores solo se ven después de pulsar «Entrar» ──

    @Test
    fun `new form shows no errors`() {
        val form = LoginForm()
        assertNull(form.emailError)
        assertNull(form.passwordError)
    }

    @Test
    fun `invalid data shows no errors before submitting`() {
        val form = LoginForm(email = "mohamed@", password = "")
        assertNull(form.emailError)
        assertNull(form.passwordError)
    }

    @Test
    fun `submitting an empty form shows empty errors`() {
        val form = LoginForm().submitted()
        assertEquals(FieldError.EMPTY, form.emailError)
        assertEquals(FieldError.EMPTY, form.passwordError)
    }

    @Test
    fun `submitting a malformed email shows invalid email`() {
        val form = valid.withEmail("mohamed@").submitted()
        assertEquals(FieldError.INVALID_EMAIL, form.emailError)
    }

    @Test
    fun `fixing the email after submitting clears its error`() {
        val form = LoginForm(email = "mohamed@").submitted().withEmail("mohamed@ejemplo.com")
        assertNull(form.emailError)
    }

    @Test
    fun `typing a password after submitting clears its error`() {
        val form = LoginForm().submitted().withPassword("x")
        assertNull(form.passwordError)
    }

    // ── Cuándo se puede entrar ──

    @Test
    fun `form with valid email and any password is valid`() = assertTrue(valid.isValid)

    @Test
    fun `login does not apply the registration password rules`() =
        assertTrue(valid.withPassword("a").isValid)

    @Test
    fun `form with malformed email is not valid`() = assertFalse(valid.withEmail("mohamed").isValid)

    @Test
    fun `form with empty password is not valid`() = assertFalse(valid.withPassword("").isValid)

    // ── Inmutabilidad: cada cambio devuelve un formulario nuevo ──

    @Test
    fun `submitting keeps the typed data`() {
        val form = valid.submitted()
        assertEquals(valid.email, form.email)
        assertEquals(valid.password, form.password)
    }

    @Test
    fun `changes do not modify the original form`() {
        valid.withEmail("otro@ejemplo.com").submitted()
        assertEquals("mohamed@ejemplo.com", valid.email)
        assertFalse(valid.isSubmitted)
    }
}
