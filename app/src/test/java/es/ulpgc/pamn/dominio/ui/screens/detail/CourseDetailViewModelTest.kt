package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.UnenrollFromCourse
import es.ulpgc.pamn.dominio.data.course.CourseCopyRequester
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem
import es.ulpgc.pamn.dominio.ui.screens.course.SilentEnrollmentStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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

// Carga, errores, estado derivado, progreso en vivo e inscripción en CourseDetailViewModel (C1).
// Los tests del diálogo de baja y concurrencia viven en CourseDetailUnenrollViewModelTest.
@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailViewModelTest {

    private var syncRequests = 0
    private val progressSync = ProgressSyncRequester { syncRequests++ }
    private val requestedCopies = mutableListOf<String>()
    private val courseCopy = CourseCopyRequester { requestedCopies += it }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Estado derivado (Ready) ---

    @Test
    fun `a course with no modules has zero progress without dividing by zero`() {
        val empty = Course("e", "Vacío", emptyList())
        val ready = CourseDetailState.Ready(empty)
        assertEquals(0, ready.totalModules)
        assertEquals(0f, ready.progressFraction, 0f)
        assertEquals(0, ready.progressPercent)
    }

    @Test
    fun `progress percent rounds to the nearest integer`() {
        val fourteen = Course("c14", "Grafos", (1..14).map { Module("m$it", "Módulo $it") })
        val sixMastered = CourseDetailState.Ready(fourteen, mastered = (1..6).map { "m$it" }.toSet())
        assertEquals(14, sixMastered.totalModules)
        assertEquals(6f / 14f, sixMastered.progressFraction, 0.0001f)
        assertEquals(43, sixMastered.progressPercent)

        val allMastered = CourseDetailState.Ready(detailTestCourse, mastered = setOf("repr", "bfs"))
        assertEquals(100, allMastered.progressPercent)
    }

    // --- Carga inicial, problemas y reintento ---

    @Test
    fun `it starts downloading the course of the route`() = runTest {
        val gateway = FakeCourseGateway(listOf(detailTestCourse))
        val viewModel = newViewModel(gateway = gateway)

        assertEquals(CourseDetailState.Loading, viewModel.state)
        runCurrent()
        assertEquals(listOf("g"), gateway.requestedCourses)
        assertEquals(CourseDetailState.Ready(detailTestCourse, enrolled = false), viewModel.state)
    }

    @Test
    fun `a course that does not exist is a problem that cannot be retried`() = runTest {
        val gateway = FakeCourseGateway()
        val viewModel = newViewModel(courseId = "nope", gateway = gateway)
        runCurrent()

        assertEquals(CourseDetailState.Problem(CourseProblem.NOT_FOUND), viewModel.state)
        viewModel.onRetry()
        runCurrent()
        assertEquals(1, gateway.requestedCourses.size)
    }

    @Test
    fun `without connection it can be retried`() = runTest {
        val gateway = gatewayAnswering(CourseOutcome.Unreachable, CourseOutcome.Found(detailTestCourse))
        val viewModel = newViewModel(gateway = gateway)
        runCurrent()
        assertEquals(CourseDetailState.Problem(CourseProblem.UNREACHABLE), viewModel.state)

        viewModel.onRetry()
        assertEquals(CourseDetailState.Loading, viewModel.state)
        runCurrent()
        assertEquals(CourseDetailState.Ready(detailTestCourse, enrolled = false), viewModel.state)
    }

    @Test
    fun `a newer format and a server failure are problems too`() = runTest {
        val newer = newViewModel(gateway = gatewayAnswering(CourseOutcome.UnsupportedFormat))
        val broken = newViewModel(gateway = gatewayAnswering(CourseOutcome.ServerFailure))
        runCurrent()
        assertEquals(CourseDetailState.Problem(CourseProblem.UNSUPPORTED_FORMAT), newer.state)
        assertEquals(CourseDetailState.Problem(CourseProblem.SERVER_FAILURE), broken.state)
    }

    @Test
    fun `retrying when nothing failed asks nothing`() = runTest {
        val gateway = FakeCourseGateway(listOf(detailTestCourse))
        val viewModel = newViewModel(gateway = gateway)
        runCurrent()

        viewModel.onRetry()
        runCurrent()
        assertEquals(1, gateway.requestedCourses.size)
    }

    // --- Combinación sin parpadeo ---

    @Test
    fun `it stays loading until both enrolled and mastered emit`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions)

        val waitingEnrollment = newViewModel(enrollment = SilentEnrollmentStore(), progress = store)
        val waitingProgress = newViewModel(enrollment = store, progress = SilentProgressStore())
        runCurrent()

        assertEquals(CourseDetailState.Loading, waitingEnrollment.state)
        assertEquals(CourseDetailState.Loading, waitingProgress.state)
    }

    // --- No inscrito vs inscrito y progreso en vivo ---

    @Test
    fun `an enrolled course shows prior progress and live updates`() = runTest {
        val store = InMemoryProgressStore(detailSessionStore())
        store.enroll("g")
        store.markMastered("g", "repr")
        val viewModel = newViewModel(enrollment = store, progress = store)
        runCurrent()

        assertEquals(
            CourseDetailState.Ready(detailTestCourse, mastered = setOf("repr"), enrolled = true),
            viewModel.state,
        )

        store.markMastered("g", "bfs")
        runCurrent()
        val ready = viewModel.state as CourseDetailState.Ready
        assertEquals(2, ready.masteredCount)
        assertEquals(100, ready.progressPercent)
    }

    // --- Inscripción (onEnroll) ---

    @Test
    fun `enrolling sets enrolling immediately, syncs and copies after enroll, and turns enrolling off`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val real = InMemoryProgressStore(detailSessionStore())
        val counting = CountingEnrollmentStore(real, gate)
        val viewModel = newViewModel(enrollment = counting, progress = real)
        runCurrent()

        viewModel.onEnroll()
        val immediate = viewModel.state as CourseDetailState.Ready
        assertTrue(immediate.enrolling)
        assertFalse(immediate.enrolled)

        runCurrent()
        assertEquals(1, counting.calls)
        assertEquals(0, syncRequests)
        assertEquals(emptyList<String>(), requestedCopies)

        gate.complete(Unit)
        runCurrent()
        val after = viewModel.state as CourseDetailState.Ready
        assertTrue(after.enrolled)
        assertFalse(after.enrolling)
        assertTrue(real.enrolled("g").first())
        assertEquals(1, syncRequests)
        assertEquals(listOf("g"), requestedCopies)
    }

    @Test
    fun `two enroll taps in a row enroll, sync and request copy only once`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val real = InMemoryProgressStore(detailSessionStore())
        val counting = CountingEnrollmentStore(real, gate)
        val viewModel = newViewModel(enrollment = counting, progress = real)
        runCurrent()

        viewModel.onEnroll()
        viewModel.onEnroll()
        gate.complete(Unit)
        runCurrent()

        assertEquals(1, counting.calls)
        assertEquals(1, syncRequests)
        assertEquals(listOf("g"), requestedCopies)
    }

    @Test
    fun `enrolling is ignored when already enrolled, loading or problem`() = runTest {
        val enrolledStore = InMemoryProgressStore(detailSessionStore()).also { it.enroll("g") }
        val countingEnrolled = CountingEnrollmentStore(enrolledStore)
        val readyEnrolled = newViewModel(enrollment = countingEnrolled, progress = enrolledStore)
        runCurrent()
        readyEnrolled.onEnroll()

        val countingLoading = CountingEnrollmentStore(InMemoryProgressStore(detailSessionStore()))
        val loadingVm = newViewModel(enrollment = countingLoading)
        loadingVm.onEnroll()

        val countingProblem = CountingEnrollmentStore(InMemoryProgressStore(detailSessionStore()))
        val problemVm = newViewModel(gateway = FakeCourseGateway(), enrollment = countingProblem)
        runCurrent()
        problemVm.onEnroll()
        runCurrent()

        assertEquals(0, countingEnrolled.calls)
        assertEquals(0, countingLoading.calls)
        assertEquals(0, countingProblem.calls)
        assertEquals(0, syncRequests)
        assertEquals(emptyList<String>(), requestedCopies)
    }

    @Test
    fun `enrolling without a session lets IllegalStateException propagate without syncing or copying`() {
        assertThrows(IllegalStateException::class.java) {
            runTest {
                val noSessionStore = InMemoryProgressStore(InMemorySessionStore())
                val viewModel = newViewModel(enrollment = noSessionStore, progress = noSessionStore)
                runCurrent()
                viewModel.onEnroll()
                runCurrent()
            }
        }
        assertEquals(0, syncRequests)
        assertEquals(emptyList<String>(), requestedCopies)
    }

    @Test
    fun `re-enrolling after unenrolling starts clean`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions)
        store.enroll("g")
        store.markMastered("g", "repr")
        val unenroll = UnenrollFromCourse(sessions, FakeProgressGateway(), store)
        val viewModel = newViewModel(enrollment = store, progress = store, unenroll = unenroll)
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(CourseDetailState.Ready(detailTestCourse, enrolled = false), viewModel.state)

        viewModel.onEnroll()
        runCurrent()
        assertEquals(CourseDetailState.Ready(detailTestCourse, enrolled = true), viewModel.state)
        assertEquals(1, syncRequests)
        assertEquals(listOf("g"), requestedCopies)
    }

    // --- Factoría ---

    @Test
    fun `the factory wires all dependencies`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions)
        val unenroll = UnenrollFromCourse(sessions, FakeProgressGateway(), store)
        val viewModel = CourseDetailViewModel.factory(
            courseId = "g",
            gateway = FakeCourseGateway(listOf(detailTestCourse)),
            enrollment = store,
            progress = store,
            progressSync = progressSync,
            courseCopy = courseCopy,
            unenroll = unenroll,
        ).create(CourseDetailViewModel::class.java, MutableCreationExtras())
        runCurrent()

        viewModel.onEnroll()
        runCurrent()
        assertEquals(CourseDetailState.Ready(detailTestCourse, enrolled = true), viewModel.state)
        assertEquals(1, syncRequests)
        assertEquals(listOf("g"), requestedCopies)
    }

    // --- Ayudantes ---

    private suspend fun newViewModel(
        courseId: String = "g",
        gateway: CourseGateway = FakeCourseGateway(listOf(detailTestCourse)),
        sessions: InMemorySessionStore? = null,
        store: InMemoryProgressStore? = null,
        enrollment: EnrollmentStore? = null,
        progress: ProgressStore? = null,
        unenroll: UnenrollFromCourse? = null,
    ): CourseDetailViewModel {
        val resolvedSessions = sessions ?: detailSessionStore()
        val resolvedStore = store ?: InMemoryProgressStore(resolvedSessions)
        val resolvedEnrollment = enrollment ?: resolvedStore
        val resolvedProgress = progress ?: resolvedStore
        return CourseDetailViewModel(
            courseId = courseId,
            gateway = gateway,
            enrollment = resolvedEnrollment,
            progress = resolvedProgress,
            progressSync = progressSync,
            courseCopy = courseCopy,
            unenroll = unenroll ?: UnenrollFromCourse(resolvedSessions, FakeProgressGateway(), resolvedEnrollment),
        )
    }

    private fun gatewayAnswering(vararg outcomes: CourseOutcome): FakeCourseGateway {
        var asked = 0
        return FakeCourseGateway(courseAnswer = { outcomes[minOf(asked++, outcomes.lastIndex)] })
    }
}

internal val detailTestCourse = Course(
    id = "g",
    title = "Grafos",
    modules = listOf(
        Module("repr", "Representar"),
        Module("bfs", "BFS", requires = listOf("repr")),
    ),
)

internal suspend fun detailSessionStore(): InMemorySessionStore = InMemorySessionStore().apply {
    save(
        Session(
            token = "token-de-prueba",
            expiresAt = "2099-01-01T00:00:00Z",
            account = Account(id = "ana", name = "Ana", email = "ana@example.com", createdAt = "2026-01-01T00:00:00Z"),
        ),
    )
}

internal class CountingEnrollmentStore(
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

    override suspend fun unenroll(userId: String, courseId: String) =
        delegate.unenroll(userId, courseId)
}

internal class SilentProgressStore : ProgressStore {
    override fun mastered(courseId: String): Flow<Set<String>> = MutableSharedFlow()
    override suspend fun markMastered(courseId: String, moduleId: String) = error("Not used")
}
