package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.UnenrollFromCourse
import es.ulpgc.pamn.dominio.course.progress.UnenrollOutcome
import es.ulpgc.pamn.dominio.model.FailureReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

// Ciclo del diálogo de baja en línea y concurrencia con emisiones de Room en CourseDetailViewModel (C1).
@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailUnenrollViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Abrir y cerrar el diálogo ---

    @Test
    fun `opening unenroll dialog works only when ready, enrolled and closed`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions)
        val notEnrolled = openViewModel(sessions, store)
        runCurrent()
        notEnrolled.onOpenUnenrollDialog()
        assertEquals(UnenrollDialogState.Closed, notEnrolled.ready().unenrollDialog)

        val loading = openViewModel(sessions, store)
        loading.onOpenUnenrollDialog()
        assertEquals(CourseDetailState.Loading, loading.state)

        store.enroll("g")
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> UnenrollOutcome.Unreachable })
        val enrolled = openViewModel(sessions, store, progressGateway = gateway)
        runCurrent()

        enrolled.onOpenUnenrollDialog()
        assertEquals(UnenrollDialogState.Confirming, enrolled.ready().unenrollDialog)

        // Ya abierto en Failed: volver a abrir no reinicia el estado del diálogo.
        enrolled.onConfirmUnenroll()
        runCurrent()
        assertEquals(UnenrollDialogState.Failed(FailureReason.UNREACHABLE), enrolled.ready().unenrollDialog)
        enrolled.onOpenUnenrollDialog()
        assertEquals(UnenrollDialogState.Failed(FailureReason.UNREACHABLE), enrolled.ready().unenrollDialog)
    }

    @Test
    fun `dismissing closes confirming and failed, but is ignored while sending`() = runTest {
        val gate = CompletableDeferred<UnenrollOutcome>()
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val viewModel = openViewModel(sessions, store, FakeProgressGateway(unenrollAnswer = { _, _ -> gate.await() }))
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onDismissUnenrollDialog()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        assertEquals(UnenrollDialogState.Sending, viewModel.ready().unenrollDialog)
        viewModel.onDismissUnenrollDialog()
        assertEquals(UnenrollDialogState.Sending, viewModel.ready().unenrollDialog)

        gate.complete(UnenrollOutcome.ServerFailure)
        runCurrent()
        assertEquals(UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE), viewModel.ready().unenrollDialog)

        viewModel.onDismissUnenrollDialog()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
    }

    // --- Confirmar baja y los 5 desenlaces ---

    @Test
    fun `confirming is ignored when closed and guards against double send`() = runTest {
        val gate = CompletableDeferred<UnenrollOutcome>()
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> gate.await() })
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val viewModel = openViewModel(sessions, store, gateway)
        runCurrent()

        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(0, gateway.unenrollCalls.size)

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        assertEquals(UnenrollDialogState.Sending, viewModel.ready().unenrollDialog)
        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(1, gateway.unenrollCalls.size)

        gate.complete(UnenrollOutcome.Unenrolled)
        runCurrent()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
    }

    @Test
    fun `unenrolled closes the dialog and clears enrolled and mastered via flows`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also {
            it.enroll("g")
            it.markMastered("g", "repr")
        }
        val viewModel = openViewModel(sessions, store)
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()

        val ready = viewModel.ready()
        assertEquals(UnenrollDialogState.Closed, ready.unenrollDialog)
        assertFalse(ready.enrolled)
        assertEquals(emptySet<String>(), ready.mastered)
    }

    @Test
    fun `unreachable shows failure and allows retrying until unenrolled`() = runTest {
        var calls = 0
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ ->
            if (calls++ == 0) UnenrollOutcome.Unreachable else UnenrollOutcome.Unenrolled
        })
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val viewModel = openViewModel(sessions, store, gateway)
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(UnenrollDialogState.Failed(FailureReason.UNREACHABLE), viewModel.ready().unenrollDialog)
        assertTrue(viewModel.ready().enrolled)

        viewModel.onConfirmUnenroll()
        assertEquals(UnenrollDialogState.Sending, viewModel.ready().unenrollDialog)
        runCurrent()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
        assertFalse(viewModel.ready().enrolled)
    }

    @Test
    fun `server failure shows failure and allows retrying`() = runTest {
        var calls = 0
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ ->
            if (calls++ == 0) UnenrollOutcome.ServerFailure else UnenrollOutcome.Unenrolled
        })
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val viewModel = openViewModel(sessions, store, gateway)
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE), viewModel.ready().unenrollDialog)

        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
        assertFalse(viewModel.ready().enrolled)
    }

    @Test
    fun `session expired closes the dialog and leaves session cleared by use case`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> UnenrollOutcome.SessionExpired })
        val viewModel = openViewModel(sessions, store, gateway)
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()

        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
        assertNull(sessions.current())
    }

    @Test
    fun `no session closes the dialog without calling the gateway`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val gateway = FakeProgressGateway()
        val viewModel = openViewModel(sessions, store, gateway)
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        sessions.clear()
        viewModel.onConfirmUnenroll()
        runCurrent()

        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
        assertEquals(0, gateway.unenrollCalls.size)
    }

    // --- Concurrencia con emisiones de Room ---

    @Test
    fun `mastered updates while dialog is confirming, sending or failed do not close it`() = runTest {
        val gate = CompletableDeferred<UnenrollOutcome>()
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> gate.await() })
        val viewModel = openViewModel(sessions, store, gateway)
        runCurrent()

        // Confirming
        viewModel.onOpenUnenrollDialog()
        store.markMastered("g", "repr")
        runCurrent()
        assertEquals(setOf("repr"), viewModel.ready().mastered)
        assertEquals(UnenrollDialogState.Confirming, viewModel.ready().unenrollDialog)

        // Sending
        viewModel.onConfirmUnenroll()
        store.markMastered("g", "bfs")
        runCurrent()
        assertEquals(setOf("repr", "bfs"), viewModel.ready().mastered)
        assertEquals(UnenrollDialogState.Sending, viewModel.ready().unenrollDialog)

        // Failed
        gate.complete(UnenrollOutcome.Unreachable)
        runCurrent()
        assertEquals(UnenrollDialogState.Failed(FailureReason.UNREACHABLE), viewModel.ready().unenrollDialog)
    }

    @Test
    fun `mastered update while dialog is failed keeps the failed dialog open`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> UnenrollOutcome.ServerFailure })
        val viewModel = openViewModel(sessions, store, gateway)
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE), viewModel.ready().unenrollDialog)

        store.markMastered("g", "repr")
        runCurrent()
        assertEquals(setOf("repr"), viewModel.ready().mastered)
        assertEquals(UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE), viewModel.ready().unenrollDialog)
    }

    @Test
    fun `external unenroll closes confirming and failed dialogs`() = runTest {
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val viewModel = openViewModel(sessions, store, FakeProgressGateway(unenrollAnswer = { _, _ ->
            UnenrollOutcome.Unreachable
        }))
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        store.unenroll("ana", "g")
        runCurrent()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
        assertFalse(viewModel.ready().enrolled)

        // Volvemos a inscribir, provocamos Failed y damos de baja desde fuera.
        store.enroll("g")
        runCurrent()
        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()
        assertEquals(UnenrollDialogState.Failed(FailureReason.UNREACHABLE), viewModel.ready().unenrollDialog)

        store.unenroll("ana", "g")
        runCurrent()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
        assertFalse(viewModel.ready().enrolled)
    }

    @Test
    fun `external unenroll during sending closes the dialog even if the request fails`() = runTest {
        val gate = CompletableDeferred<UnenrollOutcome>()
        val sessions = detailSessionStore()
        val store = InMemoryProgressStore(sessions).also { it.enroll("g") }
        val viewModel = openViewModel(sessions, store, FakeProgressGateway(unenrollAnswer = { _, _ -> gate.await() }))
        runCurrent()

        viewModel.onOpenUnenrollDialog()
        viewModel.onConfirmUnenroll()
        runCurrent()

        store.unenroll("ana", "g")
        runCurrent()
        assertEquals(UnenrollDialogState.Sending, viewModel.ready().unenrollDialog)
        assertFalse(viewModel.ready().enrolled)

        gate.complete(UnenrollOutcome.Unreachable)
        runCurrent()
        assertEquals(UnenrollDialogState.Closed, viewModel.ready().unenrollDialog)
        assertFalse(viewModel.ready().enrolled)
    }

    // --- Ayudantes ---

    private fun openViewModel(
        sessions: InMemorySessionStore,
        store: InMemoryProgressStore,
        progressGateway: FakeProgressGateway = FakeProgressGateway(),
    ): CourseDetailViewModel = CourseDetailViewModel(
        courseId = "g",
        gateway = FakeCourseGateway(listOf(detailTestCourse)),
        enrollment = store,
        progress = store,
        progressSync = {},
        courseCopy = {},
        unenroll = UnenrollFromCourse(sessions, progressGateway, store),
    )

    private fun CourseDetailViewModel.ready(): CourseDetailState.Ready = state as CourseDetailState.Ready
}
