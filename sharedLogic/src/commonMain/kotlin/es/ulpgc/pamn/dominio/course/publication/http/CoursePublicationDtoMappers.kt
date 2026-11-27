package es.ulpgc.pamn.dominio.course.publication.http

import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.http.courseJson
import es.ulpgc.pamn.dominio.course.http.toCourseSummary
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.PublishOutcome

// Mappers puros entre los DTOs de publicación y el modelo del dominio.
// Reutilizan `courseJson` (ignoreUnknownKeys = true) y `toCourseSummary()` de course/http/.

internal fun decodeMyCourses(body: String): List<CourseSummary>? = try {
    courseJson.decodeFromString<MyCoursesResponseDto>(body).courses.map { it.toCourseSummary() }
} catch (_: IllegalArgumentException) {
    null
}

internal fun decodeCourseReport(body: String): CourseReport? = try {
    courseJson.decodeFromString<CourseReportDto>(body).toCourseReport()
} catch (_: IllegalArgumentException) {
    null
}

internal fun decodePublishedCourse(body: String): PublishOutcome.Published? = try {
    val (course, warnings) = courseJson.decodeFromString<PublishedCourseResponseDto>(body)
    PublishOutcome.Published(
        course = course.toCourseSummary(),
        warnings = warnings.map { it.toCourseIssue() },
    )
} catch (_: IllegalArgumentException) {
    null
}

internal fun encodeCourseReport(report: CourseReport): String =
    courseJson.encodeToString(report.toDto())

internal fun CourseReportDto.toCourseReport(): CourseReport {
    require(valid == errors.isEmpty()) { "Inconsistent CourseReportDto: valid=$valid with ${errors.size} errors" }
    return CourseReport(
        errors = errors.map { it.toCourseIssue() },
        warnings = warnings.map { it.toCourseIssue() },
    )
}

internal fun CourseReport.toDto() = CourseReportDto(
    valid = isValid,
    errors = errors.map { it.toDto() },
    warnings = warnings.map { it.toDto() },
)

internal fun CourseIssueDto.toCourseIssue() = CourseIssue(
    code = code,
    path = path,
    message = message,
)

internal fun CourseIssue.toDto() = CourseIssueDto(
    code = code,
    path = path,
    message = message,
)
