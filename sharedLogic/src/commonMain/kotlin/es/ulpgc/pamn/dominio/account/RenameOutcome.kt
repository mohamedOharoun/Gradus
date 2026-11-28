package es.ulpgc.pamn.dominio.account

// Todos los desenlaces de cambiar el nombre (PATCH /api/users/me), también los fallos de red.
// Espejo de RenameResult en el backend, más los casos que solo existen en el cliente.
sealed interface RenameOutcome {

    // 200: nombre cambiado. `account` es la cuenta ya renombrada (con el nombre como lo guardó el servidor).
    data class Renamed(val account: Account) : RenameOutcome

    // 400: el nombre no vale (TOO_SHORT, TOO_LONG, REQUIRED).
    data class Rejected(val errors: List<ServerFieldError>) : RenameOutcome

    // 401: la sesión ya no vale (caducada, cerrada desde otro móvil o cuenta eliminada). Hay que volver a entrar.
    data object SessionExpired : RenameOutcome

    // No se ha podido hablar con el servidor (sin red, apagado o tiempo agotado). Se puede reintentar.
    data object Unreachable : RenameOutcome

    // El servidor ha respondido algo inesperado (5xx o un cuerpo que no se entiende).
    data object ServerFailure : RenameOutcome
}
