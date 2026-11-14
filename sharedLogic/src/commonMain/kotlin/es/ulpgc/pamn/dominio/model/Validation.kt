package es.ulpgc.pamn.dominio.model

// Reglas de los formularios de registro e inicio de sesión (docs/especificacion.md).
// Kotlin puro (sharedLogic/commonMain): sin Android, para probarlo con kotlin.test y compartirlo con iOS.
// Cada función devuelve null si el dato es válido, o el error que tiene.
// Son las mismas reglas que valida el backend (backend/.../domain): si cambia una, cambian las dos.
object Validation {

    const val MIN_NAME_LENGTH = 2
    const val MAX_NAME_LENGTH = 100
    const val MAX_EMAIL_LENGTH = 254

    // texto@dominio.ext, sin espacios, sin partes del dominio vacías y con extensión de 2+ caracteres.
    // Mismo patrón que Email.java en el backend.
    private val emailPattern = Regex("""^[^\s@]+@[^\s@.]+(\.[^\s@.]+)*\.[^\s@.]{2,}$""")

    // Caracteres reales: un emoji ocupa dos Char en Kotlin pero cuenta como uno,
    // igual que en el backend y en PostgreSQL.
    fun lengthOf(text: String): Int = text.count { !it.isLowSurrogate() }

    fun validateName(name: String): FieldError? {
        val length = lengthOf(name.trim())
        return when {
            length == 0 -> FieldError.EMPTY
            length < MIN_NAME_LENGTH -> FieldError.TOO_SHORT
            length > MAX_NAME_LENGTH -> FieldError.TOO_LONG
            else -> null
        }
    }

    fun validateEmail(email: String): FieldError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> FieldError.EMPTY
            !emailPattern.matches(trimmed) -> FieldError.INVALID_EMAIL
            lengthOf(trimmed) > MAX_EMAIL_LENGTH -> FieldError.TOO_LONG
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
