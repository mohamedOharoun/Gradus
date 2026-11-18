package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.LoginForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CredentialsTest {

    private val validForm = LoginForm(email = " ana@ulpgc.es ", password = " Gradus2026! ")

    @Test
    fun `a valid form becomes credentials`() {
        assertEquals(Credentials(email = "ana@ulpgc.es", password = " Gradus2026! "), validForm.toCredentials())
    }

    @Test
    fun `the email is trimmed but the password is kept as typed`() {
        val credentials = validForm.toCredentials()
        assertEquals("ana@ulpgc.es", credentials?.email)
        assertEquals(" Gradus2026! ", credentials?.password)
    }

    @Test
    fun `with a malformed email there are no credentials`() {
        assertNull(validForm.withEmail("ana@").toCredentials())
    }

    @Test
    fun `with an empty password there are no credentials`() {
        assertNull(validForm.withPassword("").toCredentials())
    }

    // Al entrar no se exigen las reglas del registro: una cuenta antigua debe poder entrar.
    @Test
    fun `a short password is still sent`() {
        assertEquals("abc", validForm.withPassword("abc").toCredentials()?.password)
    }

    @Test
    fun `the password never appears in toString`() {
        assertFalse(validForm.toCredentials().toString().contains("Gradus2026!"))
    }
}
