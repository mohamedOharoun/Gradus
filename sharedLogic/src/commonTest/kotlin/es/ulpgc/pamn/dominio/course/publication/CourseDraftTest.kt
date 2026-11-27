package es.ulpgc.pamn.dominio.course.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class CourseDraftTest {

    private val updatedAt = Instant.parse("2026-11-27T10:00:00Z")

    private val errorReport = CourseReport(
        errors = listOf(CourseIssue("MISSING_TITLE", "/title", "Falta título")),
    )

    private val warningReport = CourseReport(
        warnings = listOf(CourseIssue("DUPLICATE_TAG", "/tags/1", "Etiqueta repetida")),
    )

    private fun sampleDraft(
        id: String = "draft-1",
        userId: String = "user-1",
        targetCourseId: String? = null,
        filePath: String = "course_drafts/draft-1.json",
        title: String = "Grafos",
        moduleCount: Int? = 4,
        status: DraftStatus = DraftStatus.Pending(PendingAction.VALIDATING),
    ) = CourseDraft(
        id = id,
        userId = userId,
        targetCourseId = targetCourseId,
        filePath = filePath,
        title = title,
        moduleCount = moduleCount,
        status = status,
        updatedAt = updatedAt,
    )

    @Test
    fun `max course bytes matches two mebibytes`() {
        assertEquals(2 * 1024 * 1024, MAX_COURSE_BYTES)
        assertEquals(2_097_152, MAX_COURSE_BYTES)
    }

    @Test
    fun `pending status defaults notice to none and supports every action and notice`() {
        val defaultPending = DraftStatus.Pending(PendingAction.VALIDATING)
        assertEquals(PendingAction.VALIDATING, defaultPending.action)
        assertEquals(PendingNotice.NONE, defaultPending.notice)

        val notices = listOf(
            PendingNotice.NONE,
            PendingNotice.NETWORK,
            PendingNotice.SESSION_EXPIRED,
            PendingNotice.SERVER_FAILURE,
        )
        notices.forEach { notice ->
            val pending = DraftStatus.Pending(action = PendingAction.PUBLISHING, notice = notice)
            assertEquals(PendingAction.PUBLISHING, pending.action)
            assertEquals(notice, pending.notice)
        }
    }

    @Test
    fun `accepted status accepts empty report and report with only warnings`() {
        assertEquals(CourseReport.EMPTY, DraftStatus.Accepted().report)
        assertEquals(warningReport, DraftStatus.Accepted(warningReport).report)
    }

    @Test
    fun `accepted status rejects a report containing errors`() {
        assertFailsWith<IllegalArgumentException> {
            DraftStatus.Accepted(errorReport)
        }
    }

    @Test
    fun `accepted status copy rejects a report containing errors`() {
        val accepted = DraftStatus.Accepted(warningReport)
        assertFailsWith<IllegalArgumentException> {
            accepted.copy(report = errorReport)
        }
    }

    @Test
    fun `validation report rejection accepts a report with errors`() {
        val rejection = DraftRejection.ValidationReport(errorReport)
        assertEquals(errorReport, rejection.report)
    }

    @Test
    fun `validation report rejection rejects a valid report`() {
        assertFailsWith<IllegalArgumentException> {
            DraftRejection.ValidationReport(CourseReport.EMPTY)
        }
        assertFailsWith<IllegalArgumentException> {
            DraftRejection.ValidationReport(warningReport)
        }
    }

    @Test
    fun `validation report rejection copy rejects a valid report`() {
        val rejection = DraftRejection.ValidationReport(errorReport)
        assertFailsWith<IllegalArgumentException> {
            rejection.copy(report = CourseReport.EMPTY)
        }
        assertFailsWith<IllegalArgumentException> {
            rejection.copy(report = warningReport)
        }
    }

    @Test
    fun `rejected status supports every rejection reason`() {
        val reasons = listOf(
            DraftRejection.ValidationReport(errorReport),
            DraftRejection.IdenticalToLatest,
            DraftRejection.NotAuthor,
            DraftRejection.CourseNotFound,
            DraftRejection.TooLarge,
            DraftRejection.FileMissing,
        )

        reasons.forEach { reason ->
            assertEquals(reason, DraftStatus.Rejected(reason).reason)
        }
    }

    @Test
    fun `draft distinguishes new course and new version`() {
        val newCourseDraft = sampleDraft(targetCourseId = null, moduleCount = null)
        assertFalse(newCourseDraft.isNewVersion)

        val newVersionDraft = sampleDraft(targetCourseId = "course-1", moduleCount = 0)
        assertTrue(newVersionDraft.isNewVersion)
    }

    @Test
    fun `draft rejects blank id`() {
        assertFailsWith<IllegalArgumentException> { sampleDraft(id = "") }
        assertFailsWith<IllegalArgumentException> { sampleDraft(id = "   ") }
        assertFailsWith<IllegalArgumentException> { sampleDraft().copy(id = "") }
    }

    @Test
    fun `draft rejects blank user id`() {
        assertFailsWith<IllegalArgumentException> { sampleDraft(userId = "") }
        assertFailsWith<IllegalArgumentException> { sampleDraft(userId = "  ") }
        assertFailsWith<IllegalArgumentException> { sampleDraft().copy(userId = " ") }
    }

    @Test
    fun `draft rejects blank file path`() {
        assertFailsWith<IllegalArgumentException> { sampleDraft(filePath = "") }
        assertFailsWith<IllegalArgumentException> { sampleDraft(filePath = "\t") }
        assertFailsWith<IllegalArgumentException> { sampleDraft().copy(filePath = "") }
    }

    @Test
    fun `draft rejects non null blank target course id`() {
        assertFailsWith<IllegalArgumentException> { sampleDraft(targetCourseId = "") }
        assertFailsWith<IllegalArgumentException> { sampleDraft(targetCourseId = "   ") }
        assertFailsWith<IllegalArgumentException> { sampleDraft().copy(targetCourseId = "") }
    }

    @Test
    fun `draft rejects negative module count`() {
        assertFailsWith<IllegalArgumentException> { sampleDraft(moduleCount = -1) }
        assertFailsWith<IllegalArgumentException> { sampleDraft().copy(moduleCount = -5) }
    }
}
