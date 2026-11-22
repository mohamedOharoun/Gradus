package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// La ficha de un curso: descarga el curso de la ruta y escucha el progreso (ficha-curso.md, tarea 4).
// Tiempo simulado, como SearchViewModelTest: nada se ejecuta hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val graphs = Course("g", "Grafos", listOf(Module("repr", "Representar"), Module("bfs", "BFS", requires = listOf("repr"))))
    private val progress = InMemoryProgressStore()

    // Responde `outcomes` en orden, uno por petición (el último se repite).
    private fun gatewayAnswering(vararg outcomes: CourseOutcome): FakeCourseGateway {
        var asked = 0
        return FakeCourseGateway(courseAnswer = { outcomes[minOf(asked++, outcomes.lastIndex)] })
    }

    @Test
    fun `it starts downloading the course of the route`() = runTest {
        val gateway = FakeCourseGateway(listOf(graphs))
        val viewModel = CourseDetailViewModel("g", gateway, progress)

        assertEquals(CourseDetailState.Loading, viewModel.state)
        runCurrent()
        assertEquals(listOf("g"), gateway.requestedCourses)
        assertEquals(CourseDetailState.Ready(graphs), viewModel.state)
    }

    @Test
    fun `a course that does not exist is a problem that cannot be retried`() = runTest {
        val gateway = FakeCourseGateway()
        val viewModel = CourseDetailViewModel("nope", gateway, progress)
        runCurrent()

        assertEquals(CourseDetailState.Problem(CourseProblem.NOT_FOUND), viewModel.state)
        viewModel.onRetry()
        runCurrent()
        assertEquals(1, gateway.requestedCourses.size)
    }

    @Test
    fun `without connection it can be retried`() = runTest {
        val gateway = gatewayAnswering(CourseOutcome.Unreachable, CourseOutcome.Found(graphs))
        val viewModel = CourseDetailViewModel("g", gateway, progress)
        runCurrent()
        assertEquals(CourseDetailState.Problem(CourseProblem.UNREACHABLE), viewModel.state)

        viewModel.onRetry()
        assertEquals(CourseDetailState.Loading, viewModel.state)
        runCurrent()
        assertEquals(CourseDetailState.Ready(graphs), viewModel.state)
    }

    @Test
    fun `a newer format and a server failure are problems too`() = runTest {
        val newer = CourseDetailViewModel("g", gatewayAnswering(CourseOutcome.UnsupportedFormat), progress)
        val broken = CourseDetailViewModel("g", gatewayAnswering(CourseOutcome.ServerFailure), progress)
        runCurrent()
        assertEquals(CourseDetailState.Problem(CourseProblem.UNSUPPORTED_FORMAT), newer.state)
        assertEquals(CourseDetailState.Problem(CourseProblem.SERVER_FAILURE), broken.state)
    }

    // Lo que ya se dominaba se ve al abrir la ficha.
    @Test
    fun `the progress saved before is shown`() = runTest {
        progress.markMastered("g", "repr")
        val viewModel = CourseDetailViewModel("g", FakeCourseGateway(listOf(graphs)), progress)
        runCurrent()

        assertEquals(CourseDetailState.Ready(graphs, mastered = setOf("repr")), viewModel.state)
    }

    // Al volver del camino tras dominar un módulo, la ficha ya lo sabe (escucha el progreso).
    @Test
    fun `mastering a module while the ficha is open updates it`() = runTest {
        val viewModel = CourseDetailViewModel("g", FakeCourseGateway(listOf(graphs)), progress)
        runCurrent()

        progress.markMastered("g", "repr")
        runCurrent()

        assertEquals(1, (viewModel.state as CourseDetailState.Ready).masteredCount)
    }

    @Test
    fun `retrying when nothing failed asks nothing`() = runTest {
        val gateway = FakeCourseGateway(listOf(graphs))
        val viewModel = CourseDetailViewModel("g", gateway, progress)
        runCurrent()

        viewModel.onRetry()
        runCurrent()
        assertEquals(1, gateway.requestedCourses.size)
    }
}
