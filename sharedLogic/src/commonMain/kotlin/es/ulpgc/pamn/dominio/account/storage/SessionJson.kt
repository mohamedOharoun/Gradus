package es.ulpgc.pamn.dominio.account.storage

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import kotlinx.serialization.json.Json

// Mapper: entre Session y el JSON que la app guarda en disco (cifrado, en la app). Funciones puras.
// Son públicas porque las usa la app; el DTO (StoredSessionDto) no.
// ignoreUnknownKeys: un fichero escrito por una versión futura con campos de más sigue leyéndose.
private val sessionJson = Json { ignoreUnknownKeys = true }

// OJO: el texto lleva el token en claro (para eso se cifra después). No lo registres ni lo muestres.
fun encodeSession(session: Session): String = sessionJson.encodeToString(session.toDto())

// Lee lo guardado; null si no se puede leer (JSON roto, vacío, no es un objeto, falta un campo o tiene
// un tipo equivocado). SerializationException hereda de IllegalArgumentException; nunca sale una
// excepción hacia fuera y el fallo no lleva texto (el mensaje de la excepción podría citar el token).
fun decodeSession(text: String): Session? = try {
    sessionJson.decodeFromString<StoredSessionDto>(text).toSession()
} catch (_: IllegalArgumentException) {
    null
}

private fun Session.toDto() = StoredSessionDto(
    token = token,
    expiresAt = expiresAt,
    account = StoredAccountDto(account.id, account.name, account.email, account.createdAt),
)

private fun StoredSessionDto.toSession() = Session(
    token = token,
    expiresAt = expiresAt,
    account = Account(account.id, account.name, account.email, account.createdAt),
)
