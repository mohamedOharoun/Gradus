package es.ulpgc.pamn.dominio.account

// Todos los desenlaces de un inicio de sesión, también los fallos de red: son valores, no excepciones.
// Es el espejo de LogInResult en el backend, más los casos que solo existen en el cliente.
sealed interface LogInOutcome {

    // 201: sesión iniciada.
    data class LoggedIn(val session: Session) : LogInOutcome

    // 400: falta algún dato o el correo no tiene formato válido.
    data class Rejected(val errors: List<ServerFieldError>) : LogInOutcome

    // 401: correo o contraseña incorrectos. No se sabe cuál de los dos, a propósito.
    data object InvalidCredentials : LogInOutcome

    // No se ha podido hablar con el servidor (sin red, apagado o tiempo agotado). Se puede reintentar.
    data object Unreachable : LogInOutcome

    // El servidor ha respondido algo inesperado (5xx o un cuerpo que no se entiende).
    data object ServerFailure : LogInOutcome
}
