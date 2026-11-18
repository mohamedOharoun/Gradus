package es.ulpgc.pamn.dominio.model

// Qué le pasa a un campo. La interfaz decide el texto que se muestra para cada error.
enum class FieldError {
    EMPTY,
    TOO_SHORT,
    TOO_LONG,
    INVALID_EMAIL,
    WEAK_PASSWORD,
    PASSWORDS_DONT_MATCH,

    // Solo los detecta el servidor (account/RegisterProblem.kt).
    EMAIL_TAKEN,
    TERMS_NOT_ACCEPTED,
    INVALID_CREDENTIALS,
}
