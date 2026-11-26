package es.ulpgc.pamn.dominio.account

// Espejo de ErrorCode.java en el backend: mismos nombres, en el mismo orden.
// UNKNOWN recoge los códigos que el servidor añada antes de que la app los conozca.
enum class ApiErrorCode {
    REQUIRED,
    TOO_SHORT,
    TOO_LONG,
    INVALID_EMAIL,
    MISSING_LETTER,
    MISSING_DIGIT,
    MISSING_SPECIAL_CHARACTER,
    TERMS_NOT_ACCEPTED,
    EMAIL_TAKEN,
    MALFORMED_BODY,
    INVALID_CREDENTIALS,
    UNAUTHENTICATED,
    INVALID_PARAMETER,
    COURSE_NOT_FOUND,
    NOT_COURSE_AUTHOR,
    IDENTICAL_TO_LATEST,
    // Ajustes (docs/tareas/ajustes.md, fase 2).
    WRONG_PASSWORD,
    AUTHOR_OF_COURSES,
    // Entregas con foto (docs/tareas/ejercicio-foto.md, decisión P).
    INVALID_PHOTO,
    PHOTO_TOO_LARGE,
    EXERCISE_NOT_FOUND,
    SUBMISSION_NOT_FOUND,
    NOT_SUBMISSION_OWNER,
    UNKNOWN;

    companion object {
        // Factory method: nunca falla, un texto desconocido da UNKNOWN.
        fun fromName(name: String): ApiErrorCode = entries.firstOrNull { it.name == name } ?: UNKNOWN
    }
}
