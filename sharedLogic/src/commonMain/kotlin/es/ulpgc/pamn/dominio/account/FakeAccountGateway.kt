package es.ulpgc.pamn.dominio.account

// Gateway falso, sin red: para previews, tests del ViewModel y probar la app sin backend.
// Por defecto «crea» la cuenta y «abre» la sesión que recibe; también se le puede decir qué responder.
class FakeAccountGateway(
    private val answer: (NewAccount) -> RegistrationOutcome = ::registered,
    private val logInAnswer: (Credentials) -> LogInOutcome = ::loggedIn,
) : AccountGateway {

    // Responde siempre lo mismo al registro, p. ej. FakeAccountGateway(RegistrationOutcome.Unreachable).
    constructor(outcome: RegistrationOutcome) : this(answer = { outcome })

    // Responde siempre lo mismo al inicio de sesión, p. ej. FakeAccountGateway(LogInOutcome.InvalidCredentials).
    constructor(outcome: LogInOutcome) : this(logInAnswer = { outcome })

    // Lo que le han pedido, en orden (para comprobarlo en los tests).
    val received: List<NewAccount> get() = requests.toList()
    val receivedCredentials: List<Credentials> get() = logInRequests.toList()
    private val requests = mutableListOf<NewAccount>()
    private val logInRequests = mutableListOf<Credentials>()

    override suspend fun register(account: NewAccount): RegistrationOutcome {
        requests += account
        return answer(account)
    }

    override suspend fun logIn(credentials: Credentials): LogInOutcome {
        logInRequests += credentials
        return logInAnswer(credentials)
    }
}

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
