package es.ulpgc.pamn.dominio.account

// Todos los desenlaces de un registro, también los fallos de red: son valores, no excepciones.
// Es el espejo de RegistrationResult en el backend, más los casos que solo existen en el cliente.
sealed interface RegistrationOutcome {

    // 201: la cuenta se ha creado.
    data class Registered(val account: Account) : RegistrationOutcome

    // 400: el servidor no acepta algún dato. Contiene todos sus errores.
    data class Rejected(val errors: List<ServerFieldError>) : RegistrationOutcome

    // 409: ya hay una cuenta con ese correo.
    data object EmailTaken : RegistrationOutcome

    // No se ha podido hablar con el servidor (sin red, apagado o tiempo agotado). Se puede reintentar.
    data object Unreachable : RegistrationOutcome

    // El servidor ha respondido algo inesperado (5xx o un cuerpo que no se entiende).
    data object ServerFailure : RegistrationOutcome
}
