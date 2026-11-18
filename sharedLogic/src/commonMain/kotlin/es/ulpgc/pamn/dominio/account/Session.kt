package es.ulpgc.pamn.dominio.account

// Una sesión iniciada en este dispositivo. token es la llave que la app enviará en cada petición
// (cabecera Authorization: Bearer <token>); expiresAt, hasta cuándo vale si no se usa (ISO-8601).
data class Session(
    val token: String,
    val expiresAt: String,
    val account: Account,
) {
    // Quien tenga el token tiene la sesión: el toString generado lo mostraría en los logs.
    override fun toString() = "Session(token=****, expiresAt=$expiresAt, account=$account)"
}
