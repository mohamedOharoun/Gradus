package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// La puerta en el camino (9b) combinada con la hoja del módulo (ModuleSheet):
// - sin inscripción: tocar cualquier módulo saca NotEnrolled y no abre hoja ni práctica;
// - con inscripción: tocar un módulo desbloqueado abre su hoja y «Estudiar» abre la práctica.
// Curso A → B → C (GateTestFixtures).
@OptIn(ExperimentalCoroutinesApi::class)
class CourseViewModelGateTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Sin inscripción: ningún módulo abre la hoja ni la práctica ---

    @Test
    fun `not enrolled, tapping a frontier module gives the not enrolled notice and opens neither sheet nor practice`() = runTest {
        val viewModel = openPath(loggedInStore())
        assertEquals(ModuleState.FRONTIER, viewModel.ui().states.getValue("a"))
        viewModel.onModuleClick("a")
        assertEquals(CourseNotice.NotEnrolled, viewModel.ui().notice)
        assertNull(viewModel.ui().sheet)
        assertNull(viewModel.ui().practiceToOpen)
    }

    // El almacén real no deja dominar sin inscripción: lo dominado se fija desde fuera.
    @Test
    fun `not enrolled, tapping a mastered module gives the not enrolled notice and opens neither sheet nor practice`() = runTest {
        val viewModel = openPath(progress = FixedMasteredStore(setOf("a")), enrollment = loggedInStore())
        assertEquals(ModuleState.MASTERED, viewModel.ui().states.getValue("a"))
        viewModel.onModuleClick("a")
        assertEquals(CourseNotice.NotEnrolled, viewModel.ui().notice)
        assertNull(viewModel.ui().sheet)
        assertNull(viewModel.ui().practiceToOpen)
    }

    @Test
    fun `not enrolled, tapping a locked module gives the not enrolled notice, not the locked one`() = runTest {
        val viewModel = openPath(loggedInStore())
        viewModel.onModuleClick("c")
        assertEquals(CourseNotice.NotEnrolled, viewModel.ui().notice)
        assertNull(viewModel.ui().sheet)
        assertNull(viewModel.ui().practiceToOpen)
    }

    @Test
    fun `without a session the path is not enrolled and tapping gives the not enrolled notice`() = runTest {
        val viewModel = openPath(InMemoryProgressStore(InMemorySessionStore()))
        assertFalse(viewModel.ui().enrolled)
        viewModel.onModuleClick("a")
        assertEquals(CourseNotice.NotEnrolled, viewModel.ui().notice)
        assertNull(viewModel.ui().sheet)
        assertNull(viewModel.ui().practiceToOpen)
    }

    // --- Inscrito: abre la hoja del módulo y desde ahí la práctica ---

    @Test
    fun `enrolled, tapping a frontier module opens its sheet and study opens its practice`() = runTest {
        val viewModel = openPath(enrolledWithNodesMastered())
        viewModel.onModuleClick("b")
        assertEquals("b", viewModel.ui().sheet)
        assertNull(viewModel.ui().practiceToOpen)
        assertNull(viewModel.ui().notice)

        viewModel.onStudy()
        assertNull(viewModel.ui().sheet)
        assertEquals("b", viewModel.ui().practiceToOpen)
    }

    @Test
    fun `enrolled, tapping a mastered module opens its sheet and study opens its practice`() = runTest {
        val viewModel = openPath(enrolledWithNodesMastered())
        viewModel.onModuleClick("a")
        assertEquals("a", viewModel.ui().sheet)
        assertNull(viewModel.ui().practiceToOpen)
        assertNull(viewModel.ui().notice)

        viewModel.onStudy()
        assertNull(viewModel.ui().sheet)
        assertEquals("a", viewModel.ui().practiceToOpen)
    }

    @Test
    fun `enrolled, tapping a locked module gives the locked notice with what is missing`() = runTest {
        val viewModel = openPath(enrolledWithNodesMastered())
        viewModel.onModuleClick("c")
        assertEquals(CourseNotice.Locked(paths, listOf(edges)), viewModel.ui().notice)
        assertNull(viewModel.ui().sheet)
        assertNull(viewModel.ui().practiceToOpen)
    }

    @Test
    fun `tapping a module that does not exist changes nothing`() = runTest {
        val viewModel = openPath(enrolledWithNodesMastered())
        val before = viewModel.ui()
        viewModel.onModuleClick("no-existe")
        assertEquals(before, viewModel.ui())
    }

    // --- El camino escucha la inscripción ---

    @Test
    fun `enrolling with the path open makes the next tap open the sheet`() = runTest {
        val store = loggedInStore()
        val viewModel = openPath(store)
        assertFalse(viewModel.ui().enrolled)
        store.enroll(gateCourse.id)
        runCurrent()
        assertTrue(viewModel.ui().enrolled)
        viewModel.onModuleClick("a")
        assertEquals("a", viewModel.ui().sheet)
    }

    @Test
    fun `the path stays loading until the enrollment is known`() = runTest {
        val viewModel = openPath(progress = loggedInStore(), enrollment = SilentEnrollmentStore())
        assertEquals(CourseScreenState.Loading, viewModel.state)
    }

    // --- Lo que se conserva al llegar cambios ---

    @Test
    fun `a progress change keeps the notice`() = runTest {
        val store = loggedInStore()
        store.enroll(gateCourse.id)
        val viewModel = openPath(store)
        viewModel.onModuleClick("b")  // bloqueado: falta A
        val notice = viewModel.ui().notice
        store.markMastered(gateCourse.id, "a")
        runCurrent()
        assertEquals(setOf("a"), viewModel.ui().mastered)
        assertEquals(notice, viewModel.ui().notice)
    }

    @Test
    fun `an enrollment change keeps the notice`() = runTest {
        val store = loggedInStore()
        val viewModel = openPath(store)
        viewModel.onModuleClick("a")
        store.enroll(gateCourse.id)
        runCurrent()
        assertTrue(viewModel.ui().enrolled)
        assertEquals(CourseNotice.NotEnrolled, viewModel.ui().notice)
    }

    // No hay «darse de baja» (decisión 7): con la práctica pendiente solo puede cambiar el progreso.
    @Test
    fun `a progress change keeps the pending practice`() = runTest {
        val store = loggedInStore()
        store.enroll(gateCourse.id)
        val viewModel = openPath(store)
        viewModel.onModuleClick("a")
        viewModel.onStudy()
        store.markMastered(gateCourse.id, "a")
        runCurrent()
        assertEquals(setOf("a"), viewModel.ui().mastered)
        assertEquals("a", viewModel.ui().practiceToOpen)
    }

    // --- Factoría ---

    @Test
    fun `the factory builds a view model that uses the given stores`() = runTest {
        val store = enrolledWithNodesMastered()
        val viewModel = CourseViewModel.factory(gateCourse.id, FakeCourseGateway(listOf(gateCourse)), store, store)
            .create(CourseViewModel::class.java, MutableCreationExtras())
        runCurrent()
        assertTrue(viewModel.ui().enrolled)
        assertEquals(setOf("a"), viewModel.ui().mastered)
    }

    // --- Ayudantes ---

    private fun TestScope.openPath(store: InMemoryProgressStore) = openPath(progress = store, enrollment = store)

    private fun TestScope.openPath(progress: ProgressStore, enrollment: EnrollmentStore): CourseViewModel {
        val viewModel = CourseViewModel(gateCourse.id, FakeCourseGateway(listOf(gateCourse)), progress, enrollment)
        runCurrent()
        return viewModel
    }

    // Inscrito y con A dominado: A dominado, B frontera, C bloqueado (falta B).
    private suspend fun enrolledWithNodesMastered(): InMemoryProgressStore {
        val store = loggedInStore()
        store.enroll(gateCourse.id)
        store.markMastered(gateCourse.id, "a")
        return store
    }

    private fun CourseViewModel.ui() = (state as CourseScreenState.Ready).ui
}
