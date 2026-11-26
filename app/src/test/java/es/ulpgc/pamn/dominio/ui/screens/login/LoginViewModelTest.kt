package es.ulpgc.pamn.dominio.ui.screens.login

import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.account.FakeAccountGateway
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.LogInOutcome
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
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
import org.junit.Before
import org.junit.Test

// Sincronizar al iniciar sesión (8b, C-5): se pide justo después de guardar la sesión, y solo si el
// inicio de sesión ha ido bien. El requester es una lambda que cuenta (fun interface).
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val sessions = SavedFlagSessionStore()
    private var requests = 0

    // Si la sesión ya estaba guardada en el momento de pedir sincronizar.
    private val savedOnRequest = mutableListOf<Boolean>()

    private val requester = ProgressSyncRequester {
        requests++
        savedOnRequest += sessions.saved
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a successful login requests a sync once`() = runTest {
        val viewModel = filledIn(FakeAccountGateway())
        viewModel.onSubmit()
        runCurrent()
        assertEquals(Submission.Succeeded, viewModel.form.submission)
        assertEquals(1, requests)
    }

    @Test
    fun `when the sync is requested the session is already saved`() = runTest {
        val viewModel = filledIn(FakeAccountGateway())
        viewModel.onSubmit()
        runCurrent()
        assertEquals(listOf(true), savedOnRequest)
    }

    @Test
    fun `invalid credentials request nothing`() = runTest {
        val viewModel = filledIn(FakeAccountGateway(LogInOutcome.InvalidCredentials))
        viewModel.onSubmit()
        runCurrent()
        assertEquals(0, requests)
    }

    @Test
    fun `an unreachable server requests nothing`() = runTest {
        val viewModel = filledIn(FakeAccountGateway(LogInOutcome.Unreachable))
        viewModel.onSubmit()
        runCurrent()
        assertEquals(0, requests)
    }

    @Test
    fun `a server failure requests nothing`() = runTest {
        val viewModel = filledIn(FakeAccountGateway(LogInOutcome.ServerFailure))
        viewModel.onSubmit()
        runCurrent()
        assertEquals(0, requests)
    }

    @Test
    fun `an invalid form requests nothing`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = LoginViewModel(gateway, sessions, requester)
        viewModel.onSubmit()
        runCurrent()
        assertEquals(0, gateway.receivedCredentials.size)
        assertEquals(0, requests)
    }

    @Test
    fun `submitting again while sending does not request twice`() = runTest {
        val viewModel = filledIn(FakeAccountGateway())
        viewModel.onSubmit()
        viewModel.onSubmit()
        runCurrent()
        assertEquals(1, requests)
    }

    @Test
    fun `the factory builds a view model that requests the sync`() = runTest {
        val viewModel = LoginViewModel.factory(FakeAccountGateway(), sessions, requester)
            .create(LoginViewModel::class.java, MutableCreationExtras())
        viewModel.onEmailChange("ana@example.com")
        viewModel.onPasswordChange("secreta")
        viewModel.onSubmit()
        runCurrent()
        assertEquals(1, requests)
    }

    // Una sesión en memoria que recuerda si se ha guardado ya (para comprobar el orden).
    private class SavedFlagSessionStore(private val delegate: SessionStore = InMemorySessionStore()) : SessionStore by delegate {
        var saved = false
            private set

        override suspend fun save(session: Session) {
            delegate.save(session)
            saved = true
        }
    }

    private fun TestScope.filledIn(gateway: FakeAccountGateway): LoginViewModel {
        val viewModel = LoginViewModel(gateway, sessions, requester)
        viewModel.onEmailChange("ana@example.com")
        viewModel.onPasswordChange("secreta")
        runCurrent()
        return viewModel
    }
}
