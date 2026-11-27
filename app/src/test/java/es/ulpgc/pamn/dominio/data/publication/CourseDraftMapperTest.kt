package es.ulpgc.pamn.dominio.data.publication

import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.CourseReportJson
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class CourseDraftMapperTest {

    private val updatedAt = Instant.parse("2026-11-27T18:45:12.123456789Z")

    private fun draft(
        status: DraftStatus = DraftStatus.Pending(PendingAction.VALIDATING),
        targetCourseId: String? = null,
        moduleCount: Int? = 4,
    ) = CourseDraft(
        id = "draft-1",
        userId = "user-ana",
        targetCourseId = targetCourseId,
        filePath = "course_drafts/draft-1.json",
        title = "Estructuras de datos",
        moduleCount = moduleCount,
        status = status,
        updatedAt = updatedAt,
    )

    private fun rawEntity(
        statusKind: String = CourseDraftStorageValues.STATUS_PENDING,
        pendingAction: String? = CourseDraftStorageValues.ACTION_VALIDATING,
        pendingNotice: String? = CourseDraftStorageValues.NOTICE_NONE,
        rejectionKind: String? = null,
        reportJson: String? = null,
        targetCourseId: String? = null,
        moduleCount: Int? = 4,
    ) = CourseDraftEntity(
        id = "draft-1",
        userId = "user-ana",
        targetCourseId = targetCourseId,
        filePath = "course_drafts/draft-1.json",
        title = "Estructuras de datos",
        moduleCount = moduleCount,
        statusKind = statusKind,
        pendingAction = pendingAction,
        pendingNotice = pendingNotice,
        rejectionKind = rejectionKind,
        reportJson = reportJson,
        updatedAt = updatedAt,
    )

    @Test
    fun `round trips every pending action and notice with explicit string values`() {
        for (action in PendingAction.entries) {
            for (notice in PendingNotice.entries) {
                val original = draft(status = DraftStatus.Pending(action, notice))

                val entity = original.toEntity()

                assertEquals(CourseDraftStorageValues.STATUS_PENDING, entity.statusKind)
                assertNotNull(entity.pendingAction)
                assertNotNull(entity.pendingNotice)
                assertNull(entity.rejectionKind)
                assertNull(entity.reportJson)
                assertEquals(original, entity.toDomain())
            }
        }
    }

    @Test
    fun `round trips accepted draft with empty report and with warnings`() {
        val emptyAccepted = draft(status = DraftStatus.Accepted(CourseReport.EMPTY))
        val warningsReport = CourseReport(
            warnings = listOf(
                CourseIssue("REMOVED_IDS", "/modules", "Se eliminaron módulos previos."),
                CourseIssue("SHORT_DESCRIPTION", "/description", "Descripción muy corta."),
            ),
        )
        val withWarnings = draft(status = DraftStatus.Accepted(warningsReport))

        for (original in listOf(emptyAccepted, withWarnings)) {
            val entity = original.toEntity()

            assertEquals(CourseDraftStorageValues.STATUS_ACCEPTED, entity.statusKind)
            assertNull(entity.pendingAction)
            assertNull(entity.pendingNotice)
            assertNull(entity.rejectionKind)
            assertNotNull(entity.reportJson)
            assertEquals(original, entity.toDomain())
        }
    }

    @Test
    fun `round trips rejected draft with validation report containing errors and warnings`() {
        val report = CourseReport(
            errors = listOf(
                CourseIssue("CYCLE_DETECTED", "/modules/2/prerequisites", "Ciclo en prerrequisitos."),
            ),
            warnings = listOf(
                CourseIssue("REMOVED_IDS", "/modules", "Falta el módulo 'intro'."),
            ),
        )
        val original = draft(status = DraftStatus.Rejected(DraftRejection.ValidationReport(report)))

        val entity = original.toEntity()

        assertEquals(CourseDraftStorageValues.STATUS_REJECTED, entity.statusKind)
        assertEquals(CourseDraftStorageValues.REJECTION_VALIDATION_REPORT, entity.rejectionKind)
        assertNull(entity.pendingAction)
        assertNull(entity.pendingNotice)
        assertNotNull(entity.reportJson)
        assertEquals(original, entity.toDomain())
    }

    @Test
    fun `round trips all singleton draft rejections without report json`() {
        val rejections = listOf(
            DraftRejection.IdenticalToLatest to CourseDraftStorageValues.REJECTION_IDENTICAL_TO_LATEST,
            DraftRejection.NotAuthor to CourseDraftStorageValues.REJECTION_NOT_AUTHOR,
            DraftRejection.CourseNotFound to CourseDraftStorageValues.REJECTION_COURSE_NOT_FOUND,
            DraftRejection.TooLarge to CourseDraftStorageValues.REJECTION_TOO_LARGE,
            DraftRejection.FileMissing to CourseDraftStorageValues.REJECTION_FILE_MISSING,
        )

        for ((rejection, expectedKind) in rejections) {
            val original = draft(status = DraftStatus.Rejected(rejection))

            val entity = original.toEntity()

            assertEquals(CourseDraftStorageValues.STATUS_REJECTED, entity.statusKind)
            assertEquals(expectedKind, entity.rejectionKind)
            assertNull(entity.pendingAction)
            assertNull(entity.pendingNotice)
            assertNull(entity.reportJson)
            assertEquals(original, entity.toDomain())
        }
    }

    @Test
    fun `preserves nullable targetCourseId, moduleCount, and nanosecond instant`() {
        val newCourseNoCount = draft(targetCourseId = null, moduleCount = null)
        val newVersionZeroModules = draft(targetCourseId = "course-42", moduleCount = 0)

        assertEquals(newCourseNoCount, newCourseNoCount.toEntity().toDomain())
        assertEquals(newVersionZeroModules, newVersionZeroModules.toEntity().toDomain())
        assertEquals(updatedAt, newVersionZeroModules.toEntity().toDomain().updatedAt)
    }

    @Test
    fun `accepted draft falls back to empty report when json is null, corrupt, or contains errors`() {
        val invalidReportJson = CourseReportJson.encode(
            CourseReport(errors = listOf(CourseIssue("SYNTAX", "", "JSON inválido"))),
        )
        val entities = listOf(
            rawEntity(statusKind = CourseDraftStorageValues.STATUS_ACCEPTED, reportJson = null),
            rawEntity(statusKind = CourseDraftStorageValues.STATUS_ACCEPTED, reportJson = "{corrupt"),
            rawEntity(statusKind = CourseDraftStorageValues.STATUS_ACCEPTED, reportJson = invalidReportJson),
        )

        for (entity in entities) {
            val status = entity.toDomain().status
            assertEquals(DraftStatus.Accepted(CourseReport.EMPTY), status)
        }
    }

    @Test
    fun `rejected validation report falls back to non-valid report when json is null, corrupt, or has no errors`() {
        val validReportJson = CourseReportJson.encode(CourseReport.EMPTY)
        val entities = listOf(
            rawEntity(
                statusKind = CourseDraftStorageValues.STATUS_REJECTED,
                rejectionKind = CourseDraftStorageValues.REJECTION_VALIDATION_REPORT,
                reportJson = null,
            ),
            rawEntity(
                statusKind = CourseDraftStorageValues.STATUS_REJECTED,
                rejectionKind = CourseDraftStorageValues.REJECTION_VALIDATION_REPORT,
                reportJson = "not-json",
            ),
            rawEntity(
                statusKind = CourseDraftStorageValues.STATUS_REJECTED,
                rejectionKind = CourseDraftStorageValues.REJECTION_VALIDATION_REPORT,
                reportJson = validReportJson,
            ),
        )

        for (entity in entities) {
            val (reason) = entity.toDomain().status as DraftStatus.Rejected
            val validationReport = reason as DraftRejection.ValidationReport
            assertFalse(validationReport.report.isValid)
            assertEquals(CourseDraftStorageValues.CORRUPT_REPORT_CODE, validationReport.report.errors.single().code)
        }
    }

    @Test
    fun `unknown status, pending action, pending notice, and rejection kind fall back safely`() {
        val unknownPending = rawEntity(
            statusKind = CourseDraftStorageValues.STATUS_PENDING,
            pendingAction = "UNKNOWN_ACTION",
            pendingNotice = "UNKNOWN_NOTICE",
        ).toDomain()
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.NONE), unknownPending.status)

        val nullPending = rawEntity(
            statusKind = CourseDraftStorageValues.STATUS_PENDING,
            pendingAction = null,
            pendingNotice = null,
        ).toDomain()
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.NONE), nullPending.status)

        val unknownRejection = rawEntity(
            statusKind = CourseDraftStorageValues.STATUS_REJECTED,
            rejectionKind = "FUTURE_REASON",
        ).toDomain()
        val rejected = unknownRejection.status as DraftStatus.Rejected
        assertTrue(rejected.reason is DraftRejection.ValidationReport)
        assertFalse((rejected.reason as DraftRejection.ValidationReport).report.isValid)

        val unknownStatus = rawEntity(
            statusKind = "FUTURE_STATUS",
            pendingAction = null,
            pendingNotice = null,
        ).toDomain()
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING, PendingNotice.NONE), unknownStatus.status)
    }

    @Test
    fun `sanitizes negative moduleCount and blank targetCourseId instead of throwing`() {
        val entity = rawEntity(targetCourseId = "   ", moduleCount = -3)

        val domain = entity.toDomain()

        assertNull(domain.targetCourseId)
        assertNull(domain.moduleCount)
    }
}
