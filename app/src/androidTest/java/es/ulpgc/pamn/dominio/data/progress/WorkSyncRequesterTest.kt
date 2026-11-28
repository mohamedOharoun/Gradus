package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Clock
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import es.ulpgc.pamn.dominio.course.progress.ProgressSync
import es.ulpgc.pamn.dominio.course.progress.ProgressSyncStore
import es.ulpgc.pamn.dominio.course.progress.SessionProgressSync
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import es.ulpgc.pamn.dominio.course.progress.SyncResponse
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

// Pruebas instrumentadas de WorkSyncRequester con WorkManagerTestInitHelper (13b-1).
// Cada test inicializa WorkManager de pruebas con ProgressSyncWorker.factory(...) para no tocar
// AppContainer, y @After cancela los trabajos sin romper el singleton para tests posteriores.
@RunWith(AndroidJUnit4::class)
class WorkSyncRequesterTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var workManager: WorkManager
    private lateinit var testDriver: TestDriver

    @After
    fun tearDown() {
        if (::workManager.isInitialized) {
            workManager.cancelAllWork().result.get()
        }
    }

    private fun initWorkManager(sessionSync: SessionProgressSync, clock: Clock = Clock { FIXED_NOW }): WorkSyncRequester {
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(ProgressSyncWorker.factory(sessionSync))
            .setClock(clock)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        testDriver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context))
        return WorkSyncRequester(workManager)
    }

    private fun uniqueWorkInfos(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(WorkSyncRequester.WORK_NAME).get()

    private suspend fun awaitWorkInfo(id: UUID, predicate: (WorkInfo) -> Boolean): WorkInfo =
        withTimeout(5_000L) {
            workManager.getWorkInfoByIdFlow(id)
                .filterNotNull()
                .first(predicate)
        }

    @Test
    fun request_sync_enqueues_unique_work_with_connected_network_constraint() = runBlocking {
        val fixture = RequesterFixture().loggedIn()
        val requester = initWorkManager(fixture.sessionSync)

        requester.requestSync()

        val info = uniqueWorkInfos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    @Test
    fun two_requests_leave_a_single_unique_work_and_run_once() = runBlocking {
        val fixture = RequesterFixture().loggedIn()
        val requester = initWorkManager(fixture.sessionSync)

        requester.requestSync()
        val firstId = uniqueWorkInfos().single().id

        requester.requestSync()
        val infos = uniqueWorkInfos()
        assertEquals(1, infos.size)
        val second = infos.single()
        assertNotEquals(firstId, second.id)
        assertEquals(WorkInfo.State.ENQUEUED, second.state)

        testDriver.setAllConstraintsMet(second.id)
        val finished = awaitWorkInfo(second.id) { it.state == WorkInfo.State.SUCCEEDED }

        assertEquals(WorkInfo.State.SUCCEEDED, finished.state)
        assertEquals(1, fixture.gateway.calls.size)
    }

    @Test
    fun without_network_work_does_not_run_and_stays_enqueued() = runBlocking {
        val fixture = RequesterFixture().loggedIn()
        val requester = initWorkManager(fixture.sessionSync)

        requester.requestSync()

        val info = uniqueWorkInfos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertTrue(fixture.gateway.calls.isEmpty())
    }

    @Test
    fun with_network_met_worker_runs_sync_and_succeeds() = runBlocking {
        val fixture = RequesterFixture().loggedIn()
        fixture.store.enroll(COURSE)
        fixture.store.markMastered(COURSE, "bfs")
        val requester = initWorkManager(fixture.sessionSync)

        requester.requestSync()
        val id = uniqueWorkInfos().single().id
        testDriver.setAllConstraintsMet(id)

        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, finished.state)
        assertEquals(1, fixture.gateway.calls.size)
        assertTrue(fixture.store.pending(ANA_ID).isEmpty)
    }

    @Test
    fun unreachable_retries_with_exponential_backoff() = runBlocking {
        val fixture = RequesterFixture { _, _ -> SyncOutcome.Unreachable }.loggedIn()
        val requester = initWorkManager(fixture.sessionSync, clock = Clock { FIXED_NOW })

        requester.requestSync()
        val id = uniqueWorkInfos().single().id

        testDriver.setAllConstraintsMet(id)
        val afterFirstRetry = awaitWorkInfo(id) { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount == 1 }
        val delay1 = afterFirstRetry.nextScheduleTimeMillis - FIXED_NOW

        testDriver.setInitialDelayMet(id)
        testDriver.setAllConstraintsMet(id)
        val afterSecondRetry = awaitWorkInfo(id) { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount == 2 }
        val delay2 = afterSecondRetry.nextScheduleTimeMillis - FIXED_NOW

        testDriver.setInitialDelayMet(id)
        testDriver.setAllConstraintsMet(id)
        val afterThirdRetry = awaitWorkInfo(id) { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount == 3 }
        val delay3 = afterThirdRetry.nextScheduleTimeMillis - FIXED_NOW

        val base = WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS
        assertEquals(base, delay1)
        assertEquals(base * 2, delay2)
        assertEquals(base * 4, delay3)
    }

    @Test
    fun server_failure_retries_until_fifth_attempt_and_then_fails() = runBlocking {
        val fixture = RequesterFixture { _, _ -> SyncOutcome.ServerFailure }.loggedIn()
        val requester = initWorkManager(fixture.sessionSync)

        requester.requestSync()
        val id = uniqueWorkInfos().single().id

        for (expectedAttempt in 1..4) {
            if (expectedAttempt > 1) testDriver.setInitialDelayMet(id)
            testDriver.setAllConstraintsMet(id)
            awaitWorkInfo(id) { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount == expectedAttempt }
        }

        testDriver.setInitialDelayMet(id)
        testDriver.setAllConstraintsMet(id)
        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.FAILED }

        assertEquals(WorkInfo.State.FAILED, finished.state)
        assertEquals(5, fixture.gateway.calls.size)
    }

    @Test
    fun session_expired_fails_without_retrying_and_clears_session() = runBlocking {
        val fixture = RequesterFixture { _, _ -> SyncOutcome.SessionExpired }.loggedIn()
        val requester = initWorkManager(fixture.sessionSync)

        requester.requestSync()
        val id = uniqueWorkInfos().single().id
        testDriver.setAllConstraintsMet(id)

        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.FAILED }
        assertEquals(WorkInfo.State.FAILED, finished.state)
        assertEquals(1, fixture.gateway.calls.size)
        assertNull(fixture.sessions.current())
    }

    @Test
    fun store_exception_marks_work_as_failed_without_retrying() = runBlocking {
        val sessions = InMemorySessionStore().apply { save(ANA_SESSION) }
        val gateway = FakeProgressGateway(answer = { _, _ -> emptySynced() })
        val sessionSync = SessionProgressSync(sessions, ProgressSync(BrokenSyncStore, gateway))
        val requester = initWorkManager(sessionSync)

        requester.requestSync()
        val id = uniqueWorkInfos().single().id
        testDriver.setAllConstraintsMet(id)

        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.FAILED }
        assertEquals(WorkInfo.State.FAILED, finished.state)
        assertTrue(gateway.calls.isEmpty())
    }
}

