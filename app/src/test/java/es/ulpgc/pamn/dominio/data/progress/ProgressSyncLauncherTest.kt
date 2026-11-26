package es.ulpgc.pamn.dominio.data.progress

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import es.ulpgc.pamn.dominio.course.progress.ProgressSync
import es.ulpgc.pamn.dominio.course.progress.ProgressSyncStore
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import es.ulpgc.pamn.dominio.course.progress.SyncResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// ProgressSyncLauncher (8a): pide sincronizar sin bloquear, en un ámbito que no es el de quien lo
// pide. El launcher recibe el backgroundScope de la prueba (hace de ámbito de la app) salvo en el test
// del fallo del almacén, que usa el de runTest para que la excepción haga fallar la prueba.
// Sin delay: la respuesta del gateway la controla un CompletableDeferred.
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressSyncLauncherTest {

    // --- Sin sesión ---

    @Test
    fun `without a session the server is not called and there is no outcome`() = runTest {
        val fixture = Fixture()
        val launcher = fixture.launcher(backgroundScope)

        launcher.requestSync()
        runCurrent()

        assertTrue(fixture.gateway.calls.isEmpty())
        assertNull(launcher.lastOutcome.value)
    }

    @Test
    fun `without a session the previous outcome is kept`() = runTest {
        val fixture = Fixture().loggedIn()
        val launcher = fixture.launcher(backgroundScope)
        launcher.requestSync()
        runCurrent()

        fixture.sessions.clear()
        launcher.requestSync()
        runCurrent()

        assertEquals(1, fixture.gateway.calls.size)
        assertEquals(synced(), launcher.lastOutcome.value)
    }

    // --- Con sesión ---

    @Test
    fun `with a session the server is called even with nothing pending`() = runTest {
        val fixture = Fixture().loggedIn()
        val launcher = fixture.launcher(backgroundScope)

        launcher.requestSync()
        runCurrent()

        val call = fixture.gateway.calls.single()
        assertEquals(ANA, call.userId)
        assertTrue(call.sent.isEmpty)
    }

    @Test
    fun `with a session what is pending is sent and leaves the queue after synced`() = runTest {
        val fixture = Fixture().loggedIn()
        fixture.store.enroll(GRAPHS)
        fixture.store.markMastered(GRAPHS, "bfs")
        val launcher = fixture.launcher(backgroundScope)

        launcher.requestSync()
        runCurrent()

        val sent = fixture.gateway.calls.single().sent.enrollments.single()
        assertEquals(GRAPHS, sent.enrollment.courseId)
        assertEquals(listOf("bfs"), sent.mastered.map { it.moduleId })
        assertTrue(fixture.store.pending(ANA).isEmpty)
    }

    @Test
    fun `requesting returns without waiting for the sync`() = runTest {
        val fixture = Fixture().loggedIn()
        val launcher = fixture.launcher(backgroundScope)

        launcher.requestSync()

        // Todavía no ha corrido nada: la sincronización va en otra corrutina.
        assertTrue(fixture.gateway.calls.isEmpty())
        assertNull(launcher.lastOutcome.value)
        runCurrent()
        assertEquals(1, fixture.gateway.calls.size)
    }

    // --- El desenlace ---

    @Test
    fun `the outcome is null while the answer travels and arrives with it`() = runTest {
        val reply = CompletableDeferred<SyncOutcome>()
        val fixture = Fixture { _, _ -> reply.await() }.loggedIn()
        val launcher = fixture.launcher(backgroundScope)

        launcher.requestSync()
        runCurrent()
        assertEquals(1, fixture.gateway.calls.size)
        assertNull(launcher.lastOutcome.value)

        reply.complete(synced())
        runCurrent()
        assertEquals(synced(), launcher.lastOutcome.value)
    }

    @Test
    fun `session expired and server failure reach the outcome as they are`() = runTest {
        for (expected in listOf(SyncOutcome.SessionExpired, SyncOutcome.ServerFailure)) {
            val fixture = Fixture { _, _ -> expected }.loggedIn()
            val launcher = fixture.launcher(backgroundScope)

            launcher.requestSync()
            runCurrent()

            assertEquals(expected, launcher.lastOutcome.value)
        }
    }

    @Test
    fun `unreachable keeps everything pending does not throw and does not retry`() = runTest {
        val fixture = Fixture { _, _ -> SyncOutcome.Unreachable }.loggedIn()
        fixture.store.enroll(GRAPHS)
        fixture.store.markMastered(GRAPHS, "bfs")
        val before = fixture.store.pending(ANA)
        val launcher = fixture.launcher(backgroundScope)

        launcher.requestSync()
        runCurrent()

        assertEquals(SyncOutcome.Unreachable, launcher.lastOutcome.value)
        assertEquals(before, fixture.store.pending(ANA))
        assertEquals(1, fixture.gateway.calls.size)
    }

    // --- El ámbito ---

    @Test
    fun `cancelling the scope of whoever asked does not cancel the sync`() = runTest {
        val reply = CompletableDeferred<SyncOutcome>()
        val fixture = Fixture { _, _ -> reply.await() }.loggedIn()
        fixture.store.enroll(GRAPHS)
        val launcher = fixture.launcher(backgroundScope)
        // Hace de viewModelScope: se cancela cuando se cierra la pantalla.
        val screenScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

        screenScope.launch { launcher.requestSync() }
        runCurrent()
        assertEquals(1, fixture.gateway.calls.size)
        screenScope.cancel()

        reply.complete(synced())
        runCurrent()
        assertEquals(synced(), launcher.lastOutcome.value)
        assertTrue(fixture.store.pending(ANA).isEmpty)
    }

    // --- Dos a la vez (B-11) ---

    @Test
    fun `two requests in a row lose nothing`() = runTest {
        val replies = Replies(2)
        val fixture = Fixture { _, _ -> replies.next() }.loggedIn()
        fixture.store.enroll(GRAPHS)
        fixture.store.markMastered(GRAPHS, "bfs")
        val launcher = fixture.launcher(backgroundScope)

        launcher.requestSync()
        runCurrent()
        // Mientras viaja la primera, se domina otro módulo y se pide otra vez.
        fixture.store.markMastered(GRAPHS, "dfs")
        launcher.requestSync()
        runCurrent()

        replies.complete(0, synced())
        replies.complete(1, synced())
        runCurrent()

        assertEquals(2, fixture.gateway.calls.size)
        val secondSent = fixture.gateway.calls[1].sent.enrollments.single().mastered.map { it.moduleId }
        assertEquals(setOf("bfs", "dfs"), secondSent.toSet())
        assertTrue(fixture.store.pending(ANA).isEmpty)
    }

    @Test
    fun `with two at once the outcome is the one that finishes last`() = runTest {
        val replies = Replies(2)
        val fixture = Fixture { _, _ -> replies.next() }.loggedIn()
        val launcher = fixture.launcher(backgroundScope)
        launcher.requestSync()
        runCurrent()
        launcher.requestSync()
        runCurrent()

        replies.complete(1, synced())
        runCurrent()
        assertEquals(synced(), launcher.lastOutcome.value)

        // La primera, pedida antes, termina después: su desenlace es el que queda.
        replies.complete(0, SyncOutcome.Unreachable)
        runCurrent()
        assertEquals(SyncOutcome.Unreachable, launcher.lastOutcome.value)
    }

    // --- Fallo del almacén: es real, no se captura ---

    @Test
    fun `a store failure is not caught and leaves through the scope`() {
        val failure = assertThrows(IllegalStateException::class.java) {
            runTest {
                val fixture = Fixture().loggedIn()
                val launcher = ProgressSyncLauncher(
                    ProgressSync(FailingSyncStore, fixture.gateway),
                    fixture.sessions,
                    this,
                )
                launcher.requestSync()
                runCurrent()
            }
        }
        assertEquals(FailingSyncStore.MESSAGE, failure.message)
    }
}

