package es.ulpgc.pamn.dominio.data.publication

import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.CourseReportJson
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice

// Patrón Mapper: traducción pura entre el modelo de dominio `CourseDraft` y la fila Room
// `CourseDraftEntity`. Usa constantes de cadena explícitas (nunca `.ordinal`) y lectura defensiva
// para que una fila dañada en SQLite nunca cierre la app.
internal object CourseDraftStorageValues {
    const val STATUS_PENDING = "PENDING"
    const val STATUS_ACCEPTED = "ACCEPTED"
    const val STATUS_REJECTED = "REJECTED"

    const val ACTION_VALIDATING = "VALIDATING"
    const val ACTION_PUBLISHING = "PUBLISHING"

    const val NOTICE_NONE = "NONE"
    const val NOTICE_NETWORK = "NETWORK"
    const val NOTICE_SESSION_EXPIRED = "SESSION_EXPIRED"
    const val NOTICE_SERVER_FAILURE = "SERVER_FAILURE"

    const val REJECTION_VALIDATION_REPORT = "VALIDATION_REPORT"
    const val REJECTION_IDENTICAL_TO_LATEST = "IDENTICAL_TO_LATEST"
    const val REJECTION_NOT_AUTHOR = "NOT_AUTHOR"
    const val REJECTION_COURSE_NOT_FOUND = "COURSE_NOT_FOUND"
    const val REJECTION_TOO_LARGE = "TOO_LARGE"
    const val REJECTION_FILE_MISSING = "FILE_MISSING"

    const val CORRUPT_REPORT_CODE = "CORRUPT_REPORT"
}

private data class StatusColumns(
    val statusKind: String,
    val pendingAction: String? = null,
    val pendingNotice: String? = null,
    val rejectionKind: String? = null,
    val reportJson: String? = null,
)

fun CourseDraft.toEntity(): CourseDraftEntity {
    val (statusKind, pendingAction, pendingNotice, rejectionKind, reportJson) = status.toColumns()
    return CourseDraftEntity(
        id = id,
        userId = userId,
        targetCourseId = targetCourseId,
        filePath = filePath,
        title = title,
        moduleCount = moduleCount,
        statusKind = statusKind,
        pendingAction = pendingAction,
        pendingNotice = pendingNotice,
        rejectionKind = rejectionKind,
        reportJson = reportJson,
        updatedAt = updatedAt,
    )
}

fun CourseDraftEntity.toDomain(): CourseDraft = CourseDraft(
    id = id,
    userId = userId,
    targetCourseId = targetCourseId?.takeIf { it.isNotBlank() },
    filePath = filePath,
    title = title,
    moduleCount = moduleCount?.takeIf { it >= 0 },
    status = toDraftStatus(),
    updatedAt = updatedAt,
)

private fun DraftStatus.toColumns(): StatusColumns = when (this) {
    is DraftStatus.Pending -> StatusColumns(
        statusKind = CourseDraftStorageValues.STATUS_PENDING,
        pendingAction = action.toStorageValue(),
        pendingNotice = notice.toStorageValue(),
    )

    is DraftStatus.Accepted -> StatusColumns(
        statusKind = CourseDraftStorageValues.STATUS_ACCEPTED,
        reportJson = CourseReportJson.encode(report),
    )

    is DraftStatus.Rejected -> when (val rejection = reason) {
        is DraftRejection.ValidationReport -> StatusColumns(
            statusKind = CourseDraftStorageValues.STATUS_REJECTED,
            rejectionKind = CourseDraftStorageValues.REJECTION_VALIDATION_REPORT,
            reportJson = CourseReportJson.encode(rejection.report),
        )

        else -> StatusColumns(
            statusKind = CourseDraftStorageValues.STATUS_REJECTED,
            rejectionKind = rejection.toStorageValue(),
        )
    }
}

