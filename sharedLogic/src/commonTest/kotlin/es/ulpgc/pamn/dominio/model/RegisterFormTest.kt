package es.ulpgc.pamn.dominio.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterFormTest {

    private val onEmailStep = RegisterForm(name = "Ana", step = RegisterStep.EMAIL)

    // ── Estado inicial ──

    @Test
    fun `new form starts on the name step without errors`() {
        val form = RegisterForm()
        assertEquals(RegisterStep.NAME, form.step)
        assertNull(form.nameError)
        assertNull(form.emailError)
    }

    @Test
    fun `invalid name shows no error before pressing next`() {
        assertNull(RegisterForm(name = "A").nameError)
    }

    // ── Paso 1: nombre ──

    @Test
    fun `next with empty name stays on the name step and shows the error`() {
        val form = RegisterForm().next()
        assertEquals(RegisterStep.NAME, form.step)
        assertEquals(FieldError.EMPTY, form.nameError)
    }

    @Test
    fun `next with a too short name shows too short`() {
        assertEquals(FieldError.TOO_SHORT, RegisterForm(name = "A").next().nameError)
    }

    @Test
    fun `next with a valid name moves to the email step`() {
        assertEquals(RegisterStep.EMAIL, RegisterForm(name = "Ana").next().step)
    }

    @Test
    fun `the new step starts without errors`() {
        val form = RegisterForm(name = "Ana").next()
        assertNull(form.emailError)
    }

    @Test
    fun `fixing the name after the error hides it`() {
        val form = RegisterForm().next().withName("Ana")
        assertNull(form.nameError)
    }

    // ── Paso 2: correo ──

    @Test
    fun `next with an invalid email stays on the email step and shows the error`() {
        val form = onEmailStep.withEmail("ana@").next()
        assertEquals(RegisterStep.EMAIL, form.step)
        assertEquals(FieldError.INVALID_EMAIL, form.emailError)
    }

    @Test
    fun `next with a valid email moves to the password step`() {
        val form = onEmailStep.withEmail("ana@ejemplo.com").next()
        assertEquals(RegisterStep.PASSWORD, form.step)
    }

    @Test
    fun `name error is not shown while on the email step`() {
        val form = RegisterForm(name = "A", step = RegisterStep.EMAIL).next()
        assertNull(form.nameError)
    }

    // ── Paso 3: la contraseña llega en T10 ──

    @Test
    fun `next on the last step does not move`() {
        val form = RegisterForm(name = "Ana", email = "ana@ejemplo.com", step = RegisterStep.PASSWORD)
        assertEquals(RegisterStep.PASSWORD, form.next().step)
    }

    // ── Atrás ──

    @Test
    fun `back from the email step returns to the name step and keeps the data`() {
        val form = onEmailStep.withEmail("ana@ejemplo.com").back()
        assertEquals(RegisterForm(name = "Ana", email = "ana@ejemplo.com"), form)
    }

    @Test
    fun `back hides the errors of the step you leave`() {
        val form = onEmailStep.next().back()?.next()?.back()
        assertNull(form?.emailError)
    }

    @Test
    fun `back from the first step returns null to leave the registration`() {
        assertNull(RegisterForm().back())
    }

    // ── Editar un dato desde su chip ──

    @Test
    fun `going back to an earlier step keeps the data and hides errors`() {
        val form = RegisterForm(name = "Ana", email = "ana@", step = RegisterStep.PASSWORD, isStepSubmitted = true)
        assertEquals(RegisterForm(name = "Ana", email = "ana@"), form.goBackTo(RegisterStep.NAME))
    }

    @Test
    fun `going to a later step is ignored`() {
        val form = RegisterForm(name = "Ana")
        assertEquals(form, form.goBackTo(RegisterStep.PASSWORD))
    }

    @Test
    fun `going to the current step changes nothing`() {
        assertEquals(onEmailStep, onEmailStep.goBackTo(RegisterStep.EMAIL))
    }

    // ── Paso 3: contraseña, repetición y términos ──

    private val strongPassword = "secreta1!"
    private val readyToRegister = RegisterForm(
        name = "Ana",
        email = "ana@ejemplo.com",
        step = RegisterStep.PASSWORD,
        password = strongPassword,
        confirmation = strongPassword,
        acceptedTerms = true,
    )

    @Test
    fun `empty password has every requirement unmet`() {
        assertEquals(PasswordRequirement.entries.toSet(), RegisterForm().unmetRequirements)
    }

    @Test
    fun `requirements are met while typing`() {
        val form = RegisterForm().withPassword("abcdefgh")
        assertEquals(
            setOf(PasswordRequirement.LETTER_AND_DIGIT, PasswordRequirement.SPECIAL_CHARACTER),
            form.unmetRequirements,
        )
    }

    @Test
    fun `strong password has no unmet requirements`() {
        assertEquals(emptySet<PasswordRequirement>(), RegisterForm().withPassword(strongPassword).unmetRequirements)
    }

    @Test
    fun `passwords match is unknown while the confirmation is empty`() {
        assertNull(RegisterForm().withPassword(strongPassword).passwordsMatch)
    }

    @Test
    fun `passwords match when both are equal`() {
        val form = RegisterForm().withPassword(strongPassword).withConfirmation(strongPassword)
        assertTrue(form.passwordsMatch!!)
    }

    @Test
    fun `passwords do not match when they are different`() {
        val form = RegisterForm().withPassword(strongPassword).withConfirmation("secreta1")
        assertFalse(form.passwordsMatch!!)
    }

    @Test
    fun `can register when everything is valid and terms are accepted`() {
        assertTrue(readyToRegister.canRegister)
    }

    @Test
    fun `cannot register without accepting the terms`() {
        assertFalse(readyToRegister.withTermsAccepted(false).canRegister)
    }

    @Test
    fun `cannot register with a weak password`() {
        val form = readyToRegister.withPassword("abc").withConfirmation("abc")
        assertFalse(form.canRegister)
    }

    @Test
    fun `cannot register when the passwords do not match`() {
        assertFalse(readyToRegister.withConfirmation("otra1!xyz").canRegister)
    }

    @Test
    fun `cannot register before reaching the last step`() {
        assertFalse(readyToRegister.goBackTo(RegisterStep.EMAIL).canRegister)
    }

    @Test
    fun `going back keeps the password`() {
        val form = readyToRegister.goBackTo(RegisterStep.NAME)
        assertEquals(strongPassword, form.password)
    }
}
