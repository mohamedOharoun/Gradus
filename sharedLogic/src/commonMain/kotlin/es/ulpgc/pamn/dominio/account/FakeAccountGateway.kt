package es.ulpgc.pamn.dominio.account

// Gateway falso, sin red: para previews, tests del ViewModel y probar la app sin backend.
// Por defecto «crea» la cuenta que recibe; también se le puede decir qué responder.
class FakeAccountGateway(
    private val answer: (NewAccount) -> RegistrationOutcome = ::registered,
) : AccountGateway {

    // Responde siempre lo mismo, p. ej. FakeAccountGateway(RegistrationOutcome.Unreachable).
    constructor(outcome: RegistrationOutcome) : this({ outcome })

    // Lo que le han pedido registrar, en orden (para comprobarlo en los tests).
    val received: List<NewAccount> get() = requests.toList()
    private val requests = mutableListOf<NewAccount>()

    override suspend fun register(account: NewAccount): RegistrationOutcome {
        requests += account
        return answer(account)
    }
}

private fun registered(account: NewAccount) = RegistrationOutcome.Registered(
    Account(id = "fake-id", name = account.name, email = account.email, createdAt = "2026-01-01T00:00:00Z"),
)
