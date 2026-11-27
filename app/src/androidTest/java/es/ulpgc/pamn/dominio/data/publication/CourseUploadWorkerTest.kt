package es.ulpgc.pamn.dominio.data.publication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.FakeCoursePublicationGateway
import es.ulpgc.pamn.dominio.course.publication.InMemoryCourseDraftContentStore
import es.ulpgc.pamn.dominio.course.publication.InMemoryCourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.course.publication.ProcessCourseDraft
import es.ulpgc.pamn.dominio.course.publication.PublishOutcome
import es.ulpgc.pamn.dominio.course.publication.ValidationOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

// Pruebas de CourseUploadWorker (paso 3b) sin tocar AppContainer: inyectan un ProcessCourseDraft
// controlado mediante CourseUploadWorker.factory(...) y TestListenableWorkerBuilder.
@RunWith(AndroidJUnit4::class)
class CourseUploadWorkerTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun buildWorker(
        processor: ProcessCourseDraft,
        inputData: Data = workDataOf(CourseUploadWorker.KEY_DRAFT_ID to DRAFT_ID),
        attempt: Int = 0,
    ): CourseUploadWorker =
        TestListenableWorkerBuilder<CourseUploadWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(CourseUploadWorker.factory(processor))
            .setRunAttemptCount(attempt)
            .build()

    @Test
    fun missing_draft_id_returns_failure_without_calling_gateway() = runTest {
        val fixture = UploadWorkerFixture().loggedIn().withPendingDraft()
        val worker = buildWorker(fixture.processor, inputData = Data.EMPTY)

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun blank_draft_id_returns_failure_without_calling_gateway() = runTest {
        val fixture = UploadWorkerFixture().loggedIn().withPendingDraft()
        val worker = buildWorker(
            fixture.processor,
            inputData = workDataOf(CourseUploadWorker.KEY_DRAFT_ID to "   "),
        )

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun valid_draft_validation_returns_success_and_marks_accepted() = runTest {
        val fixture = UploadWorkerFixture().loggedIn().withPendingDraft(PendingAction.VALIDATING)
        val worker = buildWorker(fixture.processor)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertEquals(1, fixture.gateway.validateNewCalls.size)
        assertEquals(DraftStatus.Accepted(CourseReport.EMPTY), fixture.drafts.find(DRAFT_ID)?.status)
    }

    @Test
    fun invalid_draft_validation_returns_success_and_marks_rejected() = runTest {
        val report = CourseReport(
            errors = listOf(CourseIssue("SCHEMA", "/title", "Falta el título")),
        )
        val fixture = UploadWorkerFixture(
            validateNewAnswer = { _, _ -> ValidationOutcome.Validated(report) },
        ).loggedIn().withPendingDraft(PendingAction.VALIDATING)
        val worker = buildWorker(fixture.processor)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertEquals(
            DraftStatus.Rejected(DraftRejection.ValidationReport(report)),
            fixture.drafts.find(DRAFT_ID)?.status,
        )
    }

    @Test
    fun publish_draft_returns_success_deletes_row_and_file_and_invalidates_cache() = runTest {
        val fixture = UploadWorkerFixture().loggedIn().withPendingDraft(PendingAction.PUBLISHING)
        val worker = buildWorker(fixture.processor)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertEquals(1, fixture.gateway.publishNewCalls.size)
        assertNull(fixture.drafts.find(DRAFT_ID))
        assertNull(fixture.contents.read(FILE_PATH))
        assertEquals(listOf("course-1"), fixture.invalidatedCourses)
    }

    @Test
    fun unreachable_returns_retry_even_on_high_attempt() = runTest {
        for (attempt in listOf(0, 4, 100)) {
            val fixture = UploadWorkerFixture(
                validateNewAnswer = { _, _ -> ValidationOutcome.Unreachable },
            ).loggedIn().withPendingDraft(PendingAction.VALIDATING)
            val worker = buildWorker(fixture.processor, attempt = attempt)

            val result = worker.doWork()

            assertEquals("attempt=$attempt", Result.retry(), result)
            assertEquals(
                DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.NETWORK),
                fixture.drafts.find(DRAFT_ID)?.status,
            )
        }
    }

    @Test
    fun server_failure_returns_retry_on_attempts_0_to_3() = runTest {
        for (attempt in 0..3) {
            val fixture = UploadWorkerFixture(
                validateNewAnswer = { _, _ -> ValidationOutcome.ServerFailure },
            ).loggedIn().withPendingDraft(PendingAction.VALIDATING)
            val worker = buildWorker(fixture.processor, attempt = attempt)

            val result = worker.doWork()

            assertEquals("attempt=$attempt", Result.retry(), result)
            assertEquals(
                DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SERVER_FAILURE),
                fixture.drafts.find(DRAFT_ID)?.status,
            )
        }
    }

    @Test
    fun server_failure_returns_failure_on_attempt_4_and_above() = runTest {
        for (attempt in listOf(4, 5, 10)) {
            val fixture = UploadWorkerFixture(
                validateNewAnswer = { _, _ -> ValidationOutcome.ServerFailure },
            ).loggedIn().withPendingDraft(PendingAction.VALIDATING)
            val worker = buildWorker(fixture.processor, attempt = attempt)

            val result = worker.doWork()

            assertEquals("attempt=$attempt", Result.failure(), result)
            assertEquals(
                DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SERVER_FAILURE),
                fixture.drafts.find(DRAFT_ID)?.status,
            )
        }
    }

    @Test
    fun session_expired_clears_session_marks_notice_and_returns_failure() = runTest {
        val fixture = UploadWorkerFixture(
            validateNewAnswer = { _, _ -> ValidationOutcome.SessionExpired },
        ).loggedIn().withPendingDraft(PendingAction.VALIDATING)
        val worker = buildWorker(fixture.processor)

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        assertNull(fixture.sessions.current())
        assertEquals(
            DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SESSION_EXPIRED),
            fixture.drafts.find(DRAFT_ID)?.status,
        )
    }

    @Test
    fun draft_deleted_before_start_returns_success_without_calling_gateway() = runTest {
        val fixture = UploadWorkerFixture().loggedIn()
        val worker = buildWorker(fixture.processor)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun draft_deleted_during_upload_returns_success_without_resurrecting_row() = runTest {
        val sessions = InMemorySessionStore().apply { save(ANA_SESSION) }
        val drafts = InMemoryCourseDraftStore(sessions).apply {
            upsert(pendingDraft(PendingAction.VALIDATING))
        }
        val contents = InMemoryCourseDraftContentStore(mapOf(FILE_PATH to SAMPLE_JSON))
        val gateway = FakeCoursePublicationGateway(
            validateNewAnswer = { _, _ ->
                drafts.delete(DRAFT_ID)
                ValidationOutcome.Validated(CourseReport.EMPTY)
            },
        )
        val processor = ProcessCourseDraft(
            drafts = drafts,
            contents = contents,
            sessions = sessions,
            gateway = gateway,
            cacheInvalidator = {},
        )
        val worker = buildWorker(processor)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        assertEquals(1, gateway.validateNewCalls.size)
        assertNull(drafts.find(DRAFT_ID))
    }

    @Test
    fun store_exception_propagates_out_of_do_work() = runTest {
        val sessions = InMemorySessionStore().apply { save(ANA_SESSION) }
        val processor = ProcessCourseDraft(
            drafts = FailingDraftStore,
            contents = InMemoryCourseDraftContentStore(),
            sessions = sessions,
            gateway = FakeCoursePublicationGateway(),
            cacheInvalidator = {},
        )
        val worker = buildWorker(processor)

        val failure = runCatching { worker.doWork() }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(FailingDraftStore.MESSAGE, failure?.message)
    }

    @Test
    fun coroutine_cancellation_propagates_out_of_do_work() = runTest {
        val entered = CompletableDeferred<Unit>()
        val hang = CompletableDeferred<ValidationOutcome>()
        val fixture = UploadWorkerFixture(
            validateNewAnswer = { _, _ ->
                entered.complete(Unit)
                hang.await()
            },
        ).loggedIn().withPendingDraft(PendingAction.VALIDATING)
        val worker = buildWorker(fixture.processor)

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
        assertEquals(
            DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.NONE),
            fixture.drafts.find(DRAFT_ID)?.status,
        )
    }
}

