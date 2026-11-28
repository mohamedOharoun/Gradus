package es.ulpgc.pamn.dominio.account

// Una cuenta que ya existe en el servidor. Nunca lleva la contraseña.
// createdAt es el texto ISO-8601 que envía el servidor (p. ej. 2026-11-13T10:00:00.123456Z):
// de momento solo se muestra, así que no hace falta una librería de fechas.
data class Account(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: String,
)
