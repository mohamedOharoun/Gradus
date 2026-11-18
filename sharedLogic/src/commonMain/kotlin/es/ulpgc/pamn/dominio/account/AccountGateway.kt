package es.ulpgc.pamn.dominio.account

// Puerto (arquitectura hexagonal): lo que la app necesita del servidor de cuentas, sin decir cómo
// se habla con él. Hoy lo implementará un adaptador HTTP; mañana podría ser gRPC o uno falso para
// las previews y los tests. El ViewModel solo conoce esta interfaz.
interface AccountGateway {

    // suspend: la llamada tarda (va por la red), así que se espera sin bloquear la pantalla.
    // Nunca lanza excepciones: cualquier fallo llega como un RegistrationOutcome.
    suspend fun register(account: NewAccount): RegistrationOutcome

    // Comprueba el correo y la contraseña y, si son correctos, abre una sesión en este dispositivo.
    // Tampoco lanza excepciones: cualquier fallo llega como un LogInOutcome.
    suspend fun logIn(credentials: Credentials): LogInOutcome
}
