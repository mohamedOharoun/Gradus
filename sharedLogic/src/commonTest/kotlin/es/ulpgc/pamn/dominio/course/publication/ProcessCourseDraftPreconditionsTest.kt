package es.ulpgc.pamn.dominio.course.publication

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProcessCourseDraftPreconditionsTest {

    @Test
    fun `negative attempt throws IllegalArgumentException`() = runTest {
        val fixture = ProcessDraftFixture()
        assertFailsWith<IllegalArgumentException> {
            fixture.processor.process(draftId = "missing", attempt = -1)
        }
    }

    @Test
    fun `discarded draft before starting returns Done without reading file or calling network`() = runTest {
        val fixture = ProcessDraftFixture()
        fixture.logIn()

        val decision = fixture.processor.process("non-existent")

        assertEquals(DraftWorkDecision.Done, decision)
        assertTrue(fixture.contents.readPaths.isEmpty())
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun `already accepted draft returns Done without touching store file or network`() = runTest {
        val fixture = ProcessDraftFixture()
        fixture.logIn()
        val accepted = fixture.saveDraft(testDraft(status = DraftStatus.Accepted(SAMPLE_WARNING_REPORT)))

        val decision = fixture.processor.process(accepted.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertEquals(accepted, fixture.drafts.find(accepted.id))
        assertTrue(fixture.contents.readPaths.isEmpty())
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun `already rejected draft returns Done without touching store file or network`() = runTest {
        val fixture = ProcessDraftFixture()
        fixture.logIn()
        val rejected = fixture.saveDraft(testDraft(status = DraftStatus.Rejected(DraftRejection.TooLarge)))

        val decision = fixture.processor.process(rejected.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertEquals(rejected, fixture.drafts.find(rejected.id))
        assertTrue(fixture.contents.readPaths.isEmpty())
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun `missing session marks validating draft with session expired notice and gives up`() = runTest {
        val fixture = ProcessDraftFixture()
        val draft = fixture.saveDraft(testDraft(status = DraftStatus.Pending(PendingAction.VALIDATING)))

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.GiveUp, decision)
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.SESSION_EXPIRED),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
        assertTrue(fixture.contents.readPaths.isEmpty())
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
    }

    @Test
    fun `missing session marks publishing draft with session expired notice and gives up`() = runTest {
        val fixture = ProcessDraftFixture()
        val draft = fixture.saveDraft(testDraft(status = DraftStatus.Pending(PendingAction.PUBLISHING)))

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.GiveUp, decision)
        assertEquals(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.PUBLISHING, PendingNotice.SESSION_EXPIRED),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
        assertTrue(fixture.contents.readPaths.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun `different user session never sends or modifies draft and gives up`() = runTest {
        val fixture = ProcessDraftFixture()
        fixture.logIn(testSession("beto"))
        val anaDraft = fixture.saveDraft(
            testDraft(userId = "ana", status = DraftStatus.Pending(PendingAction.PUBLISHING)),
        )

        val decision = fixture.processor.process(anaDraft.id)

        assertEquals(DraftWorkDecision.GiveUp, decision)
        assertEquals(anaDraft, fixture.drafts.find(anaDraft.id))
        assertTrue(fixture.contents.readPaths.isEmpty())
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
        assertTrue(fixture.gateway.publishNewCalls.isEmpty())
    }

    @Test
    fun `missing local file marks draft rejected with FileMissing and returns Done`() = runTest {
        val fixture = ProcessDraftFixture()
        fixture.logIn()
        val draft = fixture.saveDraft(
            testDraft(status = DraftStatus.Pending(PendingAction.VALIDATING)),
            bytes = null,
        )

        val decision = fixture.processor.process(draft.id)

        assertEquals(DraftWorkDecision.Done, decision)
        assertEquals(
            draft.copy(
                status = DraftStatus.Rejected(DraftRejection.FileMissing),
                updatedAt = T1,
            ),
            fixture.drafts.find(draft.id),
        )
        assertEquals(listOf(draft.filePath), fixture.contents.readPaths)
        assertTrue(fixture.gateway.validateNewCalls.isEmpty())
    }

    @Test
    fun `every DraftWorkDecision variant can be handled without else`() {
        fun label(decision: DraftWorkDecision): String = when (decision) {
            DraftWorkDecision.Done -> "done"
            DraftWorkDecision.Retry -> "retry"
            DraftWorkDecision.GiveUp -> "give up"
        }

        assertEquals("done", label(DraftWorkDecision.Done))
        assertEquals("retry", label(DraftWorkDecision.Retry))
        assertEquals("give up", label(DraftWorkDecision.GiveUp))
    }
}
