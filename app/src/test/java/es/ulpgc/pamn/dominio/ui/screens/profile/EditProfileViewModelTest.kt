package es.ulpgc.pamn.dominio.ui.screens.profile

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.FakeAccountGateway
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.RenameOutcome
import es.ulpgc.pamn.dominio.account.ServerFieldError
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.ProfileForm
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

// «Editar perfil» (docs/tareas/ajustes.md, fase 2, tarea 6): cambia el nombre en el servidor y
// guarda en el móvil la sesión con el nombre nuevo. Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val ana = Account("u1", "Ana", "ana@ejemplo.com", "2026-11-17T10:00:00Z")
    private val session = Session("token-de-prueba", "2026-12-17T10:00:00Z", ana)
    private val sessions = InMemorySessionStore()

    private suspend fun TestScope.editing(gateway: FakeAccountGateway = FakeAccountGateway()): EditProfileViewModel {
        sessions.save(session)
        val viewModel = EditProfileViewModel(gateway, sessions)
        runCurrent()
        return viewModel
    }

    private val EditProfileViewModel.form: ProfileForm get() = (state as EditProfileState.Editing).form

    // ── Al abrir ──

    @Test
    fun `it starts loading`() = runTest {
        sessions.save(session)
        assertEquals(EditProfileState.Loading, EditProfileViewModel(FakeAccountGateway(), sessions).state)
    }

    @Test
    fun `it shows the name and the email of the account`() = runTest {
        assertEquals(EditProfileState.Editing(email = "ana@ejemplo.com", form = ProfileForm(original = "Ana")), editing().state)
    }

    @Test
    fun `without a session it is logged out`() = runTest {
        val viewModel = EditProfileViewModel(FakeAccountGateway(), sessions)
        runCurrent()
        assertEquals(EditProfileState.LoggedOut, viewModel.state)
    }

    // ── Escribir ──

    @Test
    fun `typing changes the name`() = runTest {
        val viewModel = editing()
        viewModel.onNameChange("Ana Ruiz")
        assertEquals("Ana Ruiz", viewModel.form.name)
    }

    // ── Guardar ──

    @Test
    fun `saving sends the name without surrounding spaces`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = editing(gateway)

        viewModel.onNameChange("  Ana Ruiz  ")
        viewModel.onSave()
        runCurrent()

        assertEquals(listOf("Ana Ruiz"), gateway.receivedNames)
        assertEquals(Submission.Succeeded, viewModel.form.submission)
    }

    // Ajustes y el resto de la app leen la sesión del móvil: debe llevar el nombre nuevo.
    @Test
    fun `once saved the session of the phone has the new name`() = runTest {
        val viewModel = editing()

        viewModel.onNameChange("Ana Ruiz")
        viewModel.onSave()
        runCurrent()

        assertEquals(session.copy(account = ana.copy(name = "Ana Ruiz")), sessions.current())
    }

    @Test
    fun `while saving the state says so`() = runTest {
        val viewModel = editing()
        viewModel.onNameChange("Ana Ruiz")
        viewModel.onSave()
        assertEquals(Submission.Sending, viewModel.form.submission)
    }

    @Test
    fun `saving twice sends once`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = editing(gateway)

        viewModel.onNameChange("Ana Ruiz")
        viewModel.onSave()
        viewModel.onSave()
        runCurrent()

        assertEquals(1, gateway.receivedNames.size)
    }

    @Test
    fun `without changes nothing is sent`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = editing(gateway)

        viewModel.onSave()
        runCurrent()

        assertEquals(emptyList<String>(), gateway.receivedNames)
    }

    @Test
    fun `an invalid name is not sent and shows its error`() = runTest {
        val gateway = FakeAccountGateway()
        val viewModel = editing(gateway)

        viewModel.onNameChange("A")
        viewModel.onSave()
        runCurrent()

        assertEquals(emptyList<String>(), gateway.receivedNames)
        assertEquals(FieldError.TOO_SHORT, viewModel.form.nameError)
    }

    // ── Lo que responde el servidor ──

    @Test
    fun `the error of the server shows under the name and the session does not change`() = runTest {
        val viewModel = editing(FakeAccountGateway(RenameOutcome.Rejected(listOf(ServerFieldError("name", "TOO_LONG")))))

        viewModel.onNameChange("Ana Ruiz")
        viewModel.onSave()
        runCurrent()

        assertEquals(FieldError.TOO_LONG, viewModel.form.nameError)
        assertEquals(session, sessions.current())
    }

    @Test
    fun `without connection it can be retried`() = runTest {
        val viewModel = editing(FakeAccountGateway(RenameOutcome.Unreachable))

        viewModel.onNameChange("Ana Ruiz")
        viewModel.onSave()
        runCurrent()

        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), viewModel.form.submission)
        assertEquals(session, sessions.current())
    }

    // La sesión ya no vale (p. ej. se cambió la contraseña desde otro móvil): se borra y se sale.
    @Test
    fun `an expired session is forgotten and ends logged out`() = runTest {
        val viewModel = editing(FakeAccountGateway(RenameOutcome.SessionExpired))

        viewModel.onNameChange("Ana Ruiz")
        viewModel.onSave()
        runCurrent()

        assertNull(sessions.current())
        assertEquals(EditProfileState.LoggedOut, viewModel.state)
    }
}
