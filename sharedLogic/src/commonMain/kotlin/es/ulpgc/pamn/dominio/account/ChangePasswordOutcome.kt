package es.ulpgc.pamn.dominio.account

// Todos los desenlaces de cambiar la contraseña (PUT /api/users/me/password), también los fallos de red.
// Espejo de ChangePasswordResult en el backend, más los casos que solo existen en el cliente.
sealed interface ChangePasswordOutcome {

    // 204: contraseña cambiada. Las sesiones de los otros dispositivos se han cerrado; esta sigue.
    data object Changed : ChangePasswordOutcome

    // 400: falta alguna o la nueva no cumple las reglas. Campos: currentPassword y newPassword.
    data class Rejected(val errors: List<ServerFieldError>) : ChangePasswordOutcome

    // 403: la contraseña actual no es la de la cuenta.
    data object WrongPassword : ChangePasswordOutcome

    // 401: la sesión ya no vale. Hay que volver a entrar.
    data object SessionExpired : ChangePasswordOutcome

    data object Unreachable : ChangePasswordOutcome

    data object ServerFailure : ChangePasswordOutcome
}