private const val ANA_ID = "ana"
private const val DRAFT_ID = "draft-1"
private const val FILE_PATH = "course_drafts/draft-1.json"
private val SAMPLE_JSON = """{"title":"Grafos"}""".encodeToByteArray()
private val UPDATED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000L)

private val ANA_SESSION = Session(
    token = "token-ana",
    expiresAt = "2099-01-01T00:00:00Z",
    account = Account(id = ANA_ID, name = "Ana", email = "ana@example.com", createdAt = "2026-01-01T00:00:00Z"),
)

private fun pendingDraft(action: PendingAction) = CourseDraft(
    id = DRAFT_ID,
    userId = ANA_ID,
    targetCourseId = null,
    filePath = FILE_PATH,
    title = "Grafos",
    moduleCount = 3,
    status = DraftStatus.Pending(action),
    updatedAt = UPDATED_AT,
)

private class UploadWorkerFixture(
    validateNewAnswer: suspend (Session, ByteArray) -> ValidationOutcome = { _, _ ->
        ValidationOutcome.Validated(CourseReport.EMPTY)
    },
    publishNewAnswer: (suspend (Session, ByteArray) -> PublishOutcome)? = null,
) {
    val sessions = InMemorySessionStore()
    val drafts = InMemoryCourseDraftStore(sessions)
    val contents = InMemoryCourseDraftContentStore()
    val invalidatedCourses = mutableListOf<String>()
    val gateway = if (publishNewAnswer == null) {
        FakeCoursePublicationGateway(validateNewAnswer = validateNewAnswer)
    } else {
        FakeCoursePublicationGateway(
            validateNewAnswer = validateNewAnswer,
            publishNewAnswer = publishNewAnswer,
        )
    }
    val processor = ProcessCourseDraft(
        drafts = drafts,
        contents = contents,
        sessions = sessions,
        gateway = gateway,
        cacheInvalidator = { invalidatedCourses += it },
    )

    suspend fun loggedIn(): UploadWorkerFixture = apply { sessions.save(ANA_SESSION) }

    suspend fun withPendingDraft(action: PendingAction = PendingAction.VALIDATING): UploadWorkerFixture = apply {
        drafts.upsert(pendingDraft(action))
        contents.put(FILE_PATH, SAMPLE_JSON)
    }
}

private object FailingDraftStore : CourseDraftStore {
    const val MESSAGE = "draft store broke"
    override fun drafts(): Flow<List<CourseDraft>> = emptyFlow()
    override suspend fun find(draftId: String): CourseDraft = error(MESSAGE)
    override suspend fun upsert(draft: CourseDraft) = error("Not used")
    override suspend fun updateIfPresent(draft: CourseDraft): Boolean = error("Not used")
    override suspend fun delete(draftId: String) = error("Not used")
    override suspend fun clearForUser(userId: String): List<CourseDraft> = error("Not used")
}