private const val FIXED_NOW = 1_700_000_000_000L
private const val ANA_ID = "ana"
private const val COURSE = "graphs"

private val ANA_SESSION = Session(
    token = "token-ana",
    expiresAt = "2099-01-01T00:00:00Z",
    account = Account(id = ANA_ID, name = "Ana", email = "ana@example.com", createdAt = "2026-01-01T00:00:00Z"),
)

private fun emptySynced() = SyncOutcome.Synced(SyncResponse(ProgressSnapshot(emptyList()), emptyList()))

private class RequesterFixture(
    answer: suspend (Session, ProgressSnapshot) -> SyncOutcome = { _, _ -> emptySynced() },
) {
    val sessions = InMemorySessionStore()
    val store = InMemoryProgressStore(sessions)
    val gateway = FakeProgressGateway(answer)
    val sessionSync = SessionProgressSync(sessions, ProgressSync(store, gateway))

    suspend fun loggedIn(): RequesterFixture = apply { sessions.save(ANA_SESSION) }
}

private object BrokenSyncStore : ProgressSyncStore {
    override suspend fun pending(userId: String): ProgressSnapshot = error("store broke")
    override suspend fun settle(userId: String, sent: ProgressSnapshot, remote: ProgressSnapshot) = error("Not used")
}
