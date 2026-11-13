package es.ulpgc.pamn.dominio.model

// Reglas de los formularios de registro e inicio de sesión (docs/especificacion.md).
// Kotlin puro: sin Android, para probarlo con JUnit y reutilizarlo en el servidor.
// Cada función devuelve null si el dato es válido, o el error que tiene.
object Validation {

    const val MIN_NAME_LENGTH = 2
    private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]{2,}$""")

    fun validateName(name: String): FieldError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> FieldError.EMPTY
            trimmed.length < MIN_NAME_LENGTH -> FieldError.TOO_SHORT
            else -> null
        }
    }

    fun validateEmail(email: String): FieldError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> FieldError.EMPTY
            !emailPattern.matches(trimmed) -> FieldError.INVALID_EMAIL
            else -> null
        }
    }

    fun unmetRequirements(password: String): Set<PasswordRequirement> =
        PasswordRequirement.entries.filterNot { it.isMetBy(password) }.toSet()

    // La contraseña no se recorta: un espacio al final es parte de ella.
    fun validatePassword(password: String): FieldError? = when {
        password.isEmpty() -> FieldError.EMPTY
        unmetRequirements(password).isNotEmpty() -> FieldError.WEAK_PASSWORD
        else -> null
    }

    fun validatePasswordConfirmation(password: String, confirmation: String): FieldError? = when {
        confirmation.isEmpty() -> FieldError.EMPTY
        confirmation != password -> FieldError.PASSWORDS_DONT_MATCH
        else -> null
    }
}
