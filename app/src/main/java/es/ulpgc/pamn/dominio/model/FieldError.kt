package es.ulpgc.pamn.dominio.model

// Qué le pasa a un campo. La interfaz decide el texto que se muestra para cada error.
enum class FieldError {
    EMPTY,
    TOO_SHORT,
    INVALID_EMAIL,
    WEAK_PASSWORD,
    PASSWORDS_DONT_MATCH,
}
