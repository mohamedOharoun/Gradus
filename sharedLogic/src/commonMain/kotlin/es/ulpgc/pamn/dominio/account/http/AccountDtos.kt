package es.ulpgc.pamn.dominio.account.http

import kotlinx.serialization.Serializable

// DTO: lo que viaja por la red, campo a campo igual que los records del backend (backend/.../web/).
// No son el modelo de la app: si el JSON cambia, cambian estas clases y su mapper, nada más.

// RegisterUserRequest.java. Exactamente estos cuatro campos: el backend rechaza los desconocidos.
@Serializable
internal data class RegisterUserRequestDto(
    val name: String,
    val email: String,
    val password: String,
    val acceptedTerms: Boolean,
) {
    override fun toString() = "RegisterUserRequestDto(name=$name, email=$email, password=****, acceptedTerms=$acceptedTerms)"
}

// UserResponse.java.
@Serializable
internal data class UserResponseDto(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: String,
)

// LogInRequest.java. Exactamente estos dos campos: el backend rechaza los desconocidos.
@Serializable
internal data class LogInRequestDto(
    val email: String,
    val password: String,
) {
    override fun toString() = "LogInRequestDto(email=$email, password=****)"
}

// SessionResponse.java: {"token", "expiresAt", "user": UserResponse}.
@Serializable
internal data class SessionResponseDto(
    val token: String,
    val expiresAt: String,
    val user: UserResponseDto,
) {
    // Quien tenga el token tiene la sesión: no puede acabar en los logs.
    override fun toString() = "SessionResponseDto(token=****, expiresAt=$expiresAt, user=$user)"
}

// ErrorResponse.java: {"errors": [{"field", "code", "message"}]}. «message» se ignora: el texto lo decide la app.
@Serializable
internal data class ErrorResponseDto(val errors: List<FieldErrorDto>)

@Serializable
internal data class FieldErrorDto(val field: String, val code: String)
