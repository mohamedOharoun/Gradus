package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import es.ulpgc.pamn.dominio.course.progress.ProgressSync
import es.ulpgc.pamn.dominio.course.progress.ProgressSyncStore
import es.ulpgc.pamn.dominio.course.progress.SessionProgressSync
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import es.ulpgc.pamn.dominio.course.progress.SyncResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

// Pruebas de ProgressSyncWorker (13b-1) sin tocar AppContainer: inyectan un SessionProgressSync
// controlado mediante ProgressSyncWorker.factory(...) y TestListenableWorkerBuilder.
@RunWith(AndroidJUnit4::class)
class ProgressSyncWorkerTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun buildWorker(sessionSync: SessionProgressSync, attempt: Int = 0): ProgressSyncWorker =
        TestListenableWorkerBuilder<ProgressSyncWorker>(context)
            .setWorkerFactory(ProgressSyncWorker.factory(sessionSync))
            .setRunAttemptCount(attempt)
            .build()

    @Test
    fun no_session_returns_success_and_does_not_call_gateway() = runTest {
        val fixture = WorkerFixture()
        val worker = buildWorker(fixture.sessionSync)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertTrue(fixture.gateway.calls.isEmpty())
    }

    @Test
    fun synced_returns_success_and_settles_pending_progress() = runTest {
        val fixture = WorkerFixture().loggedIn()
        fixture.store.enroll(COURSE)
        fixture.store.markMastered(COURSE, "bfs")
        val worker = buildWorker(fixture.sessionSync)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertEquals(1, fixture.gateway.calls.size)
        assertTrue(fixture.store.pending(ANA_ID).isEmpty)
    }

    @Test
    fun unreachable_returns_retry_even_on_high_attempt() = runTest {
        for (attempt in listOf(0, 4, 100)) {
            val fixture = WorkerFixture { _, _ -> SyncOutcome.Unreachable }.loggedIn()
            fixture.store.enroll(COURSE)
            val worker = buildWorker(fixture.sessionSync, attempt = attempt)

            val result = worker.doWork()

            assertEquals("attempt=$attempt", Result.retry(), result)
            assertFalse(fixture.store.pending(ANA_ID).isEmpty)
        }
    }

    @Test
    fun server_failure_returns_retry_on_attempts_0_to_3() = runTest {
        for (attempt in 0..3) {
            val fixture = WorkerFixture { _, _ -> SyncOutcome.ServerFailure }.loggedIn()
            val worker = buildWorker(fixture.sessionSync, attempt = attempt)

            val result = worker.doWork()

            assertEquals("attempt=$attempt", Result.retry(), result)
        }
    }

    @Test
    fun server_failure_returns_failure_on_attempt_4_and_above() = runTest {
        for (attempt in listOf(4, 5, 10)) {
            val fixture = WorkerFixture { _, _ -> SyncOutcome.ServerFailure }.loggedIn()
            val worker = buildWorker(fixture.sessionSync, attempt = attempt)

            val result = worker.doWork()

            assertEquals("attempt=$attempt", Result.failure(), result)
        }
    }

    @Test
    fun session_expired_clears_session_and_returns_failure() = runTest {
        val fixture = WorkerFixture { _, _ -> SyncOutcome.SessionExpired }.loggedIn()
        val worker = buildWorker(fixture.sessionSync)

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        assertNull(fixture.sessions.current())
    }

    @Test
    fun session_expired_keeps_newer_session_and_returns_failure() = runTest {
        val sessions = InMemorySessionStore()
        sessions.save(ANA_SESSION)
        val store = InMemoryProgressStore(sessions)
        val gateway = FakeProgressGateway(
            answer = { _, _ ->
                sessions.save(BETO_SESSION)
                SyncOutcome.SessionExpired
            },
        )
        val worker = buildWorker(SessionProgressSync(sessions, ProgressSync(store, gateway)))

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        assertEquals(BETO_SESSION, sessions.current())
    }

    @Test
    fun store_exception_propagates_out_of_do_work() = runTest {
        val sessions = InMemorySessionStore().apply { save(ANA_SESSION) }
        val gateway = FakeProgressGateway(answer = { _, _ -> emptySynced() })
        val worker = buildWorker(SessionProgressSync(sessions, ProgressSync(FailingSyncStore, gateway)))

        val failure = runCatching { worker.doWork() }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(FailingSyncStore.MESSAGE, failure?.message)
    }

    @Test
    fun session_read_exception_propagates_out_of_do_work() = runTest {
        val gateway = FakeProgressGateway(answer = { _, _ -> emptySynced() })
        val store = InMemoryProgressStore(InMemorySessionStore())
        val worker = buildWorker(SessionProgressSync(FailingSessionStore, ProgressSync(store, gateway)))

        val failure = runCatching { worker.doWork() }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertEquals(FailingSessionStore.MESSAGE, failure?.message)
    }

    @Test
    fun coroutine_cancellation_propagates_out_of_do_work() = runTest {
        val entered = CompletableDeferred<Unit>()
        val hang = CompletableDeferred<SyncOutcome>()
        val fixture = WorkerFixture { _, _ ->
            entered.complete(Unit)
            hang.await()
        }.loggedIn()
        fixture.store.enroll(COURSE)
        val worker = buildWorker(fixture.sessionSync)

        var caught: CancellationException? = null
        val job = launch {
            try {
                worker.doWork()
            } catch (cancelled: CancellationException) {
                caught = cancelled
                throw cancelled
            }
        }
        entered.await()
        job.cancel()
        job.join()

        assertTrue(caught is CancellationException)
        assertFalse(fixture.store.pending(ANA_ID).isEmpty)
    }
}

private const val ANA_ID = "ana"
private const val COURSE = "graphs"

private val ANA_SESSION = Session(
    token = "token-ana",
    expiresAt = "2099-01-01T00:00:00Z",
    account = Account(id = ANA_ID, name = "Ana", email = "ana@example.com", createdAt = "2026-01-01T00:00:00Z"),
)

private val BETO_SESSION = Session(
    token = "token-beto",
    expiresAt = "2099-01-01T00:00:00Z",
    account = Account(id = "beto", name = "Beto", email = "beto@example.com", createdAt = "2026-01-01T00:00:00Z"),
)

private fun emptySynced() = SyncOutcome.Synced(SyncResponse(ProgressSnapshot(emptyList()), emptyList()))

private class WorkerFixture(
    answer: suspend (Session, ProgressSnapshot) -> SyncOutcome = { _, _ -> emptySynced() },
) {
    val sessions = InMemorySessionStore()
    val store = InMemoryProgressStore(sessions)
    val gateway = FakeProgressGateway(answer)
    val sessionSync = SessionProgressSync(sessions, ProgressSync(store, gateway))

    suspend fun loggedIn(): WorkerFixture = apply { sessions.save(ANA_SESSION) }
}

private object FailingSyncStore : ProgressSyncStore {
    const val MESSAGE = "store broke"
    override suspend fun pending(userId: String): ProgressSnapshot = error(MESSAGE)
    override suspend fun settle(userId: String, sent: ProgressSnapshot, remote: ProgressSnapshot) = error("Not used")
}

private object FailingSessionStore : SessionStore {
    const val MESSAGE = "disk unavailable"
    override suspend fun current(): Session = throw IOException(MESSAGE)
    override suspend fun save(session: Session) = error("Not used")
    override suspend fun clear() = error("Not used")
}
