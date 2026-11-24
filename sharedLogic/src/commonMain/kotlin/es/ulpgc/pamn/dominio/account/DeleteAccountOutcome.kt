package es.ulpgc.pamn.dominio.account

// Todos los desenlaces de eliminar la cuenta (DELETE /api/users/me), también los fallos de red.
// Espejo de DeleteAccountResult en el backend, más los casos que solo existen en el cliente.
sealed interface DeleteAccountOutcome {

    // 204: la cuenta y sus sesiones ya no existen. Quien llama borra la sesión del móvil.
    data object Deleted : DeleteAccountOutcome

    // 400: falta la contraseña.
    data class Rejected(val errors: List<ServerFieldError>) : DeleteAccountOutcome

    // 403: la contraseña no es la de la cuenta.
    data object WrongPassword : DeleteAccountOutcome

    // 409: la cuenta ha publicado cursos y no se puede eliminar (decisión I de docs/tareas/ajustes.md).
    data object AuthorOfCourses : DeleteAccountOutcome

    // 401: la sesión ya no vale. Hay que volver a entrar.
    data object SessionExpired : DeleteAccountOutcome

    data object Unreachable : DeleteAccountOutcome

    data object ServerFailure : DeleteAccountOutcome
}
