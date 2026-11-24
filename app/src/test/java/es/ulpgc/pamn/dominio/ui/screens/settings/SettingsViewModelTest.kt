package es.ulpgc.pamn.dominio.ui.screens.settings

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.FakeAccountGateway
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.LogOutOutcome
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.settings.AppSettings
import es.ulpgc.pamn.dominio.settings.InMemorySettingsStore
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

// Ajustes (ajustes.md, tarea 6): el perfil sale de la sesión, el modo oscuro del store y
// «Cerrar sesión» avisa al servidor y borra la sesión del móvil aunque el servidor falle (decisión C).
// Tiempo simulado, como CourseDetailViewModelTest: nada se ejecuta hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val ana = Account("u1", "Ana Ruiz", "ana@ejemplo.com", "2026-11-17T10:00:00Z")
    private val session = Session("token-de-prueba", "2026-12-17T10:00:00Z", ana)
    private val sessions = InMemorySessionStore()
    private val settings = InMemorySettingsStore()

    private suspend fun TestScope.loggedIn(gateway: FakeAccountGateway = FakeAccountGateway()): SettingsViewModel {
        sessions.save(session)
        val viewModel = SettingsViewModel(gateway, sessions, settings)
        runCurrent()
        return viewModel
    }

    // ── El perfil ──

    @Test
    fun `it starts loading`() = runTest {
        sessions.save(session)
        assertEquals(SettingsState.Loading, SettingsViewModel(FakeAccountGateway(), sessions, settings).state)
    }

    @Test
    fun `the profile is the account of the session`() = runTest {
        assertEquals(SettingsState.Ready(ana), loggedIn().state)
    }

    // No debería pasar (a Ajustes se llega con sesión), pero si no la hay, se sale a Bienvenida.
    @Test
    fun `without a session it is logged out`() = runTest {
        val viewModel = SettingsViewModel(FakeAccountGateway(), sessions, settings)
        runCurrent()

        assertEquals(SettingsState.LoggedOut, viewModel.state)
    }

    // Al volver de «Editar perfil», Ajustes vuelve a leer la sesión y enseña el nombre nuevo.
    @Test
    fun `on resume it reads the session again`() = runTest {
        val viewModel = loggedIn()
        sessions.save(session.copy(account = ana.copy(name = "Ana María")))

        viewModel.onResume()
        runCurrent()

        assertEquals(SettingsState.Ready(ana.copy(name = "Ana María")), viewModel.state)
    }

    // Si ya se está cerrando la sesión, volver a leerla no lo deshace.
    @Test
    fun `on resume while logging out nothing changes`() = runTest {
        val viewModel = loggedIn()
        viewModel.onLogOut()

        viewModel.onResume()

        assertEquals(SettingsState.Ready(ana, loggingOut = true), viewModel.state)
    }

    // ── El modo oscuro ──

    @Test
    fun `the dark mode is the one of the store`() = runTest {
        settings.setDarkMode(true)

        assertEquals(AppSettings(darkMode = true), loggedIn().appSettings.value)
    }

    @Test
    fun `touching the switch saves the dark mode`() = runTest {
        val viewModel = loggedIn()

        viewModel.onDarkModeChange(true)
        runCurrent()

        assertEquals(AppSettings(darkMode = true), settings.settings.value)
        assertEquals(AppSettings(darkMode = true), viewModel.appSettings.value)
    }

    // ── Cerrar sesión ──

    @Test
    fun `logging out tells the server which session to close`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = loggedIn(gateway)

        viewModel.onLogOut()
        runCurrent()

        assertEquals(listOf(session), gateway.closedSessions)
    }

    @Test
    fun `logging out forgets the session and ends logged out`() = runTest {
        val viewModel = loggedIn()

        viewModel.onLogOut()
        runCurrent()

        assertNull(sessions.current())
        assertEquals(SettingsState.LoggedOut, viewModel.state)
    }

    // Decisión C: el usuario quiere salir; la sesión del servidor caduca sola.
    @Test
    fun `without connection it logs out anyway`() = runTest {
        val viewModel = loggedIn(FakeAccountGateway(LogOutOutcome.Unreachable))

        viewModel.onLogOut()
        runCurrent()

        assertNull(sessions.current())
        assertEquals(SettingsState.LoggedOut, viewModel.state)
    }

    @Test
    fun `if the server fails it logs out anyway`() = runTest {
        val viewModel = loggedIn(FakeAccountGateway(LogOutOutcome.ServerFailure))

        viewModel.onLogOut()
        runCurrent()

        assertNull(sessions.current())
        assertEquals(SettingsState.LoggedOut, viewModel.state)
    }

    // Mientras se cierra, el estado lo dice (y la pantalla no vuelve a pedirlo).
    @Test
    fun `while logging out the state says so`() = runTest {
        val viewModel = loggedIn()

        viewModel.onLogOut()

        assertEquals(SettingsState.Ready(ana, loggingOut = true), viewModel.state)
    }

    // Dos toques rápidos: el servidor recibe una sola petición.
    @Test
    fun `touching log out twice asks the server once`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = loggedIn(gateway)

        viewModel.onLogOut()
        viewModel.onLogOut()
        runCurrent()

        assertEquals(1, gateway.closedSessions.size)
    }

    // Antes de tener el perfil no hay nada que cerrar todavía.
    @Test
    fun `logging out before the profile is loaded does nothing`() = runTest {
        sessions.save(session)
        val gateway = FakeAccountGateway()
        val viewModel = SettingsViewModel(gateway, sessions, settings)

        viewModel.onLogOut()
        runCurrent()

        assertEquals(emptyList<Session>(), gateway.closedSessions)
        assertEquals(SettingsState.Ready(ana), viewModel.state)
    }
}
