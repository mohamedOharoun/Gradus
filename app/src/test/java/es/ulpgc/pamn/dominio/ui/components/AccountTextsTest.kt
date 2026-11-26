package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.PasswordRequirement
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.model.Validation
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// Los textos de la cuenta y de los formularios (docs/tareas/ajustes.md, fase 3, paso 6). Con UiText
// (decisión U): se comprueba qué texto es y con qué datos; las frases están en values/ y values-en/.
class AccountTextsTest {

    @Test
    fun `each field error has its own text`() {
        assertEquals(UiText.of(R.string.error_required), FieldError.EMPTY.toMessage())
        assertEquals(UiText.of(R.string.error_too_long), FieldError.TOO_LONG.toMessage())
        assertEquals(UiText.of(R.string.error_invalid_email), FieldError.INVALID_EMAIL.toMessage())
        assertEquals(UiText.of(R.string.error_weak_password), FieldError.WEAK_PASSWORD.toMessage())
        assertEquals(UiText.of(R.string.error_passwords_dont_match), FieldError.PASSWORDS_DONT_MATCH.toMessage())
        assertEquals(UiText.of(R.string.error_email_taken), FieldError.EMAIL_TAKEN.toMessage())
        assertEquals(UiText.of(R.string.error_terms_not_accepted), FieldError.TERMS_NOT_ACCEPTED.toMessage())
        assertEquals(UiText.of(R.string.error_invalid_credentials), FieldError.INVALID_CREDENTIALS.toMessage())
    }

    // El mínimo sale de la regla, no del texto: si cambia la regla, el mensaje dice lo mismo.
    @Test
    fun `too short says the minimum length`() {
        assertEquals(UiText.of(R.string.error_too_short, Validation.MIN_NAME_LENGTH), FieldError.TOO_SHORT.toMessage())
    }

    @Test
    fun `no two field errors share a text`() {
        assertEquals(FieldError.entries.size, FieldError.entries.map { it.toMessage() }.toSet().size)
    }

    @Test
    fun `each reason a form could not be sent has its own text`() {
        assertEquals(UiText.of(R.string.error_unreachable), FailureReason.UNREACHABLE.toMessage())
        assertEquals(UiText.of(R.string.error_server_failure), FailureReason.SERVER_FAILURE.toMessage())
    }

    // El botón principal de un formulario: su acción, «…ando» mientras se envía y «Reintentar» si falló.
    @Test
    fun `the main button says the action, then sending, then retry`() {
        val idle = UiText.of(R.string.action_enter)
        val sending = UiText.of(R.string.action_entering)
        assertEquals(idle, Submission.Idle.buttonText(idle, sending))
        assertEquals(sending, Submission.Sending.buttonText(idle, sending))
        assertEquals(UiText.of(R.string.action_retry), Submission.Failed(FailureReason.UNREACHABLE).buttonText(idle, sending))
        assertEquals(idle, Submission.Succeeded.buttonText(idle, sending))
    }

    @Test
    fun `each password requirement has its own label`() {
        assertEquals(UiText.of(R.string.password_rule_min_length), PasswordRequirement.MIN_LENGTH.toLabel())
        assertEquals(UiText.of(R.string.password_rule_letter_and_digit), PasswordRequirement.LETTER_AND_DIGIT.toLabel())
        assertEquals(UiText.of(R.string.password_rule_special_character), PasswordRequirement.SPECIAL_CHARACTER.toLabel())
    }
}
