package es.ulpgc.pamn.dominio.ui.screens.password

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.ChangePasswordOutcome
import es.ulpgc.pamn.dominio.account.FakeAccountGateway
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.PasswordChange
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.PasswordForm
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// «Cambiar contraseña» (docs/tareas/ajustes.md, fase 2, tarea 7). Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

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

    private suspend fun TestScope.filledIn(gateway: FakeAccountGateway = FakeAccountGateway()): ChangePasswordViewModel {
        sessions.save(session)
        val viewModel = ChangePasswordViewModel(gateway, sessions)
        viewModel.onCurrentChange("Gradus2026!")
        viewModel.onNextChange("Nueva2026?")
        viewModel.onConfirmationChange("Nueva2026?")
        runCurrent()
        return viewModel
    }

    private val ChangePasswordViewModel.form: PasswordForm get() = (state as ChangePasswordState.Editing).form

    @Test
    fun `it starts empty`() {
        assertEquals(ChangePasswordState.Editing(PasswordForm()), ChangePasswordViewModel(FakeAccountGateway(), sessions).state)
    }

    @Test
    fun `typing fills the three fields`() = runTest {
        val form = filledIn().form
        assertEquals(Triple("Gradus2026!", "Nueva2026?", "Nueva2026?"), Triple(form.current, form.next, form.confirmation))
    }

    @Test
    fun `saving sends both passwords as typed`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = filledIn(gateway)

        viewModel.onSave()
        runCurrent()

        assertEquals(listOf(PasswordChange("Gradus2026!", "Nueva2026?")), gateway.receivedPasswordChanges)
        assertEquals(Submission.Succeeded, viewModel.form.submission)
    }

    // Decisión G: el servidor cierra las otras sesiones; la de este móvil sigue igual.
    @Test
    fun `once changed the session of this phone stays`() = runTest {
        val viewModel = filledIn()
        viewModel.onSave()
        runCurrent()
        assertEquals(session, sessions.current())
    }

    @Test
    fun `while saving the state says so`() = runTest {
        val viewModel = filledIn()
        viewModel.onSave()
        assertEquals(Submission.Sending, viewModel.form.submission)
    }

    @Test
    fun `saving twice sends once`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = filledIn(gateway)

        viewModel.onSave()
        viewModel.onSave()
        runCurrent()

        assertEquals(1, gateway.receivedPasswordChanges.size)
    }

    @Test
    fun `an incomplete form is not sent and shows its errors`() = runTest {
        val gateway = FakeAccountGateway()
        sessions.save(session)
        val viewModel = ChangePasswordViewModel(gateway, sessions)

        viewModel.onSave()
        runCurrent()

        assertEquals(emptyList<PasswordChange>(), gateway.receivedPasswordChanges)
        assertEquals(FieldError.EMPTY, viewModel.form.currentError)
    }

    @Test
    fun `a wrong current password is told under it`() = runTest {
        val viewModel = filledIn(FakeAccountGateway(ChangePasswordOutcome.WrongPassword))

        viewModel.onSave()
        runCurrent()

        assertTrue(viewModel.form.isWrongCurrent)
    }

    @Test
    fun `without connection it can be retried`() = runTest {
        val viewModel = filledIn(FakeAccountGateway(ChangePasswordOutcome.Unreachable))

        viewModel.onSave()
        runCurrent()

        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), viewModel.form.submission)
    }

    @Test
    fun `an expired session is forgotten and ends logged out`() = runTest {
        val viewModel = filledIn(FakeAccountGateway(ChangePasswordOutcome.SessionExpired))

        viewModel.onSave()
        runCurrent()

        assertNull(sessions.current())
        assertEquals(ChangePasswordState.LoggedOut, viewModel.state)
    }

    @Test
    fun `without a session saving ends logged out`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = ChangePasswordViewModel(gateway, sessions)
        viewModel.onCurrentChange("Gradus2026!")
        viewModel.onNextChange("Nueva2026?")
        viewModel.onConfirmationChange("Nueva2026?")

        viewModel.onSave()
        runCurrent()

        assertEquals(ChangePasswordState.LoggedOut, viewModel.state)
        assertEquals(emptyList<PasswordChange>(), gateway.receivedPasswordChanges)
    }
}
