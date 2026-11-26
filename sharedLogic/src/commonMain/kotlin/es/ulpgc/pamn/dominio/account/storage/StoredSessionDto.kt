package es.ulpgc.pamn.dominio.account.storage

import kotlinx.serialization.Serializable

// DTO: lo que se guarda en disco. Aparte de SessionResponseDto (el de la red): si el JSON del servidor
// cambia, lo ya guardado en el móvil sigue leyéndose.
@Serializable
internal data class StoredSessionDto(
    val token: String,
    val expiresAt: String,
    val account: StoredAccountDto,
) {
    // Quien tenga el token tiene la sesión: no puede acabar en los logs.
    override fun toString() = "StoredSessionDto(token=****, expiresAt=$expiresAt, account=$account)"
}

@Serializable
internal data class StoredAccountDto(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: String,
)
