package es.ulpgc.pamn.dominio.ui.screens.settings

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.DeleteAccountOutcome
import es.ulpgc.pamn.dominio.account.FakeAccountGateway
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.model.DeleteAccountForm
import es.ulpgc.pamn.dominio.model.DeleteProblem
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.Submission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// El diálogo «Eliminar cuenta» de Ajustes (docs/tareas/ajustes.md, fase 2, tarea 8, decisiones H e I).
// Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val session = Session("token-de-prueba", "2026-12-17T10:00:00Z", Account("u1", "Ana", "ana@ejemplo.com", "2026-11-17T10:00:00Z"))
    private val sessions = InMemorySessionStore()

    private suspend fun TestScope.openWithPassword(gateway: FakeAccountGateway = FakeAccountGateway()): DeleteAccountViewModel {
        sessions.save(session)
        val viewModel = DeleteAccountViewModel(gateway, sessions)
        viewModel.onOpen()
        viewModel.onPasswordChange("Gradus2026!")
        runCurrent()
        return viewModel
    }

    private val DeleteAccountViewModel.form: DeleteAccountForm get() = (state as DeleteAccountState.Open).form

    // ── Abrir y cerrar ──

    @Test
    fun `it starts closed`() {
        assertEquals(DeleteAccountState.Closed, DeleteAccountViewModel(FakeAccountGateway(), sessions).state)
    }

    @Test
    fun `opening shows an empty form`() {
        val viewModel = DeleteAccountViewModel(FakeAccountGateway(), sessions)
        viewModel.onOpen()
        assertEquals(DeleteAccountState.Open(DeleteAccountForm()), viewModel.state)
    }

    // Al volver a abrirlo no queda la contraseña de antes.
    @Test
    fun `cancelling closes it and forgets the password`() = runTest {
        val viewModel = openWithPassword()
        viewModel.onDismiss()
        assertEquals(DeleteAccountState.Closed, viewModel.state)
        viewModel.onOpen()
        assertEquals("", viewModel.form.password)
    }

    // Mientras se borra no se puede cancelar: la petición ya ha salido.
    @Test
    fun `while deleting it cannot be cancelled`() = runTest {
        val viewModel = openWithPassword()
        viewModel.onConfirm()
        viewModel.onDismiss()
        assertEquals(Submission.Sending, viewModel.form.submission)
    }

    // ── Confirmar ──

    @Test
    fun `confirming without the password only says it is required`() = runTest {
        val gateway = FakeAccountGateway()
        sessions.save(session)
        val viewModel = DeleteAccountViewModel(gateway, sessions)
        viewModel.onOpen()

        viewModel.onConfirm()
        runCurrent()

        assertEquals(0, gateway.deletions)
        assertEquals(FieldError.EMPTY, viewModel.form.passwordError)
    }

    // Decisión H: borrada la cuenta, se olvida la sesión y se sale a Bienvenida.
    @Test
    fun `once deleted the session is forgotten and it ends logged out`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = openWithPassword(gateway)

        viewModel.onConfirm()
        runCurrent()

        assertEquals(1, gateway.deletions)
        assertNull(sessions.current())
        assertEquals(DeleteAccountState.LoggedOut, viewModel.state)
    }

    @Test
    fun `confirming twice deletes once`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = openWithPassword(gateway)

        viewModel.onConfirm()
        viewModel.onConfirm()
        runCurrent()

        assertEquals(1, gateway.deletions)
    }

    // ── Lo que responde el servidor ──

    @Test
    fun `a wrong password is told and the session stays`() = runTest {
        val viewModel = openWithPassword(FakeAccountGateway(DeleteAccountOutcome.WrongPassword))

        viewModel.onConfirm()
        runCurrent()

        assertEquals(DeleteProblem.WRONG_PASSWORD, viewModel.form.problem)
        assertEquals(session, sessions.current())
    }

    // Decisión I.
    @Test
    fun `the author of courses is told and the session stays`() = runTest {
        val viewModel = openWithPassword(FakeAccountGateway(DeleteAccountOutcome.AuthorOfCourses))

        viewModel.onConfirm()
        runCurrent()

        assertEquals(DeleteProblem.AUTHOR_OF_COURSES, viewModel.form.problem)
        assertEquals(session, sessions.current())
    }

    @Test
    fun `without connection it can be retried`() = runTest {
        val viewModel = openWithPassword(FakeAccountGateway(DeleteAccountOutcome.Unreachable))

        viewModel.onConfirm()
        runCurrent()

        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), viewModel.form.submission)
        assertEquals(session, sessions.current())
    }

    @Test
    fun `an expired session is forgotten and ends logged out`() = runTest {
        val viewModel = openWithPassword(FakeAccountGateway(DeleteAccountOutcome.SessionExpired))

        viewModel.onConfirm()
        runCurrent()

        assertNull(sessions.current())
        assertEquals(DeleteAccountState.LoggedOut, viewModel.state)
    }

    @Test
    fun `deleting the account uses the voluntary SessionSignOut command`() = runTest {
        sessions.save(session)
        var voluntarySignOuts = 0
        val viewModel = DeleteAccountViewModel(
            FakeAccountGateway(DeleteAccountOutcome.Deleted), sessions,
            signOut = {
                voluntarySignOuts++
                sessions.clear()
            },
        )
        viewModel.onOpen()
        viewModel.onPasswordChange("Gradus2026!")

        viewModel.onConfirm()
        runCurrent()

        assertEquals(1, voluntarySignOuts)
        assertNull(sessions.current())
        assertEquals(DeleteAccountState.LoggedOut, viewModel.state)
    }

    @Test
    fun `an expired session clears directly without calling voluntary SessionSignOut`() = runTest {
        sessions.save(session)
        var voluntarySignOuts = 0
        val viewModel = DeleteAccountViewModel(
            FakeAccountGateway(DeleteAccountOutcome.SessionExpired), sessions,
            signOut = {
                voluntarySignOuts++
                sessions.clear()
            },
        )
        viewModel.onOpen()
        viewModel.onPasswordChange("Gradus2026!")

        viewModel.onConfirm()
        runCurrent()

        assertEquals(0, voluntarySignOuts)
        assertNull(sessions.current())
        assertEquals(DeleteAccountState.LoggedOut, viewModel.state)
    }

    @Test
    fun `deleting the account clears local drafts before signing out`() = runTest {
        sessions.save(session)
        val events = mutableListOf<String>()
        val viewModel = DeleteAccountViewModel(
            FakeAccountGateway(DeleteAccountOutcome.Deleted),
            sessions,
            signOut = {
                events.add("signOut")
                sessions.clear()
            },
            clearUserDrafts = { userId -> events.add("clear:$userId") },
        )
        viewModel.onOpen()
        viewModel.onPasswordChange("Gradus2026!")

        viewModel.onConfirm()
        runCurrent()

        assertEquals(listOf("clear:u1", "signOut"), events)
        assertEquals(DeleteAccountState.LoggedOut, viewModel.state)
    }

    @Test
    fun `an expired session does not clear local drafts`() = runTest {
        sessions.save(session)
        var cleared = 0
        val viewModel = DeleteAccountViewModel(
            FakeAccountGateway(DeleteAccountOutcome.SessionExpired),
            sessions,
            clearUserDrafts = { cleared++ },
        )
        viewModel.onOpen()
        viewModel.onPasswordChange("Gradus2026!")

        viewModel.onConfirm()
        runCurrent()

        assertEquals(0, cleared)
        assertEquals(DeleteAccountState.LoggedOut, viewModel.state)
    }

    @Test
    fun `failed account deletions do not clear local drafts`() = runTest {
        val outcomes = listOf(
            DeleteAccountOutcome.WrongPassword,
            DeleteAccountOutcome.AuthorOfCourses,
            DeleteAccountOutcome.Unreachable,
        )

        outcomes.forEach { outcome ->
            sessions.save(session)
            var cleared = 0
            val viewModel = DeleteAccountViewModel(
                FakeAccountGateway(outcome),
                sessions,
                clearUserDrafts = { cleared++ },
            )
            viewModel.onOpen()
            viewModel.onPasswordChange("Gradus2026!")

            viewModel.onConfirm()
            runCurrent()

            assertEquals("should not clear drafts for $outcome", 0, cleared)
        }
    }
}
