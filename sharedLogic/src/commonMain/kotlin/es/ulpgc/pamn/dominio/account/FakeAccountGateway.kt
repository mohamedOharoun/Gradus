package es.ulpgc.pamn.dominio.account

// Gateway falso, sin red: para previews, tests del ViewModel y probar la app sin backend.
// Por defecto «crea» la cuenta y «abre» la sesión que recibe; también se le puede decir qué responder.
class FakeAccountGateway(
    private val answer: (NewAccount) -> RegistrationOutcome = ::registered,
    private val logInAnswer: (Credentials) -> LogInOutcome = ::loggedIn,
    private val logOutAnswer: (Session) -> LogOutOutcome = { LogOutOutcome.LoggedOut },
    private val renameAnswer: (Session, String) -> RenameOutcome = ::renamed,
    private val changePasswordAnswer: (PasswordChange) -> ChangePasswordOutcome = { ChangePasswordOutcome.Changed },
    private val deleteAnswer: (String) -> DeleteAccountOutcome = { DeleteAccountOutcome.Deleted },
) : AccountGateway {

    // Responde siempre lo mismo al registro, p. ej. FakeAccountGateway(RegistrationOutcome.Unreachable).
    constructor(outcome: RegistrationOutcome) : this(answer = { outcome })

    // Responde siempre lo mismo al inicio de sesión, p. ej. FakeAccountGateway(LogInOutcome.InvalidCredentials).
    constructor(outcome: LogInOutcome) : this(logInAnswer = { outcome })

    // Responde siempre lo mismo al cerrar sesión, p. ej. FakeAccountGateway(LogOutOutcome.Unreachable).
    constructor(outcome: LogOutOutcome) : this(logOutAnswer = { outcome })

    // Ajustes: responden siempre lo mismo, p. ej. FakeAccountGateway(DeleteAccountOutcome.AuthorOfCourses).
    constructor(outcome: RenameOutcome) : this(renameAnswer = { _, _ -> outcome })
    constructor(outcome: ChangePasswordOutcome) : this(changePasswordAnswer = { outcome })
    constructor(outcome: DeleteAccountOutcome) : this(deleteAnswer = { outcome })

    // Lo que le han pedido, en orden (para comprobarlo en los tests).
    val received: List<NewAccount> get() = requests.toList()
    val receivedCredentials: List<Credentials> get() = logInRequests.toList()
    private val requests = mutableListOf<NewAccount>()
    private val logInRequests = mutableListOf<Credentials>()
    val closedSessions: List<Session> get() = logOutRequests.toList()
    private val logOutRequests = mutableListOf<Session>()
    val receivedNames: List<String> get() = renameRequests.toList()
    private val renameRequests = mutableListOf<String>()
    val receivedPasswordChanges: List<PasswordChange> get() = passwordChanges.toList()
    private val passwordChanges = mutableListOf<PasswordChange>()
    val deletions: Int get() = deleteRequests
    private var deleteRequests = 0

    override suspend fun register(account: NewAccount): RegistrationOutcome {
        requests += account
        return answer(account)
    }

    override suspend fun logIn(credentials: Credentials): LogInOutcome {
        logInRequests += credentials
        return logInAnswer(credentials)
    }

    override suspend fun logOut(session: Session): LogOutOutcome {
        logOutRequests += session
        return logOutAnswer(session)
    }

    override suspend fun rename(session: Session, name: String): RenameOutcome {
        renameRequests += name
        return renameAnswer(session, name)
    }

    override suspend fun changePassword(session: Session, change: PasswordChange): ChangePasswordOutcome {
        passwordChanges += change
        return changePasswordAnswer(change)
    }

    override suspend fun deleteAccount(session: Session, password: String): DeleteAccountOutcome {
        deleteRequests++
        return deleteAnswer(password)
    }
}

// Como el servidor: la cuenta de la sesión con el nombre nuevo, sin espacios alrededor.
private fun renamed(session: Session, name: String) =
    RenameOutcome.Renamed(session.account.copy(name = name.trim()))

private fun registered(account: NewAccount) = RegistrationOutcome.Registered(
    Account(id = "fake-id", name = account.name, email = account.email, createdAt = "2026-01-01T00:00:00Z"),
)

private fun loggedIn(credentials: Credentials) = LogInOutcome.LoggedIn(
    Session(
        token = "fake-token",
        expiresAt = "2026-02-01T00:00:00Z",
        account = Account(id = "fake-id", name = "Fake", email = credentials.email, createdAt = "2026-01-01T00:00:00Z"),
    ),
)