private fun CourseDraftEntity.toDraftStatus(): DraftStatus = when (statusKind) {
    CourseDraftStorageValues.STATUS_PENDING -> DraftStatus.Pending(
        action = parsePendingAction(pendingAction),
        notice = parsePendingNotice(pendingNotice),
    )

    CourseDraftStorageValues.STATUS_ACCEPTED -> {
        val safeReport = reportJson?.let(CourseReportJson::decode)
            ?.takeIf(CourseReport::isValid)
            ?: CourseReport.EMPTY
        DraftStatus.Accepted(safeReport)
    }

    CourseDraftStorageValues.STATUS_REJECTED -> DraftStatus.Rejected(parseRejection(rejectionKind, reportJson))

    else -> DraftStatus.Pending(
        action = parsePendingAction(pendingAction),
        notice = parsePendingNotice(pendingNotice),
    )
}

private fun PendingAction.toStorageValue(): String = when (this) {
    PendingAction.VALIDATING -> CourseDraftStorageValues.ACTION_VALIDATING
    PendingAction.PUBLISHING -> CourseDraftStorageValues.ACTION_PUBLISHING
}

private fun parsePendingAction(value: String?): PendingAction = when (value) {
    CourseDraftStorageValues.ACTION_VALIDATING -> PendingAction.VALIDATING
    CourseDraftStorageValues.ACTION_PUBLISHING -> PendingAction.PUBLISHING
    else -> PendingAction.VALIDATING
}

private fun PendingNotice.toStorageValue(): String = when (this) {
    PendingNotice.NONE -> CourseDraftStorageValues.NOTICE_NONE
    PendingNotice.NETWORK -> CourseDraftStorageValues.NOTICE_NETWORK
    PendingNotice.SESSION_EXPIRED -> CourseDraftStorageValues.NOTICE_SESSION_EXPIRED
    PendingNotice.SERVER_FAILURE -> CourseDraftStorageValues.NOTICE_SERVER_FAILURE
}

private fun parsePendingNotice(value: String?): PendingNotice = when (value) {
    CourseDraftStorageValues.NOTICE_NONE -> PendingNotice.NONE
    CourseDraftStorageValues.NOTICE_NETWORK -> PendingNotice.NETWORK
    CourseDraftStorageValues.NOTICE_SESSION_EXPIRED -> PendingNotice.SESSION_EXPIRED
    CourseDraftStorageValues.NOTICE_SERVER_FAILURE -> PendingNotice.SERVER_FAILURE
    else -> PendingNotice.NONE
}

private fun DraftRejection.toStorageValue(): String = when (this) {
    is DraftRejection.ValidationReport -> CourseDraftStorageValues.REJECTION_VALIDATION_REPORT
    DraftRejection.IdenticalToLatest -> CourseDraftStorageValues.REJECTION_IDENTICAL_TO_LATEST
    DraftRejection.NotAuthor -> CourseDraftStorageValues.REJECTION_NOT_AUTHOR
    DraftRejection.CourseNotFound -> CourseDraftStorageValues.REJECTION_COURSE_NOT_FOUND
    DraftRejection.TooLarge -> CourseDraftStorageValues.REJECTION_TOO_LARGE
    DraftRejection.FileMissing -> CourseDraftStorageValues.REJECTION_FILE_MISSING
}

private fun parseRejection(kind: String?, reportJson: String?): DraftRejection = when (kind) {
    CourseDraftStorageValues.REJECTION_VALIDATION_REPORT -> {
        val decoded = reportJson?.let(CourseReportJson::decode)
        val safeReport = if (decoded != null && !decoded.isValid) decoded else fallbackErrorReport()
        DraftRejection.ValidationReport(safeReport)
    }

    CourseDraftStorageValues.REJECTION_IDENTICAL_TO_LATEST -> DraftRejection.IdenticalToLatest
    CourseDraftStorageValues.REJECTION_NOT_AUTHOR -> DraftRejection.NotAuthor
    CourseDraftStorageValues.REJECTION_COURSE_NOT_FOUND -> DraftRejection.CourseNotFound
    CourseDraftStorageValues.REJECTION_TOO_LARGE -> DraftRejection.TooLarge
    CourseDraftStorageValues.REJECTION_FILE_MISSING -> DraftRejection.FileMissing
    else -> DraftRejection.ValidationReport(fallbackErrorReport())
}

private fun fallbackErrorReport(): CourseReport = CourseReport(
    errors = listOf(
        CourseIssue(
            code = CourseDraftStorageValues.CORRUPT_REPORT_CODE,
            path = "",
            message = "No se pudo leer el informe de validación guardado.",
        ),
    ),
)
