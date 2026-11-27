package es.ulpgc.pamn.dominio.course.publication.http

import es.ulpgc.pamn.dominio.course.http.CourseSummaryDto
import kotlinx.serialization.Serializable

// DTOs de las rutas de publicación y «Mis cursos» (backend/docs/api-cursos.md).
// Reutilizan CourseSummaryDto y comparten CourseIssueDto / CourseReportDto con CourseReportJson.

@Serializable
internal data class MyCoursesResponseDto(
    val courses: List<CourseSummaryDto>,
)

@Serializable
internal data class CourseIssueDto(
    val code: String,
    val path: String,
    val message: String,
)

@Serializable
internal data class CourseReportDto(
    val valid: Boolean,
    val errors: List<CourseIssueDto>,
    val warnings: List<CourseIssueDto>,
)

@Serializable
internal data class PublishedCourseResponseDto(
    val course: CourseSummaryDto,
    val warnings: List<CourseIssueDto>,
)
