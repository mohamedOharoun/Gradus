package es.ulpgc.pamn.dominio.course.publication

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProcessCourseDraftPublishTest {

    private fun publishingDraft(
        id: String = "draft-1",
        targetCourseId: String? = null,
    ) = testDraft(
        id = id,
        targetCourseId = targetCourseId,
        status = DraftStatus.Pending(PendingAction.PUBLISHING),
    )

    @Test
    fun `publishing new course deletes draft deletes file invalidates cache and returns Done`() = runTest {
        val summary = testSummary(id = "published-new-1", version = 1)
        val gateway = FakeCoursePublicationGateway(
            publishNewAnswer = { _, _ -> PublishOutcome.Published(summary, emptyList()) },
        )
        val fixture = ProcessDraftFixture(gateway)
        val session = fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft(targetCourseId = null))

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertNull(fixture.drafts.find(draft.id))
        assertNull(fixture.contents.peek(draft.filePath))
        assertEquals(listOf(draft.filePath), fixture.contents.deletedPaths)
        assertEquals(listOf("published-new-1"), fixture.invalidatedCourseIds)
        assertEquals(1, gateway.publishNewCalls.size)
        assertEquals(session, gateway.publishNewCalls.single().first)
        assertContentEquals(SAMPLE_JSON, gateway.publishNewCalls.single().second)
        assertTrue(gateway.publishVersionCalls.isEmpty())
    }

    @Test
    fun `publishing new version deletes draft deletes file invalidates cache and returns Done`() = runTest {
        val summary = testSummary(id = "course-9", version = 2)
        val gateway = FakeCoursePublicationGateway(
            publishVersionAnswer = { _, _, _ -> PublishOutcome.Published(summary, SAMPLE_WARNING_REPORT.warnings) },
        )
        val fixture = ProcessDraftFixture(gateway)
        val session = fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft(targetCourseId = "course-9"))

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertNull(fixture.drafts.find(draft.id))
        assertNull(fixture.contents.peek(draft.filePath))
        assertEquals(listOf("course-9"), fixture.invalidatedCourseIds)
        assertEquals(1, gateway.publishVersionCalls.size)
        assertEquals(session, gateway.publishVersionCalls.single().session)
        assertEquals("course-9", gateway.publishVersionCalls.single().courseId)
        assertContentEquals(SAMPLE_JSON, gateway.publishVersionCalls.single().courseJson)
        assertTrue(gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun `publishing terminal error outcomes reject draft without deleting file or invalidating cache`() = runTest {
        val cases = listOf(
            PublishOutcome.Invalid(SAMPLE_ERROR_REPORT) to DraftRejection.ValidationReport(SAMPLE_ERROR_REPORT),
            PublishOutcome.TooLarge to DraftRejection.TooLarge,
            PublishOutcome.IdenticalToLatest to DraftRejection.IdenticalToLatest,
            PublishOutcome.NotAuthor to DraftRejection.NotAuthor,
            PublishOutcome.NotFound to DraftRejection.CourseNotFound,
        )

        for ((outcome, expectedRejection) in cases) {
            val gateway = FakeCoursePublicationGateway(
                publishVersionAnswer = { _, _, _ -> outcome },
            )
            val fixture = ProcessDraftFixture(gateway)
            fixture.logIn()
            val draft = fixture.saveDraft(publishingDraft(targetCourseId = "course-9"))

            val decision = fixture.processor.process(draft.id)

            assertEquals(DraftWorkDecision.Done, decision)
            assertEquals(
                draft.copy(status = DraftStatus.Rejected(expectedRejection), updatedAt = T1),
                fixture.drafts.find(draft.id),
            )
            assertNotNull(fixture.contents.peek(draft.filePath))
            assertTrue(fixture.invalidatedCourseIds.isEmpty())
        }
    }

    @Test
    fun `publishing SessionExpired clears current session marks notice and gives up`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            publishNewAnswer = { _, _ -> PublishOutcome.SessionExpired },
        )
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft())

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.GiveUp, decision)
        assertNull(fixture.sessions.current())
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.PUBLISHING, PendingNotice.SESSION_EXPIRED),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `publishing SessionExpired keeps newer session saved while request was in flight`() = runTest {
        val gate = CompletableDeferred<PublishOutcome>()
        val gateway = FakeCoursePublicationGateway(publishNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn(testSession("ana", token = "tok-old"))
        val newSession = testSession("ana", token = "tok-new")
        val draft = fixture.saveDraft(publishingDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.sessions.save(newSession)
        gate.complete(PublishOutcome.SessionExpired)

        assertEquals(DraftWorkDecision.GiveUp, deferred.await())
        assertEquals(newSession, fixture.sessions.current())
    }

    @Test
    fun `publishing Unreachable marks network notice and retries`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            publishNewAnswer = { _, _ -> PublishOutcome.Unreachable },
        )
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft())

        assertEquals(DraftWorkDecision.Retry, fixture.processor.process(draft.id, attempt = 0))
        assertEquals(DraftWorkDecision.Retry, fixture.processor.process(draft.id, attempt = 20))
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.PUBLISHING, PendingNotice.NETWORK),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `publishing ServerFailure retries on attempts 0 through 3 and gives up on fifth attempt`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            publishNewAnswer = { _, _ -> PublishOutcome.ServerFailure },
        )
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft())

        for (attempt in 0 until MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS - 1) {
            assertEquals(DraftWorkDecision.Retry, fixture.processor.process(draft.id, attempt = attempt))
        }
        assertEquals(
            DraftWorkDecision.GiveUp,
            fixture.processor.process(draft.id, attempt = MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS - 1),
        )
        assertEquals(
            DraftWorkDecision.GiveUp,
            fixture.processor.process(draft.id, attempt = MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS + 1),
        )
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.PUBLISHING, PendingNotice.SERVER_FAILURE),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `publishing discarded in flight still invalidates cache and returns Done on Published`() = runTest {
        val gate = CompletableDeferred<PublishOutcome>()
        val summary = testSummary(id = "course-9", version = 2)
        val gateway = FakeCoursePublicationGateway(publishVersionAnswer = { _, _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft(targetCourseId = "course-9"))

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.drafts.delete(draft.id)
        fixture.contents.delete(draft.filePath)
        gate.complete(PublishOutcome.Published(summary, emptyList()))

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
        assertNull(fixture.contents.peek(draft.filePath))
        assertEquals(listOf("course-9"), fixture.invalidatedCourseIds)
    }

    @Test
    fun `publishing discarded in flight does not resurrect draft on error`() = runTest {
        val gate = CompletableDeferred<PublishOutcome>()
        val gateway = FakeCoursePublicationGateway(publishNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(PublishOutcome.Invalid(SAMPLE_ERROR_REPORT))

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `publishing discarded in flight returns Done instead of Retry on Unreachable`() = runTest {
        val gate = CompletableDeferred<PublishOutcome>()
        val gateway = FakeCoursePublicationGateway(publishNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft())

        val deferred = async { fixture.processor.process(draft.id, attempt = 0) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(PublishOutcome.Unreachable)

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `publishing discarded in flight returns Done instead of Retry on ServerFailure`() = runTest {
        val gate = CompletableDeferred<PublishOutcome>()
        val gateway = FakeCoursePublicationGateway(publishNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft())

        val deferred = async { fixture.processor.process(draft.id, attempt = 0) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(PublishOutcome.ServerFailure)

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `publishing discarded in flight clears session without resurrecting draft on SessionExpired`() = runTest {
        val gate = CompletableDeferred<PublishOutcome>()
        val gateway = FakeCoursePublicationGateway(publishNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(publishingDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(PublishOutcome.SessionExpired)

        assertEquals(DraftWorkDecision.GiveUp, deferred.await())
        assertNull(fixture.sessions.current())
        assertNull(fixture.drafts.find(draft.id))
    }
}
