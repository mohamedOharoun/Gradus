package es.ulpgc.pamn.dominio.course.publication

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProcessCourseDraftValidationTest {

    @Test
    fun `validating new course accepts valid empty report and calls validateNew`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            validateNewAnswer = { _, _ -> ValidationOutcome.Validated(CourseReport.EMPTY) },
        )
        val fixture = ProcessDraftFixture(gateway)
        val session = fixture.logIn()
        val draft = fixture.saveDraft(testDraft(targetCourseId = null))

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertEquals(
            draft.copy(status = DraftStatus.Accepted(CourseReport.EMPTY), updatedAt = T1),
            fixture.drafts.find(draft.id),
        )
        assertEquals(1, gateway.validateNewCalls.size)
        assertEquals(session, gateway.validateNewCalls.single().first)
        assertContentEquals(SAMPLE_JSON, gateway.validateNewCalls.single().second)
        assertTrue(gateway.validateVersionCalls.isEmpty())
        assertTrue(fixture.invalidatedCourseIds.isEmpty())
    }

    @Test
    fun `validating new version preserves warnings in Accepted and calls validateVersion`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            validateVersionAnswer = { _, _, _ -> ValidationOutcome.Validated(SAMPLE_WARNING_REPORT) },
        )
        val fixture = ProcessDraftFixture(gateway)
        val session = fixture.logIn()
        val draft = fixture.saveDraft(testDraft(targetCourseId = "course-7"))

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertEquals(
            draft.copy(status = DraftStatus.Accepted(SAMPLE_WARNING_REPORT), updatedAt = T1),
            fixture.drafts.find(draft.id),
        )
        assertEquals(1, gateway.validateVersionCalls.size)
        assertEquals(session, gateway.validateVersionCalls.single().session)
        assertEquals("course-7", gateway.validateVersionCalls.single().courseId)
        assertContentEquals(SAMPLE_JSON, gateway.validateVersionCalls.single().courseJson)
        assertTrue(gateway.validateNewCalls.isEmpty())
    }

    @Test
    fun `validating invalid report transitions draft to Rejected with ValidationReport`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            validateNewAnswer = { _, _ -> ValidationOutcome.Validated(SAMPLE_ERROR_REPORT) },
        )
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertEquals(
            draft.copy(
                status = DraftStatus.Rejected(DraftRejection.ValidationReport(SAMPLE_ERROR_REPORT)),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `validating terminal error outcomes reject draft with matching reason`() = runTest {
        val cases = listOf(
            ValidationOutcome.TooLarge to DraftRejection.TooLarge,
            ValidationOutcome.IdenticalToLatest to DraftRejection.IdenticalToLatest,
            ValidationOutcome.NotAuthor to DraftRejection.NotAuthor,
            ValidationOutcome.NotFound to DraftRejection.CourseNotFound,
        )

        for ((outcome, expectedRejection) in cases) {
            val gateway = FakeCoursePublicationGateway(
                validateVersionAnswer = { _, _, _ -> outcome },
            )
            val fixture = ProcessDraftFixture(gateway)
            fixture.logIn()
            val draft = fixture.saveDraft(testDraft(targetCourseId = "course-1"))

            val decision = fixture.processor.process(draft.id)

            assertEquals(DraftWorkDecision.Done, decision)
            assertEquals(
                draft.copy(status = DraftStatus.Rejected(expectedRejection), updatedAt = T1),
                fixture.drafts.find(draft.id),
            )
        }
    }

    @Test
    fun `validating SessionExpired clears current session marks notice and gives up`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            validateNewAnswer = { _, _ -> ValidationOutcome.SessionExpired },
        )
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.GiveUp, decision)
        assertNull(fixture.sessions.current())
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SESSION_EXPIRED),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `validating SessionExpired keeps newer session saved while request was in flight`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        val oldSession = fixture.logIn(testSession("ana", token = "tok-old"))
        val newSession = testSession("ana", token = "tok-new")
        val draft = fixture.saveDraft(testDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.sessions.save(newSession)
        gate.complete(ValidationOutcome.SessionExpired)

        assertEquals(DraftWorkDecision.GiveUp, deferred.await())
        assertEquals(newSession, fixture.sessions.current())
        assertEquals(oldSession, gateway.validateNewCalls.single().first)
    }

    @Test
    fun `validating Unreachable marks network notice and retries even on high attempt`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            validateNewAnswer = { _, _ -> ValidationOutcome.Unreachable },
        )
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        assertEquals(DraftWorkDecision.Retry, fixture.processor.process(draft.id, attempt = 0))
        assertEquals(DraftWorkDecision.Retry, fixture.processor.process(draft.id, attempt = 50))
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.NETWORK),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `validating ServerFailure retries on attempts 0 through 3 and gives up on fifth attempt`() = runTest {
        val gateway = FakeCoursePublicationGateway(
            validateNewAnswer = { _, _ -> ValidationOutcome.ServerFailure },
        )
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        for (attempt in 0 until MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS - 1) {
            assertEquals(DraftWorkDecision.Retry, fixture.processor.process(draft.id, attempt = attempt))
        }
        assertEquals(
            DraftWorkDecision.GiveUp,
            fixture.processor.process(draft.id, attempt = MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS - 1),
        )
        assertEquals(
            DraftWorkDecision.GiveUp,
            fixture.processor.process(draft.id, attempt = MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS + 2),
        )
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SERVER_FAILURE),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `validating discarded in flight does not resurrect draft on success`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(ValidationOutcome.Validated(CourseReport.EMPTY))

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `validating discarded in flight does not resurrect draft on rejection`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(ValidationOutcome.TooLarge)

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `validating discarded in flight returns Done instead of Retry on Unreachable`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val deferred = async { fixture.processor.process(draft.id, attempt = 0) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(ValidationOutcome.Unreachable)

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `validating discarded in flight returns Done instead of Retry on ServerFailure`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val deferred = async { fixture.processor.process(draft.id, attempt = 0) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(ValidationOutcome.ServerFailure)

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `validating discarded in flight clears session without resurrecting draft on SessionExpired`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.drafts.delete(draft.id)
        gate.complete(ValidationOutcome.SessionExpired)

        assertEquals(DraftWorkDecision.GiveUp, deferred.await())
        assertNull(fixture.sessions.current())
        assertNull(fixture.drafts.find(draft.id))
    }

    @Test
    fun `validating SessionExpired when session was already cleared does not throw`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft())

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.sessions.clear()
        gate.complete(ValidationOutcome.SessionExpired)

        assertEquals(DraftWorkDecision.GiveUp, deferred.await())
        assertNull(fixture.sessions.current())
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SESSION_EXPIRED),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
    }

    @Test
    fun `validating stamps updatedAt when server response arrives`() = runTest {
        val gate = CompletableDeferred<ValidationOutcome>()
        val gateway = FakeCoursePublicationGateway(validateNewAnswer = { _, _ -> gate.await() })
        val fixture = ProcessDraftFixture(gateway, initialClock = T0)
        fixture.logIn()
        val draft = fixture.saveDraft(testDraft(updatedAt = T0))

        val deferred = async { fixture.processor.process(draft.id) }
        yield()
        fixture.clock.current = T1
        gate.complete(ValidationOutcome.Validated(CourseReport.EMPTY))

        assertEquals(DraftWorkDecision.Done, deferred.await())
        assertEquals(T1, fixture.drafts.find(draft.id)?.updatedAt)
    }
}
