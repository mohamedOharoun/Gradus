package es.ulpgc.pamn.dominio.data.publication

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
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.FakeCoursePublicationGateway
import es.ulpgc.pamn.dominio.course.publication.InMemoryCourseDraftContentStore
import es.ulpgc.pamn.dominio.course.publication.InMemoryCourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.course.publication.ProcessCourseDraft
import es.ulpgc.pamn.dominio.course.publication.ValidationOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Pruebas instrumentadas de WorkUploadRequester con WorkManagerTestInitHelper (paso 3b).
// Cada test inicializa WorkManager de pruebas con CourseUploadWorker.factory(...) para no tocar
// AppContainer, y @After cancela los trabajos sin romper el singleton para tests posteriores.
@RunWith(AndroidJUnit4::class)
class WorkUploadRequesterTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var workManager: WorkManager
    private lateinit var testDriver: TestDriver

    @After
    fun tearDown() {
        if (::workManager.isInitialized) {
            workManager.cancelAllWork().result.get()
        }
    }

    private fun initWorkManager(
        processor: ProcessCourseDraft,
        clock: Clock = Clock { FIXED_NOW },
    ): WorkUploadRequester {
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(CourseUploadWorker.factory(processor))
            .setClock(clock)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        testDriver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context))
        return WorkUploadRequester(workManager)
    }

    private fun uniqueWorkInfos(draftId: String): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(WorkUploadRequester.workName(draftId)).get()

    private suspend fun awaitWorkInfo(id: UUID, predicate: (WorkInfo) -> Boolean): WorkInfo =
        withTimeout(5.seconds) {
            workManager.getWorkInfoByIdFlow(id)
                .filterNotNull()
                .first(predicate)
        }

    @Test
    fun enqueue_blank_draft_id_throws_illegal_argument() = runBlocking {
        val fixture = UploadRequesterFixture().loggedIn()
        val requester = initWorkManager(fixture.processor)

        assertThrows(IllegalArgumentException::class.java) {
            requester.enqueue("   ")
        }
        Unit
    }

    @Test
    fun enqueue_creates_unique_work_with_connected_network_constraint() = runBlocking {
        val fixture = UploadRequesterFixture().loggedIn().withPendingDraft(DRAFT_1)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)

        val info = uniqueWorkInfos(DRAFT_1).single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    @Test
    fun two_enqueues_for_same_draft_leave_single_unique_work_and_run_once() = runBlocking {
        val fixture = UploadRequesterFixture().loggedIn().withPendingDraft(DRAFT_1)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)
        val firstId = uniqueWorkInfos(DRAFT_1).single().id

        requester.enqueue(DRAFT_1)
        val infos = uniqueWorkInfos(DRAFT_1)
        assertEquals(1, infos.size)
        val second = infos.single()
        assertNotEquals(firstId, second.id)
        assertEquals(WorkInfo.State.ENQUEUED, second.state)

        testDriver.setAllConstraintsMet(second.id)
        val finished = awaitWorkInfo(second.id) { it.state == WorkInfo.State.SUCCEEDED }

        assertEquals(WorkInfo.State.SUCCEEDED, finished.state)
        assertEquals(1, fixture.gateway.validateNewCalls.size)
        assertEquals(DraftStatus.Accepted(CourseReport.EMPTY), fixture.drafts.find(DRAFT_1)?.status)
    }

    @Test
    fun enqueues_for_different_drafts_run_independently() = runBlocking {
        val json1 = """{"title":"Curso 1"}""".encodeToByteArray()
        val json2 = """{"title":"Curso 2"}""".encodeToByteArray()
        val fixture = UploadRequesterFixture()
            .loggedIn()
            .withPendingDraft(DRAFT_1, json = json1)
            .withPendingDraft(DRAFT_2, json = json2)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)
        requester.enqueue(DRAFT_2)

        val id1 = uniqueWorkInfos(DRAFT_1).single().id
        val id2 = uniqueWorkInfos(DRAFT_2).single().id
        assertNotEquals(id1, id2)

        testDriver.setAllConstraintsMet(id1)
        val finished1 = awaitWorkInfo(id1) { it.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, finished1.state)
        assertEquals(DraftStatus.Accepted(CourseReport.EMPTY), fixture.drafts.find(DRAFT_1)?.status)
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING), fixture.drafts.find(DRAFT_2)?.status)

        testDriver.setAllConstraintsMet(id2)
        val finished2 = awaitWorkInfo(id2) { it.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, finished2.state)
        assertEquals(DraftStatus.Accepted(CourseReport.EMPTY), fixture.drafts.find(DRAFT_2)?.status)
        assertEquals(
            listOf(json1.decodeToString(), json2.decodeToString()),
            fixture.gateway.validateNewCalls.map { it.second.decodeToString() },
        )
    }

    @Test
    fun without_network_work_does_not_run_and_stays_enqueued() = runBlocking {
        val fixture = UploadRequesterFixture().loggedIn().withPendingDraft(DRAFT_1)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)

        val info = uniqueWorkInfos(DRAFT_1).single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
    }

    @Test
    fun with_network_met_worker_completes_and_succeeds() = runBlocking {
        val fixture = UploadRequesterFixture()
            .loggedIn()
            .withPendingDraft(DRAFT_1)
            .withPendingDraft(DRAFT_2)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)
        val id = uniqueWorkInfos(DRAFT_1).single().id
        testDriver.setAllConstraintsMet(id)

        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, finished.state)
        assertEquals(1, fixture.gateway.validateNewCalls.size)
        assertEquals(DraftStatus.Accepted(CourseReport.EMPTY), fixture.drafts.find(DRAFT_1)?.status)
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING), fixture.drafts.find(DRAFT_2)?.status)
    }

    @Test
    fun unreachable_retries_with_exponential_backoff() = runBlocking {
        val fixture = UploadRequesterFixture(
            validateNewAnswer = { _, _ -> ValidationOutcome.Unreachable },
        ).loggedIn().withPendingDraft(DRAFT_1)
        val requester = initWorkManager(fixture.processor, clock = { FIXED_NOW })

        requester.enqueue(DRAFT_1)
        val id = uniqueWorkInfos(DRAFT_1).single().id

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
        val fixture = UploadRequesterFixture(
            validateNewAnswer = { _, _ -> ValidationOutcome.ServerFailure },
        ).loggedIn().withPendingDraft(DRAFT_1)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)
        val id = uniqueWorkInfos(DRAFT_1).single().id

        for (expectedAttempt in 1..4) {
            if (expectedAttempt > 1) testDriver.setInitialDelayMet(id)
            testDriver.setAllConstraintsMet(id)
            awaitWorkInfo(id) { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount == expectedAttempt }
        }

        testDriver.setInitialDelayMet(id)
        testDriver.setAllConstraintsMet(id)
        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.FAILED }

        assertEquals(WorkInfo.State.FAILED, finished.state)
        assertEquals(5, fixture.gateway.validateNewCalls.size)
        assertEquals(
            DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SERVER_FAILURE),
            fixture.drafts.find(DRAFT_1)?.status,
        )
    }

    @Test
    fun session_expired_fails_without_retrying_and_clears_session() = runBlocking {
        val fixture = UploadRequesterFixture(
            validateNewAnswer = { _, _ -> ValidationOutcome.SessionExpired },
        ).loggedIn().withPendingDraft(DRAFT_1)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)
        val id = uniqueWorkInfos(DRAFT_1).single().id
        testDriver.setAllConstraintsMet(id)

        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.FAILED }
        assertEquals(WorkInfo.State.FAILED, finished.state)
        assertEquals(1, fixture.gateway.validateNewCalls.size)
        assertNull(fixture.sessions.current())
        assertEquals(
            DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SESSION_EXPIRED),
            fixture.drafts.find(DRAFT_1)?.status,
        )
    }

    @Test
    fun cancel_cancels_unique_work_without_affecting_other_drafts() = runBlocking {
        val fixture = UploadRequesterFixture()
            .loggedIn()
            .withPendingDraft(DRAFT_1)
            .withPendingDraft(DRAFT_2)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)
        requester.enqueue(DRAFT_2)
        val id1 = uniqueWorkInfos(DRAFT_1).single().id
        val id2 = uniqueWorkInfos(DRAFT_2).single().id

        requester.cancel(DRAFT_1)

        val cancelled = awaitWorkInfo(id1) { it.state == WorkInfo.State.CANCELLED }
        assertEquals(WorkInfo.State.CANCELLED, cancelled.state)
        assertEquals(WorkInfo.State.ENQUEUED, uniqueWorkInfos(DRAFT_2).single().state)

        testDriver.setAllConstraintsMet(id2)
        val finished2 = awaitWorkInfo(id2) { it.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, finished2.state)
        assertEquals(1, fixture.gateway.validateNewCalls.size)
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING), fixture.drafts.find(DRAFT_1)?.status)
        assertEquals(DraftStatus.Accepted(CourseReport.EMPTY), fixture.drafts.find(DRAFT_2)?.status)
    }

    @Test
    fun cancel_blank_draft_id_is_ignored_without_error() = runBlocking {
        val fixture = UploadRequesterFixture().loggedIn().withPendingDraft(DRAFT_1)
        val requester = initWorkManager(fixture.processor)

        requester.enqueue(DRAFT_1)
        requester.cancel("   ")

        assertEquals(WorkInfo.State.ENQUEUED, uniqueWorkInfos(DRAFT_1).single().state)
    }

    @Test
    fun store_exception_marks_work_as_failed_without_retrying() = runBlocking {
        val sessions = InMemorySessionStore().apply { save(ANA_SESSION) }
        val gateway = FakeCoursePublicationGateway()
        val processor = ProcessCourseDraft(
            drafts = BrokenDraftStore,
            contents = InMemoryCourseDraftContentStore(),
            sessions = sessions,
            gateway = gateway,
            cacheInvalidator = {},
        )
        val requester = initWorkManager(processor)

        requester.enqueue(DRAFT_1)
        val id = uniqueWorkInfos(DRAFT_1).single().id
        testDriver.setAllConstraintsMet(id)

        val finished = awaitWorkInfo(id) { it.state == WorkInfo.State.FAILED }
        assertEquals(WorkInfo.State.FAILED, finished.state)
        assertTrue(gateway.validateNewCalls.isEmpty())
    }
}

