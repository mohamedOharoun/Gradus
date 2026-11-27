package es.ulpgc.pamn.dominio.course.publication

import kotlin.time.Instant

// Límite exacto del fichero JSON de un curso (2 MiB), igual que `CourseContent.MAX_SIZE_BYTES` del backend.
const val MAX_COURSE_BYTES: Int = 2_097_152

// Borrador local de un curso nuevo (`targetCourseId == null`) o de una nueva versión de un curso
// ya publicado (`targetCourseId != null`).
data class CourseDraft(
    val id: String,
    val userId: String,
    val targetCourseId: String?,
    val filePath: String,
    val title: String,
    val moduleCount: Int?,
    val status: DraftStatus,
    val updatedAt: Instant,
) {
    val isNewVersion: Boolean get() = targetCourseId != null

    init {
        require(id.isNotBlank()) { "draft id must not be blank" }
        require(userId.isNotBlank()) { "draft userId must not be blank" }
        require(filePath.isNotBlank()) { "draft filePath must not be blank" }
        require(targetCourseId == null || targetCourseId.isNotBlank()) {
            "draft targetCourseId must not be blank when provided"
        }
        require(moduleCount == null || moduleCount >= 0) {
            "draft moduleCount must be >= 0, was $moduleCount"
        }
    }
}
