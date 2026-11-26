package es.ulpgc.pamn.dominio.ui.navigation

import androidx.datastore.core.CorruptionException
import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.data.session.SessionUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

// El arranque (11c-2) y salida unificada de Main: qué estado sale de lo que dice el almacén de la
// sesión. El Flow se controla a mano: un MutableSharedFlow no tiene valor hasta que el test emite,
// y uno con flow { } puede fallar.
@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {

    private val ana = Session("token-ana", "2026-12-18T10:00:00Z", Account("ana", "Ana", "ana@ulpgc.es", "2026-11-13T10:00:00Z"))
    private val beto = Session("token-beto", "2026-12-18T10:00:00Z", Account("beto", "Beto", "beto@ulpgc.es", "2026-11-13T10:00:00Z"))

    private val sessions = MutableSharedFlow<SessionUpdate>(extraBufferCapacity = 8)
    private var syncRequests = 0
    private val progressSync = ProgressSyncRequester { syncRequests++ }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Crea el ViewModel y deja que empiece a escuchar (con Eagerly, nada más crearlo).
    private fun TestScope.startViewModel(
        source: Flow<SessionUpdate> = sessions,
        sync: ProgressSyncRequester = progressSync,
    ): StartViewModel = StartViewModel(source, sync).also { runCurrent() }

    private fun TestScope.emit(update: SessionUpdate) {
        sessions.tryEmit(update)
        runCurrent()
    }

    private fun TestScope.emitActive(session: Session) = emit(SessionUpdate.Active(session))
    private fun TestScope.emitMissing(voluntary: Boolean = false) = emit(SessionUpdate.Missing(voluntary))

    // Un almacén cuya lectura falla las primeras veces (una por elemento) y después da `finally`.
    private class FlakySessions(private val failures: List<Throwable>, private val finally: SessionUpdate) {
        var reads = 0
            private set

        val flow: Flow<SessionUpdate> = flow {
            val attempt = reads++
            if (attempt < failures.size) throw failures[attempt]
            emit(finally)
        }
    }

    @Test
    fun `it is Loading until the first value arrives`() = runTest {
        val viewModel = startViewModel()
        assertEquals(StartState.Loading, viewModel.state.value)
    }

    @Test
    fun `with a saved session it is SignedIn`() = runTest {
        val viewModel = startViewModel()
        emitActive(ana)
        assertEquals(StartState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `without a saved session it is SignedOut to login`() = runTest {
        val viewModel = startViewModel()
        emitMissing()
        assertEquals(StartState.SignedOut(MainExit.ToLogin), viewModel.state.value)
    }

    @Test
    fun `when the session is deleted unexpectedly it goes from SignedIn to SignedOut ToLogin`() = runTest {
        val viewModel = startViewModel()
        emitActive(ana)
        emitMissing(voluntary = false)
        assertEquals(StartState.SignedOut(MainExit.ToLogin), viewModel.state.value)
    }

    @Test
    fun `when the user signs out voluntarily it goes from SignedIn to SignedOut ToWelcome`() = runTest {
        val viewModel = startViewModel()
        emitActive(ana)
        emitMissing(voluntary = true)
        assertEquals(StartState.SignedOut(MainExit.ToWelcome), viewModel.state.value)
    }

    @Test
    fun `when the user logs in it goes from SignedOut to SignedIn`() = runTest {
        val viewModel = startViewModel()
        emitMissing()
        emitActive(ana)
        assertEquals(StartState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `another session while signed in stays SignedIn`() = runTest {
        val viewModel = startViewModel()
        emitActive(ana)
        emitActive(beto)
        assertEquals(StartState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `an IOException when reading is SignedOut ToLogin and does not throw`() = runTest {
        val viewModel = startViewModel(FlakySessions(listOf(IOException("disk")), SessionUpdate.Missing(false)).flow)
        assertEquals(StartState.SignedOut(MainExit.ToLogin), viewModel.state.value)
    }

    @Test
    fun `a ProviderException when reading is SignedOut ToLogin and does not throw`() = runTest {
        val viewModel = startViewModel(FlakySessions(listOf(ProviderException("keystore busy")), SessionUpdate.Missing(false)).flow)
        assertEquals(StartState.SignedOut(MainExit.ToLogin), viewModel.state.value)
    }

    // DataStore avisa de un fichero ilegible con CorruptionException (es una IOException), pero su
    // ReplaceFileCorruptionHandler ya la convierte en «sin sesión»: aquí también valdría.
    @Test
    fun `a CorruptionException when reading is SignedOut ToLogin too`() = runTest {
        val viewModel = startViewModel(FlakySessions(listOf(CorruptionException("broken")), SessionUpdate.Missing(false)).flow)
        assertEquals(StartState.SignedOut(MainExit.ToLogin), viewModel.state.value)
    }

    @Test
    fun `after a failure it does not read again before the retry delay`() = runTest {
        val store = FlakySessions(listOf(IOException("disk")), SessionUpdate.Active(ana))
        startViewModel(store.flow)
        advanceTimeBy(1_999)
        runCurrent()
        assertEquals(1, store.reads)
    }

    @Test
    fun `after a failure it reads again every 2 seconds`() = runTest {
        val store = FlakySessions(listOf(IOException("disk")), SessionUpdate.Active(ana))
        startViewModel(store.flow)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(2, store.reads)
    }

    @Test
    fun `when the retry works it picks up the saved session`() = runTest {
        val store = FlakySessions(listOf(ProviderException("keystore busy")), SessionUpdate.Active(ana))
        val viewModel = startViewModel(store.flow)
        assertEquals(StartState.SignedOut(MainExit.ToLogin), viewModel.state.value)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(StartState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `while it keeps failing it stays SignedOut and keeps retrying`() = runTest {
        val store = FlakySessions(listOf(IOException("a"), IOException("b"), ProviderException("c")), SessionUpdate.Active(ana))
        val viewModel = startViewModel(store.flow)
        advanceTimeBy(2_000)
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(3, store.reads)
        assertEquals(StartState.SignedOut(MainExit.ToLogin), viewModel.state.value)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(4, store.reads)
        assertEquals(StartState.SignedIn, viewModel.state.value)
    }

    // Un error pasajero con la app ya dentro de Main (Android restauró la pila): el estado pasa a
    // SignedOut y la regla de navegación lleva a Login aunque la sesión siga guardada. Al leerse bien
    // llega SignedIn, pero el usuario se queda en Login: no entra solo. Nunca vuelve a Loading.
    @Test
    fun `a passing failure goes Loading then SignedOut then SignedIn, never back to Loading`() = runTest {
        val store = FlakySessions(listOf(IOException("disk")), SessionUpdate.Active(ana))
        val viewModel = StartViewModel(store.flow, progressSync)
        val seen = mutableListOf<StartState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect { seen += it } }
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(listOf(StartState.Loading, StartState.SignedOut(MainExit.ToLogin), StartState.SignedIn), seen)
    }

    @Test
    fun `an error that is not a read failure is not swallowed`() {
        assertThrows(IllegalStateException::class.java) {
            runTest {
                startViewModel(FlakySessions(listOf(IllegalStateException("bug")), SessionUpdate.Missing(false)).flow)
            }
        }
    }

    @Test
    fun `only IOException, CorruptionException and ProviderException are read failures`() {
        assertTrue(IOException("disk").isSessionReadFailure())
        assertTrue(CorruptionException("broken").isSessionReadFailure())
        assertTrue(ProviderException("keystore busy").isSessionReadFailure())
        assertFalse(IllegalStateException("bug").isSessionReadFailure())
        assertFalse(GeneralSecurityException("not a system failure").isSessionReadFailure())
        assertFalse(RuntimeException("other").isSessionReadFailure())
    }

    @Test
    fun `the state of a signed in user does not carry the token`() {
        assertFalse(StartState.SignedIn.toString().contains("token"))
    }

    @Test
    fun `the factory builds a view model that follows the session`() = runTest {
        val viewModel = StartViewModel.factory(sessions, progressSync).create(StartViewModel::class.java, MutableCreationExtras())
        runCurrent()
        emitActive(ana)
        assertEquals(StartState.SignedIn, viewModel.state.value)
    }

    // --- Sincronización al arrancar con sesión guardada (13b-2) ---

    @Test
    fun `while Loading it does not request sync`() = runTest {
        startViewModel()
        assertEquals(0, syncRequests)
    }

    @Test
    fun `when starting with a saved session it requests sync once`() = runTest {
        startViewModel()
        emitActive(ana)
        assertEquals(1, syncRequests)
    }

    @Test
    fun `when starting without a saved session it does not request sync`() = runTest {
        startViewModel()
        emitMissing()
        assertEquals(0, syncRequests)
    }

    @Test
    fun `logging in from SignedOut to SignedIn does not request sync`() = runTest {
        startViewModel()
        emitMissing()
        emitActive(ana)
        assertEquals(0, syncRequests)
    }

    @Test
    fun `another session while already signed in does not request sync again`() = runTest {
        startViewModel()
        emitActive(ana)
        emitActive(beto)
        assertEquals(1, syncRequests)
    }

    @Test
    fun `logging in again after session deletion does not request sync again`() = runTest {
        startViewModel()
        emitActive(ana)
        emitMissing()
        emitActive(beto)
        assertEquals(1, syncRequests)
    }

    @Test
    fun `a transient read failure followed by a saved session does not request sync`() = runTest {
        val store = FlakySessions(listOf(IOException("disk")), SessionUpdate.Active(ana))
        startViewModel(store.flow)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(0, syncRequests)
    }

    @Test
    fun `synchronous SignedOut then SignedIn does not request startup sync`() = runTest {
        val burst = flow {
            emit(SessionUpdate.Missing(false))
            emit(SessionUpdate.Active(ana))
        }
        val viewModel = startViewModel(burst)
        assertEquals(StartState.SignedIn, viewModel.state.value)
        assertEquals(0, syncRequests)
    }

    @Test
    fun `an unexpected read error does not request sync`() {
        assertThrows(IllegalStateException::class.java) {
            runTest {
                startViewModel(FlakySessions(listOf(IllegalStateException("bug")), SessionUpdate.Missing(false)).flow)
            }
        }
        assertEquals(0, syncRequests)
    }

    @Test
    fun `the factory wires the sync requester to the view model`() = runTest {
        StartViewModel.factory(sessions, progressSync).create(StartViewModel::class.java, MutableCreationExtras())
        runCurrent()
        emitActive(ana)
        assertEquals(1, syncRequests)
    }
}
