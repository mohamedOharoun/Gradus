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

    // ── Cerrar sesión ──

    private val session = Session("tok", "2026-12-22T10:00:00Z", Account("u1", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))

    @Test
    fun `by default the fake gateway logs out`() = runTest {
        assertEquals(LogOutOutcome.LoggedOut, FakeAccountGateway().logOut(session))
    }

    @Test
    fun `the fake gateway can be told how to answer a log out`() = runTest {
        assertEquals(LogOutOutcome.Unreachable, FakeAccountGateway(LogOutOutcome.Unreachable).logOut(session))
    }

    @Test
    fun `the fake gateway remembers which session it was asked to close`() = runTest {
        val gateway = FakeAccountGateway()
        gateway.logOut(session)
        assertEquals(listOf(session), gateway.closedSessions)
    }

    // ── Ajustes (fase 2) ──

    // Usa la misma `session` que los tests de cerrar sesión.

    @Test
    fun `by default the fake gateway renames the account of the session like the server`() = runTest {
        assertEquals(
            RenameOutcome.Renamed(session.account.copy(name = "Ana Ruiz")),
            FakeAccountGateway().rename(session, "  Ana Ruiz  "),
        )
    }

    @Test
    fun `the fake gateway can be told how to answer a rename and remembers the name`() = runTest {
        val gateway = FakeAccountGateway(RenameOutcome.Unreachable)
        assertEquals(RenameOutcome.Unreachable, gateway.rename(session, "Ana Ruiz"))
        assertEquals(listOf("Ana Ruiz"), gateway.receivedNames)
    }

    @Test
    fun `by default the fake gateway changes the password and remembers it`() = runTest {
        val gateway = FakeAccountGateway()
        val change = PasswordChange("Gradus2026!", "Nueva2026?")
        assertEquals(ChangePasswordOutcome.Changed, gateway.changePassword(session, change))
        assertEquals(listOf(change), gateway.receivedPasswordChanges)
    }

    @Test
    fun `the fake gateway can be told how to answer a password change`() = runTest {
        assertEquals(
            ChangePasswordOutcome.WrongPassword,
            FakeAccountGateway(ChangePasswordOutcome.WrongPassword).changePassword(session, PasswordChange("a", "b")),
        )
    }

    @Test
    fun `by default the fake gateway deletes the account and counts it`() = runTest {
        val gateway = FakeAccountGateway()
        assertEquals(DeleteAccountOutcome.Deleted, gateway.deleteAccount(session, "Gradus2026!"))
        assertEquals(1, gateway.deletions)
    }

    @Test
    fun `the fake gateway can be told how to answer a deletion`() = runTest {
        assertEquals(
            DeleteAccountOutcome.AuthorOfCourses,
            FakeAccountGateway(DeleteAccountOutcome.AuthorOfCourses).deleteAccount(session, "Gradus2026!"),
        )
    }
}
