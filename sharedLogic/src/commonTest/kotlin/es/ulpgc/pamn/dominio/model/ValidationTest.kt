package es.ulpgc.pamn.dominio.model

import es.ulpgc.pamn.dominio.model.PasswordRequirement.LETTER_AND_DIGIT
import es.ulpgc.pamn.dominio.model.PasswordRequirement.MIN_LENGTH
import es.ulpgc.pamn.dominio.model.PasswordRequirement.SPECIAL_CHARACTER
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidationTest {

    // ── Nombre: al menos 2 caracteres (sin contar espacios de los lados) ──

    @Test
    fun `empty name is an error`() = assertEquals(FieldError.EMPTY, Validation.validateName(""))

    @Test
    fun `name with only spaces is empty`() = assertEquals(FieldError.EMPTY, Validation.validateName("   "))

    @Test
    fun `one-letter name is too short`() = assertEquals(FieldError.TOO_SHORT, Validation.validateName("A"))

    @Test
    fun `two-letter name is valid`() = assertNull(Validation.validateName("Al"))

    @Test
    fun `spaces around a valid name are ignored`() = assertNull(Validation.validateName("  Mohamed "))

    @Test
    fun `name of the maximum length is valid`() = assertNull(Validation.validateName("a".repeat(Validation.MAX_NAME_LENGTH)))

    @Test
    fun `name over the maximum length is too long`() =
        assertEquals(FieldError.TOO_LONG, Validation.validateName("a".repeat(Validation.MAX_NAME_LENGTH + 1)))

    // ── Longitud: caracteres reales, como el backend y PostgreSQL (un emoji cuenta 1, no 2) ──

    @Test
    fun `an emoji counts as one character`() = assertEquals(1, Validation.lengthOf("😀"))

    @Test
    fun `a single emoji is a name that is too short`() =
        assertEquals(FieldError.TOO_SHORT, Validation.validateName("😀"))

    @Test
    fun `a name of maximum length in emojis is valid`() =
        assertNull(Validation.validateName("😀".repeat(Validation.MAX_NAME_LENGTH)))

    // ── Correo: formato texto@dominio.ext ──
    // Mismos casos que EmailTest del backend (backend/src/test/.../domain/EmailTest.java):
    // si cambia el formato de correo, hay que cambiarlo en los dos lados.

    @Test
    fun `empty email is an error`() = assertEquals(FieldError.EMPTY, Validation.validateEmail(""))

    @Test
    fun `email without at sign is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("mohamed"))

    @Test
    fun `email without domain is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("mohamed@"))

    @Test
    fun `email without extension is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("mohamed@ejemplo"))

    @Test
    fun `email with inner spaces is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("moh amed@ejemplo.com"))

    @Test
    fun `well-formed email is valid`() = assertNull(Validation.validateEmail("mohamed@ejemplo.com"))

    @Test
    fun `spaces around a valid email are ignored`() = assertNull(Validation.validateEmail(" mohamed@ejemplo.com "))

    @Test
    fun `email with subdomains and plus sign is valid`() =
        assertNull(Validation.validateEmail("nombre.apellido+gradus@sub.dominio.es"))

    @Test
    fun `email with an empty part in the domain is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("a@b..com"))

    @Test
    fun `email ending with a dot is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("a@b.com."))

    @Test
    fun `email with a one-letter extension is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("a@b.c"))

    @Test
    fun `email with two at signs is invalid`() =
        assertEquals(FieldError.INVALID_EMAIL, Validation.validateEmail("a@@b.com"))

    @Test
    fun `email over the maximum length is too long`() =
        assertEquals(FieldError.TOO_LONG, Validation.validateEmail("a".repeat(Validation.MAX_EMAIL_LENGTH) + "@b.com"))

    // ── Requisitos de la contraseña (para marcarlos uno a uno en el paso 3) ──

    @Test
    fun `empty password meets no requirement`() =
        assertEquals(setOf(MIN_LENGTH, LETTER_AND_DIGIT, SPECIAL_CHARACTER), Validation.unmetRequirements(""))

    @Test
    fun `eight letters only miss digit and special character`() =
        assertEquals(setOf(LETTER_AND_DIGIT, SPECIAL_CHARACTER), Validation.unmetRequirements("abcdefgh"))

    @Test
    fun `letters and digit only miss special character`() =
        assertEquals(setOf(SPECIAL_CHARACTER), Validation.unmetRequirements("abcdefg1"))

    @Test
    fun `digits and symbol without letter miss letter and digit`() =
        assertEquals(setOf(LETTER_AND_DIGIT), Validation.unmetRequirements("12345678!"))

    @Test
    fun `a space does not count as special character`() =
        assertEquals(setOf(SPECIAL_CHARACTER), Validation.unmetRequirements("abcdefg1 "))

    @Test
    fun `short password with everything else misses only length`() =
        assertEquals(setOf(MIN_LENGTH), Validation.unmetRequirements("ab1!"))

    @Test
    fun `password with all requirements has none unmet`() =
        assertEquals(emptySet<PasswordRequirement>(), Validation.unmetRequirements("secreta1!"))

    @Test
    fun `password length counts an emoji as one character`() =
        assertEquals(setOf(MIN_LENGTH), Validation.unmetRequirements("😀😀😀😀a1!"))

    @Test
    fun `letters with accents and enye count as letters`() =
        assertEquals(emptySet<PasswordRequirement>(), Validation.unmetRequirements("contraseña1!"))

    // ── Contraseña completa ──

    @Test
    fun `empty password is an error`() = assertEquals(FieldError.EMPTY, Validation.validatePassword(""))

    @Test
    fun `password missing requirements is weak`() =
        assertEquals(FieldError.WEAK_PASSWORD, Validation.validatePassword("secreta1"))

    @Test
    fun `password with all requirements is valid`() = assertNull(Validation.validatePassword("secreta1!"))

    // ── Repetir contraseña ──

    @Test
    fun `empty confirmation is an error`() =
        assertEquals(FieldError.EMPTY, Validation.validatePasswordConfirmation("secreta1!", ""))

    @Test
    fun `different confirmation does not match`() =
        assertEquals(FieldError.PASSWORDS_DONT_MATCH, Validation.validatePasswordConfirmation("secreta1!", "secreta1"))

    @Test
    fun `identical confirmation is valid`() =
        assertNull(Validation.validatePasswordConfirmation("secreta1!", "secreta1!"))
}
