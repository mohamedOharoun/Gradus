package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.Validation
import es.ulpgc.pamn.dominio.ui.text.UiText

// Adaptador entre la lógica y la interfaz: model/ dice qué error hay; aquí se decide qué texto lo dice
// (UiText, docs/tareas/ajustes.md, fase 3, decisión U). Las frases, en values/ y values-en/strings.xml.
fun FieldError.toMessage(): UiText = when (this) {
    FieldError.EMPTY -> UiText.of(R.string.error_required)
    FieldError.TOO_SHORT -> UiText.of(R.string.error_too_short, Validation.MIN_NAME_LENGTH)
    FieldError.TOO_LONG -> UiText.of(R.string.error_too_long)
    FieldError.INVALID_EMAIL -> UiText.of(R.string.error_invalid_email)
    FieldError.WEAK_PASSWORD -> UiText.of(R.string.error_weak_password)
    FieldError.PASSWORDS_DONT_MATCH -> UiText.of(R.string.error_passwords_dont_match)
    FieldError.EMAIL_TAKEN -> UiText.of(R.string.error_email_taken)
    FieldError.TERMS_NOT_ACCEPTED -> UiText.of(R.string.error_terms_not_accepted)
    // No dice cuál de los dos falla, a propósito (así no se puede averiguar qué correos tienen cuenta).
    FieldError.INVALID_CREDENTIALS -> UiText.of(R.string.error_invalid_credentials)
}

// Por qué no se ha podido enviar un formulario. Los datos se conservan: basta con reintentar.
fun FailureReason.toMessage(): UiText = when (this) {
    FailureReason.UNREACHABLE -> UiText.of(R.string.error_unreachable)
    FailureReason.SERVER_FAILURE -> UiText.of(R.string.error_server_failure)
}
