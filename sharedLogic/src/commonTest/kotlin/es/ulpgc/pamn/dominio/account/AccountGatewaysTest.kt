package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.account.http.HttpAccountGateway
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AccountGatewaysTest {

    private val newAccount = NewAccount("Ana", "ana@ulpgc.es", "Gradus2026!", acceptedTerms = true)

    @Test
    fun `http builds the HTTP adapter`() {
        assertIs<HttpAccountGateway>(AccountGateways.http("http://10.0.2.2:7070"))
    }

    @Test
    fun `the fake gateway answers what it is told`() = runTest {
        assertEquals(RegistrationOutcome.EmailTaken, FakeAccountGateway(RegistrationOutcome.EmailTaken).register(newAccount))
    }

    @Test
    fun `by default the fake gateway registers the account it receives`() = runTest {
        val outcome = FakeAccountGateway().register(newAccount)
        val account = assertIs<RegistrationOutcome.Registered>(outcome).account
        assertEquals("Ana", account.name)
        assertEquals("ana@ulpgc.es", account.email)
    }

    @Test
    fun `the fake gateway remembers what it was asked to register`() = runTest {
        val gateway = FakeAccountGateway()
        gateway.register(newAccount)
        assertEquals(listOf(newAccount), gateway.received)
    }

    // ── Inicio de sesión ──

    private val credentials = Credentials("ana@ulpgc.es", "Gradus2026!")

    @Test
    fun `by default the fake gateway logs in with the email it receives`() = runTest {
        val session = assertIs<LogInOutcome.LoggedIn>(FakeAccountGateway().logIn(credentials)).session
        assertEquals("ana@ulpgc.es", session.account.email)
    }

    @Test
    fun `the fake gateway can be told how to answer a log in`() = runTest {
        val gateway = FakeAccountGateway(LogInOutcome.InvalidCredentials)
        assertEquals(LogInOutcome.InvalidCredentials, gateway.logIn(credentials))
    }

    @Test
    fun `the fake gateway remembers the credentials it received`() = runTest {
        val gateway = FakeAccountGateway()
        gateway.logIn(credentials)
        assertEquals(listOf(credentials), gateway.receivedCredentials)
    }
}
