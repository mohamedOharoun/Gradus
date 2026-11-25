package es.ulpgc.pamn.dominio.ui.screens.enrollment

import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.ui.components.EnrollmentState
import es.ulpgc.pamn.dominio.ui.screens.course.SilentEnrollmentStore
import es.ulpgc.pamn.dominio.ui.screens.course.gateCourse
import es.ulpgc.pamn.dominio.ui.screens.course.loggedInStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// El botón de inscribirse (10a): el ViewModel escucha enrolled(courseId) y expone un solo estado
// (EnrollmentState). Curso de prueba: gateCourse (GateTestFixtures).
@OptIn(ExperimentalCoroutinesApi::class)
class EnrollmentViewModelTest {

    private val courseId = gateCourse.id

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Lo que refleja ---

    @Test
    fun `before the first value the state is unknown`() = runTest {
        val viewModel = EnrollmentViewModel(courseId, SilentEnrollmentStore())
        runCurrent()
        assertEquals(EnrollmentState.Unknown, viewModel.state)
    }

    @Test
    fun `without enrollment the state is not enrolled`() = runTest {
        val viewModel = open(loggedInStore())
        assertEquals(EnrollmentState.NotEnrolled, viewModel.state)
    }

    @Test
    fun `already enrolled the state is enrolled`() = runTest {
        val store = loggedInStore()
        store.enroll(courseId)
        val viewModel = open(store)
        assertEquals(EnrollmentState.Enrolled, viewModel.state)
    }

    @Test
    fun `an enrollment made from outside is followed`() = runTest {
        val store = loggedInStore()
        val viewModel = open(store)
        assertEquals(EnrollmentState.NotEnrolled, viewModel.state)
        store.enroll(courseId)
        runCurrent()
        assertEquals(EnrollmentState.Enrolled, viewModel.state)
    }

    // --- Inscribirse ---

    @Test
    fun `enrolling changes the state to enrolled and enrolls in the store`() = runTest {
        val store = loggedInStore()
        val viewModel = open(store)
        viewModel.onEnroll()
        runCurrent()
        assertEquals(EnrollmentState.Enrolled, viewModel.state)
        assertTrue(store.enrolled(courseId).first())
    }

    @Test
    fun `while enrolling the state says so`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val viewModel = open(CountingEnrollmentStore(loggedInStore(), gate))
        viewModel.onEnroll()
        runCurrent()
        assertEquals(EnrollmentState.Enrolling, viewModel.state)
        gate.complete(Unit)
        runCurrent()
        assertEquals(EnrollmentState.Enrolled, viewModel.state)
    }

    @Test
    fun `two taps in a row enroll only once`() = runTest {
        val store = CountingEnrollmentStore(loggedInStore(), CompletableDeferred())
        val viewModel = open(store)
        viewModel.onEnroll()
        viewModel.onEnroll()
        runCurrent()
        assertEquals(1, store.calls)
    }

    @Test
    fun `tapping again once enrolled does not enroll again`() = runTest {
        val store = CountingEnrollmentStore(loggedInStore())
        val viewModel = open(store)
        viewModel.onEnroll()
        runCurrent()
        viewModel.onEnroll()
        runCurrent()
        assertEquals(1, store.calls)
    }

    @Test
    fun `tapping while the state is unknown does not enroll`() = runTest {
        val store = CountingEnrollmentStore(SilentEnrollmentStore())
        val viewModel = open(store)
        viewModel.onEnroll()
        runCurrent()
        assertEquals(0, store.calls)
        assertEquals(EnrollmentState.Unknown, viewModel.state)
    }

    // Si la inscripción llega de fuera mientras la nuestra sigue en marcha, ya está inscrito.
    @Test
    fun `an enrollment from outside while enrolling makes the state enrolled`() = runTest {
        val real = loggedInStore()
        val viewModel = open(CountingEnrollmentStore(real, CompletableDeferred()))
        viewModel.onEnroll()
        runCurrent()
        assertEquals(EnrollmentState.Enrolling, viewModel.state)
        real.enroll(courseId)
        runCurrent()
        assertEquals(EnrollmentState.Enrolled, viewModel.state)
    }

    // --- Sin sesión: fallo de programación, no se captura ---

    @Test
    fun `enrolling without a session fails with IllegalStateException and is not caught`() {
        assertThrows(IllegalStateException::class.java) {
            runTest {
                val viewModel = open(InMemoryProgressStore(InMemorySessionStore()))
                // Sin sesión enrolled emite false: el botón se ofrece, y enroll lanza.
                assertEquals(EnrollmentState.NotEnrolled, viewModel.state)
                viewModel.onEnroll()
                runCurrent()
            }
        }
    }

    // --- Factoría ---

    @Test
    fun `the factory builds a view model that uses the given store`() = runTest {
        val store = loggedInStore()
        store.enroll(courseId)
        val viewModel = EnrollmentViewModel.factory(courseId, store)
            .create(EnrollmentViewModel::class.java, MutableCreationExtras())
        runCurrent()
        assertEquals(EnrollmentState.Enrolled, viewModel.state)
    }

    // --- Ayudantes ---

    private fun TestScope.open(store: EnrollmentStore): EnrollmentViewModel {
        val viewModel = EnrollmentViewModel(courseId, store)
        runCurrent()
        return viewModel
    }
}

// Cuenta las llamadas a enroll y, si hay puerta (gate), no termina hasta que se abra: así se ve el
// estado «inscribiéndose». Lo demás lo delega en el almacén de verdad.
private class CountingEnrollmentStore(
    private val delegate: EnrollmentStore,
    private val gate: CompletableDeferred<Unit>? = null,
) : EnrollmentStore {
    var calls = 0
        private set

    override fun enrolled(courseId: String): Flow<Boolean> = delegate.enrolled(courseId)

    override suspend fun enroll(courseId: String) {
        calls++
        gate?.await()
        delegate.enroll(courseId)
    }
}