private const val FIXED_NOW = 1_700_000_000_000L
private const val ANA_ID = "ana"
private const val DRAFT_1 = "draft-1"
private const val DRAFT_2 = "draft-2"
private val DEFAULT_JSON = """{"title":"Grafos"}""".encodeToByteArray()

private val ANA_SESSION = Session(
    token = "token-ana",
    expiresAt = "2099-01-01T00:00:00Z",
    account = Account(id = ANA_ID, name = "Ana", email = "ana@example.com", createdAt = "2026-01-01T00:00:00Z"),
)

private class UploadRequesterFixture(
    validateNewAnswer: suspend (Session, ByteArray) -> ValidationOutcome = { _, _ ->
        ValidationOutcome.Validated(CourseReport.EMPTY)
    },
) {
    val sessions = InMemorySessionStore()
    val drafts = InMemoryCourseDraftStore(sessions)
    val contents = InMemoryCourseDraftContentStore()
    val gateway = FakeCoursePublicationGateway(validateNewAnswer = validateNewAnswer)
    val processor = ProcessCourseDraft(
        drafts = drafts,
        contents = contents,
        sessions = sessions,
        gateway = gateway,
        cacheInvalidator = {},
    )

    suspend fun loggedIn(): UploadRequesterFixture = apply { sessions.save(ANA_SESSION) }

    suspend fun withPendingDraft(
        draftId: String,
        action: PendingAction = PendingAction.VALIDATING,
        json: ByteArray = DEFAULT_JSON,
    ): UploadRequesterFixture = apply {
        val filePath = "course_drafts/$draftId.json"
        drafts.upsert(
            CourseDraft(
                id = draftId,
                userId = ANA_ID,
                targetCourseId = null,
                filePath = filePath,
                title = "Curso $draftId",
                moduleCount = 2,
                status = DraftStatus.Pending(action),
                updatedAt = Instant.fromEpochMilliseconds(FIXED_NOW),
            ),
        )
        contents.put(filePath, json)
    }
}

private object BrokenDraftStore : CourseDraftStore {
    override fun drafts(): Flow<List<CourseDraft>> = emptyFlow()
    override suspend fun find(draftId: String): CourseDraft = error("store broke")
    override suspend fun upsert(draft: CourseDraft) = error("Not used")
    override suspend fun updateIfPresent(draft: CourseDraft): Boolean = error("Not used")
    override suspend fun delete(draftId: String) = error("Not used")
    override suspend fun clearForUser(userId: String): List<CourseDraft> = error("Not used")
}
