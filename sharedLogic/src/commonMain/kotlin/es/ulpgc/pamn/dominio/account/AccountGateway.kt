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

    // Cierra la sesión en el servidor (la de este dispositivo). Tampoco lanza excepciones.
    // Borrarla del móvil (SessionStore) es cosa de quien llama.
    suspend fun logOut(session: Session): LogOutOutcome

    // Ajustes (docs/tareas/ajustes.md, fase 2). Las tres necesitan la sesión (su token) y no lanzan
    // excepciones: cualquier fallo llega como su desenlace.

    // Cambia el nombre visible. El correo no se puede cambiar (decisión F).
    suspend fun rename(session: Session, name: String): RenameOutcome

    // Cambia la contraseña; el servidor cierra las sesiones de los otros dispositivos (decisión G).
    suspend fun changePassword(session: Session, change: PasswordChange): ChangePasswordOutcome

    // Elimina la cuenta, confirmándolo con la contraseña (decisión H). Borrar la sesión del móvil es
    // cosa de quien llama.
    suspend fun deleteAccount(session: Session, password: String): DeleteAccountOutcome
}
