package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NewAccountTest {

    private val readyForm = RegisterForm(
        name = "  Ana  ",
        email = " ana@ulpgc.es ",
        step = RegisterStep.PASSWORD,
        password = " Gradus2026! ",
        confirmation = " Gradus2026! ",
        acceptedTerms = true,
    )

    @Test
    fun `a form ready to register becomes a new account`() {
        assertEquals(
            NewAccount(name = "Ana", email = "ana@ulpgc.es", password = " Gradus2026! ", acceptedTerms = true),
            readyForm.toNewAccount(),
        )
    }

    @Test
    fun `name and email are trimmed but the password is kept as typed`() {
        val account = readyForm.toNewAccount()
        assertEquals("Ana", account?.name)
        assertEquals("ana@ulpgc.es", account?.email)
        assertEquals(" Gradus2026! ", account?.password)
    }

    @Test
    fun `without accepting the terms there is no new account`() {
        assertNull(readyForm.withTermsAccepted(false).toNewAccount())
    }

    @Test
    fun `before the password step there is no new account`() {
        assertNull(readyForm.copy(step = RegisterStep.EMAIL).toNewAccount())
    }

    @Test
    fun `with an invalid field there is no new account`() {
        assertNull(readyForm.withEmail("ana@").toNewAccount())
    }

    @Test
    fun `the password never appears in toString`() {
        val text = readyForm.toNewAccount().toString()
        assertEquals(false, text.contains("Gradus2026!"))
    }
}
