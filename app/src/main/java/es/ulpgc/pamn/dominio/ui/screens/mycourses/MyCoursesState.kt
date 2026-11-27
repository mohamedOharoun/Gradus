package es.ulpgc.pamn.dominio.ui.screens.mycourses

import es.ulpgc.pamn.dominio.course.publication.AuthorCourseFilter
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseReport

// Avisos no bloqueantes mostrados dentro de «Mis cursos» (red, servidor o fichero elegido).
enum class MyCoursesInlineNotice {
    UNREACHABLE,
    SERVER_FAILURE,
    FILE_TOO_LARGE,
    FILE_UNREADABLE,
}

// Datos necesarios para abrir la hoja inferior de motivos de rechazo o avisos previos a publicar.
data class OpenCourseReport(
    val courseTitle: String,
    val report: CourseReport,
    val isRejection: Boolean,
)

// Hueco preparado para futuras métricas del curso (estudiantes y valoración); hoy siempre es null.
data class AuthorCourseMetrics(
    val studentCount: Int,
    val rating: Double,
)

// Estado inmutable de la pantalla «Mis cursos».
data class MyCoursesUiState(
    val items: List<AuthorCourseItem> = emptyList(),
    val allCount: Int = 0,
    val filter: AuthorCourseFilter = AuthorCourseFilter.ALL,
    val counts: Map<AuthorCourseFilter, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val inlineNotice: MyCoursesInlineNotice? = null,
    val openReport: OpenCourseReport? = null,
    val pendingDiscard: CourseDraft? = null,
)

// Patrón «Parameter Object»: agrupa todos los eventos de «Mis cursos» en un único objeto con
// lambdas vacías por defecto para simplificar previews y tests.
data class MyCoursesActions(
    val onUploadCourse: () -> Unit = {},
    val onOpenGuide: () -> Unit = {},
    val onSelectFilter: (AuthorCourseFilter) -> Unit = {},
    val onUploadNewVersion: (AuthorCourseItem) -> Unit = {},
    val onPublish: (String) -> Unit = {},
    val onOpenReport: (AuthorCourseItem) -> Unit = {},
    val onCloseReport: () -> Unit = {},
    val onRequestDiscard: (CourseDraft) -> Unit = {},
    val onConfirmDiscard: () -> Unit = {},
    val onDismissDiscard: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onDismissNotice: () -> Unit = {},
)