private const val ANA = "ana"
private const val GRAPHS = "graphs"

private fun synced() = SyncOutcome.Synced(SyncResponse(ProgressSnapshot(emptyList()), emptyList()))

// El almacén real en memoria, la sesión y el gateway falso, unidos como en ProgressModule.
private class Fixture(
    answer: suspend (Session, ProgressSnapshot) -> SyncOutcome = { _, _ -> synced() },
) {
    val sessions = InMemorySessionStore()
    val store = InMemoryProgressStore(sessions)
    val gateway = FakeProgressGateway(answer)

    fun launcher(scope: CoroutineScope) = ProgressSyncLauncher(ProgressSync(store, gateway), sessions, scope)

    suspend fun loggedIn(): Fixture = apply {
        sessions.save(
            Session(
                token = "token-de-prueba",
                expiresAt = "2099-01-01T00:00:00Z",
                account = Account(id = ANA, name = "Ana", email = "ana@example.com", createdAt = "2026-01-01T00:00:00Z"),
            ),
        )
    }
}

// Una respuesta por llamada, en orden de llegada; el test decide cuándo y con qué llega cada una.
private class Replies(count: Int) {
    private val replies = List(count) { CompletableDeferred<SyncOutcome>() }
    private var taken = 0

    suspend fun next(): SyncOutcome = replies[taken++].await()

    fun complete(call: Int, outcome: SyncOutcome) {
        replies[call].complete(outcome)
    }
}

// Un almacén que falla al leer lo pendiente (como fallaría Room).
private object FailingSyncStore : ProgressSyncStore {
    const val MESSAGE = "store broke"
    override suspend fun pending(userId: String): ProgressSnapshot = error(MESSAGE)
    override suspend fun settle(userId: String, sent: ProgressSnapshot, remote: ProgressSnapshot) = error("Not used")
}
