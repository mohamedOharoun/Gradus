package es.ulpgc.pamn.dominio.model

// Cada requisito sabe comprobarse a sí mismo: añadir uno nuevo es añadir una línea aquí.
enum class PasswordRequirement(val isMetBy: (String) -> Boolean) {
    MIN_LENGTH({ password -> Validation.lengthOf(password) >= 8 }),
    LETTER_AND_DIGIT({ password -> password.any(Char::isLetter) && password.any(Char::isDigit) }),
    SPECIAL_CHARACTER({ password -> password.any { !it.isLetterOrDigit() && !it.isWhitespace() } }),
}
