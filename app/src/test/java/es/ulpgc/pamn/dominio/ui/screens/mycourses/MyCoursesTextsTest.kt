package es.ulpgc.pamn.dominio.ui.screens.mycourses

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseFilter
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.course.publication.ReportTextLabels
import es.ulpgc.pamn.dominio.ui.text.StringResolver
import es.ulpgc.pamn.dominio.ui.text.UiText
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyCoursesTextsTest {

    private val instant = Instant.parse("2026-11-27T18:00:00Z")
    private val sampleErrorReport = CourseReport(
        errors = listOf(CourseIssue(code = "INVALID", path = "", message = "Error de prueba")),
    )

    // --- AuthorCourseFilter ---

    @Test
    fun `each filter maps to its label resource`() {
        assertEquals(UiText.of(R.string.my_courses_filter_all), AuthorCourseFilter.ALL.filterLabel())
        assertEquals(UiText.of(R.string.my_courses_filter_published), AuthorCourseFilter.PUBLISHED.filterLabel())
        assertEquals(UiText.of(R.string.my_courses_filter_accepted), AuthorCourseFilter.ACCEPTED.filterLabel())
        assertEquals(UiText.of(R.string.my_courses_filter_pending), AuthorCourseFilter.PENDING.filterLabel())
        assertEquals(UiText.of(R.string.my_courses_filter_rejected), AuthorCourseFilter.REJECTED.filterLabel())
    }

    @Test
    fun `filter chip text combines label and count including zero`() {
        assertEquals(
            UiText.of(R.string.my_courses_filter_label, UiText.of(R.string.my_courses_filter_all), 4),
            AuthorCourseFilter.ALL.filterChipText(4),
        )
        assertEquals(
            UiText.of(R.string.my_courses_filter_label, UiText.of(R.string.my_courses_filter_rejected), 0),
            AuthorCourseFilter.REJECTED.filterChipText(0),
        )
    }

    @Test
    fun `filter description includes label and count for accessibility`() {
        assertEquals(
            UiText.of(R.string.my_courses_filter_description, UiText.of(R.string.my_courses_filter_pending), 2),
            AuthorCourseFilter.PENDING.filterDescription(2),
        )
    }

    // --- statusBadgeText ---

    @Test
    fun `draft status badge text covers all four states`() {
        val publishedStatus: DraftStatus? = null
        assertEquals(UiText.of(R.string.my_courses_status_published), publishedStatus.statusBadgeText())
        assertEquals(
            UiText.of(R.string.my_courses_status_accepted),
            DraftStatus.Accepted().statusBadgeText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_status_pending),
            DraftStatus.Pending(PendingAction.VALIDATING).statusBadgeText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_status_rejected),
            DraftStatus.Rejected(DraftRejection.TooLarge).statusBadgeText(),
        )
    }

    @Test
    fun `author course item status badge delegates to its draft status`() {
        assertEquals(
            UiText.of(R.string.my_courses_status_published),
            AuthorCourseItem(published = summary()).statusBadgeText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_status_accepted),
            AuthorCourseItem(draft = draft(status = DraftStatus.Accepted())).statusBadgeText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_status_pending),
            AuthorCourseItem(draft = draft(status = DraftStatus.Pending(PendingAction.PUBLISHING))).statusBadgeText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_status_rejected),
            AuthorCourseItem(draft = draft(status = DraftStatus.Rejected(DraftRejection.FileMissing))).statusBadgeText(),
        )
    }

    // --- titleText ---

    @Test
    fun `title text uses raw display title when present`() {
        val item = AuthorCourseItem(published = summary(title = "Kotlin para Android"))
        assertEquals(UiText.raw("Kotlin para Android"), item.titleText())
    }

    @Test
    fun `title text falls back to untitled draft resource when blank`() {
        val item = AuthorCourseItem(draft = draft(title = "   "))
        assertEquals(UiText.of(R.string.my_courses_untitled_draft), item.titleText())
    }

    @Test
    fun `title text prefers non-blank draft title over published title`() {
        val item = AuthorCourseItem(
            published = summary(id = "c-1", title = "Título antiguo"),
            draft = draft(targetCourseId = "c-1", title = "Título nuevo"),
        )
        assertEquals(UiText.raw("Título nuevo"), item.titleText())
    }

    // --- modulesText, readySubtitle, publishedVersionText ---

    @Test
    fun `modules text uses course modules plural`() {
        assertEquals(UiText.plural(R.plurals.course_modules, 0), modulesText(0))
        assertEquals(UiText.plural(R.plurals.course_modules, 1), modulesText(1))
        assertEquals(UiText.plural(R.plurals.course_modules, 12), modulesText(12))
    }

    @Test
    fun `ready subtitle includes module count when known and omits it when null`() {
        assertEquals(
            UiText.of(R.string.my_courses_ready_with_modules, UiText.plural(R.plurals.course_modules, 9)),
            readySubtitle(9),
        )
        assertEquals(UiText.of(R.string.my_courses_ready_to_publish), readySubtitle(null))
    }

    @Test
    fun `published version text includes version number`() {
        assertEquals(UiText.of(R.string.my_courses_published_version, 3), publishedVersionText(3))
    }

    // --- PendingAction & PendingNotice ---

    @Test
    fun `pending action maps validating and publishing`() {
        assertEquals(UiText.of(R.string.my_courses_pending_validating), PendingAction.VALIDATING.toText())
        assertEquals(UiText.of(R.string.my_courses_pending_publishing), PendingAction.PUBLISHING.toText())
    }

    @Test
    fun `pending notice returns null for none and resource for each warning`() {
        assertNull(PendingNotice.NONE.toText())
        assertEquals(UiText.of(R.string.my_courses_pending_notice_network), PendingNotice.NETWORK.toText())
        assertEquals(UiText.of(R.string.my_courses_pending_notice_session), PendingNotice.SESSION_EXPIRED.toText())
        assertEquals(UiText.of(R.string.my_courses_pending_notice_server), PendingNotice.SERVER_FAILURE.toText())
    }

    // --- DraftRejection ---

    @Test
    fun `every draft rejection maps to its resource`() {
        assertEquals(
            UiText.of(R.string.my_courses_rejected_validation),
            DraftRejection.ValidationReport(sampleErrorReport).toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_rejected_identical),
            DraftRejection.IdenticalToLatest.toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_rejected_not_author),
            DraftRejection.NotAuthor.toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_rejected_not_found),
            DraftRejection.CourseNotFound.toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_rejected_too_large),
            DraftRejection.TooLarge.toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_rejected_file_missing),
            DraftRejection.FileMissing.toText(),
        )
    }

    // --- MyCoursesInlineNotice ---

    @Test
    fun `every inline notice maps to its resource`() {
        assertEquals(
            UiText.of(R.string.my_courses_notice_unreachable),
            MyCoursesInlineNotice.UNREACHABLE.toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_notice_server),
            MyCoursesInlineNotice.SERVER_FAILURE.toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_notice_file_too_large),
            MyCoursesInlineNotice.FILE_TOO_LARGE.toText(),
        )
        assertEquals(
            UiText.of(R.string.my_courses_notice_file_unreadable),
            MyCoursesInlineNotice.FILE_UNREADABLE.toText(),
        )
    }

    // --- reportTextLabels ---

    @Test
    fun `report text labels resolves all four strings and formats report`() {
        val resolver = FakeStringResolver(
            mapOf(
                R.string.my_courses_report_errors_heading to "Errors",
                R.string.my_courses_report_warnings_heading to "Warnings",
                R.string.my_courses_report_root_path to "root",
                R.string.my_courses_report_empty to "No errors or warnings.",
            ),
        )

        val labels = reportTextLabels(resolver)

        assertEquals(
            ReportTextLabels(
                errorsHeading = "Errors",
                warningsHeading = "Warnings",
                rootPathLabel = "root",
                emptyReportMessage = "No errors or warnings.",
            ),
            labels,
        )
        assertEquals(
            "Errors (1):\n- [INVALID] root: Error de prueba",
            sampleErrorReport.formatAsPlainText(labels),
        )
        assertEquals("No errors or warnings.", CourseReport.EMPTY.formatAsPlainText(labels))
    }

    private fun summary(id: String = "course-1", title: String = "Curso") = CourseSummary(
        id = id,
        version = 1,
        author = Author(id = "author-1", name = "Ana"),
        publishedAt = "2026-11-20T10:00:00Z",
        title = title,
        description = "",
        language = "es",
        tags = emptyList(),
        moduleCount = 5,
    )

    private fun draft(
        id: String = "draft-1",
        targetCourseId: String? = null,
        title: String = "Borrador",
        status: DraftStatus = DraftStatus.Accepted(),
    ) = CourseDraft(
        id = id,
        userId = "author-1",
        targetCourseId = targetCourseId,
        filePath = "$id.json",
        title = title,
        moduleCount = 5,
        status = status,
        updatedAt = instant,
    )

    private class FakeStringResolver(private val strings: Map<Int, String>) : StringResolver {
        override fun string(id: Int, args: List<Any>): String =
            strings[id] ?: error("Unexpected string resource id: $id")

        override fun plural(id: Int, count: Int, args: List<Any>): String =
            error("Unexpected plural resource id: $id")
    }
}
