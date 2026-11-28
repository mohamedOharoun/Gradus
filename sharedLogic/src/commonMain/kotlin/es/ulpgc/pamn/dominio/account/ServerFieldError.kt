package es.ulpgc.pamn.dominio.account

// Un error que devuelve el servidor para un campo, tal cual llega: {"field": "email", "code": "INVALID_EMAIL"}.
// El código se guarda como texto para no perder los códigos que el servidor añada en el futuro;
// apiCode lo lee como ApiErrorCode (UNKNOWN si la app todavía no lo conoce).
data class ServerFieldError(
    val field: String,
    val code: String,
) {
    val apiCode: ApiErrorCode get() = ApiErrorCode.fromName(code)
}
