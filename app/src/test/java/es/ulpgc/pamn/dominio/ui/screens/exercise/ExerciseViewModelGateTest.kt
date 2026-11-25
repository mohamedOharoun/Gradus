package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.StudyAccess
import es.ulpgc.pamn.dominio.ui.screens.course.SilentEnrollmentStore
import es.ulpgc.pamn.dominio.ui.screens.course.gateCourse
import es.ulpgc.pamn.dominio.ui.screens.course.loggedInStore
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

// La puerta en la práctica (9b): antes de enseñar nada se lee si hay inscripción. Sin ella, la
// pantalla vuelve atrás y la práctica nunca se compone (así no se llega a markMastered).
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelGateTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `access is unknown until the enrollment is read`() = runTest {
        val viewModel = openPractice(progress = loggedInStore(), enrollment = SilentEnrollmentStore())
        assertNull(viewModel.access)
    }

    @Test
    fun `not enrolled, the practice asks to go back`() = runTest {
        val viewModel = openPractice(loggedInStore())
        assertEquals(StudyAccess.NotEnrolled, viewModel.access)
    }

    @Test
    fun `without a session, the practice asks to go back`() = runTest {
        val viewModel = openPractice(InMemoryProgressStore(InMemorySessionStore()))
        assertEquals(StudyAccess.NotEnrolled, viewModel.access)
    }

    @Test
    fun `enrolled, the practice is shown`() = runTest {
        val store = loggedInStore()
        store.enroll(gateCourse.id)
        val viewModel = openPractice(store)
        assertEquals(StudyAccess.Allowed, viewModel.access)
        assertNotNull(viewModel.study)
    }

    // No hay «darse de baja» (decisión 7): basta con el primer valor y no se sigue escuchando.
    @Test
    fun `access is decided with the first value, enrolling later does not change it`() = runTest {
        val store = loggedInStore()
        val viewModel = openPractice(store)
        store.enroll(gateCourse.id)
        runCurrent()
        assertEquals(StudyAccess.NotEnrolled, viewModel.access)
    }

    @Test
    fun `the factory builds a view model that uses the given stores`() = runTest {
        val store = loggedInStore()
        store.enroll(gateCourse.id)
        val viewModel = ExerciseViewModel.factory(gateCourse, "a", store, store, Random(0))
            .create(ExerciseViewModel::class.java, MutableCreationExtras())
        runCurrent()
        assertEquals(StudyAccess.Allowed, viewModel.access)
    }

    // --- Ayudantes ---

    private fun TestScope.openPractice(store: InMemoryProgressStore) = openPractice(progress = store, enrollment = store)

    private fun TestScope.openPractice(progress: ProgressStore, enrollment: EnrollmentStore): ExerciseViewModel {
        val viewModel = ExerciseViewModel(gateCourse, "a", progress, enrollment, Random(0))
        runCurrent()
        return viewModel
    }
}
