package es.ulpgc.pamn.dominio.ui.screens.mycourses

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseFilter
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.course.publication.ReportTextLabels
import es.ulpgc.pamn.dominio.ui.text.StringResolver
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.resolve

// Funciones puras de texto para «Mis cursos»: devuelven UiText para mantenerlas libres de Compose
// y poder probar todos los casos en JVM (MyCoursesTextsTest).

fun AuthorCourseFilter.filterLabel(): UiText = when (this) {
    AuthorCourseFilter.ALL -> UiText.of(R.string.my_courses_filter_all)
    AuthorCourseFilter.PUBLISHED -> UiText.of(R.string.my_courses_filter_published)
    AuthorCourseFilter.ACCEPTED -> UiText.of(R.string.my_courses_filter_accepted)
    AuthorCourseFilter.PENDING -> UiText.of(R.string.my_courses_filter_pending)
    AuthorCourseFilter.REJECTED -> UiText.of(R.string.my_courses_filter_rejected)
}

// «Todos · 4», «Publicados · 2», «Pendientes · 0»…
fun AuthorCourseFilter.filterChipText(count: Int): UiText =
    UiText.of(R.string.my_courses_filter_label, filterLabel(), count)

// Descripción accesible para TalkBack: «Filtrar cursos: Todos, 4».
fun AuthorCourseFilter.filterDescription(count: Int): UiText =
    UiText.of(R.string.my_courses_filter_description, filterLabel(), count)

// Etiqueta del badge de estado sobre los cuatro estados reales (null = publicado sin borrador).
fun DraftStatus?.statusBadgeText(): UiText = when (this) {
    null -> UiText.of(R.string.my_courses_status_published)
    is DraftStatus.Accepted -> UiText.of(R.string.my_courses_status_accepted)
    is DraftStatus.Pending -> UiText.of(R.string.my_courses_status_pending)
    is DraftStatus.Rejected -> UiText.of(R.string.my_courses_status_rejected)
}

fun AuthorCourseItem.statusBadgeText(): UiText = draft?.status.statusBadgeText()

// El título del curso lo escribe el autor (UiText.raw); si falta en un borrador roto, texto genérico.
fun AuthorCourseItem.titleText(): UiText =
    displayTitle.takeIf { it.isNotBlank() }?.let(UiText::raw)
        ?: UiText.of(R.string.my_courses_untitled_draft)

// «1 módulo», «12 módulos».
fun modulesText(count: Int): UiText = UiText.plural(R.plurals.course_modules, count)

// «Listo para publicar · 9 módulos» o «Listo para publicar» si no se conoce el número de módulos.
fun readySubtitle(moduleCount: Int?): UiText =
    moduleCount?.let { UiText.of(R.string.my_courses_ready_with_modules, modulesText(it)) }
        ?: UiText.of(R.string.my_courses_ready_to_publish)

// «Publicada: versión 2» cuando un curso publicado tiene además un borrador de nueva versión.
fun publishedVersionText(version: Int): UiText =
    UiText.of(R.string.my_courses_published_version, version)

fun PendingAction.toText(): UiText = when (this) {
    PendingAction.VALIDATING -> UiText.of(R.string.my_courses_pending_validating)
    PendingAction.PUBLISHING -> UiText.of(R.string.my_courses_pending_publishing)
}

fun PendingNotice.toText(): UiText? = when (this) {
    PendingNotice.NONE -> null
    PendingNotice.NETWORK -> UiText.of(R.string.my_courses_pending_notice_network)
    PendingNotice.SESSION_EXPIRED -> UiText.of(R.string.my_courses_pending_notice_session)
    PendingNotice.SERVER_FAILURE -> UiText.of(R.string.my_courses_pending_notice_server)
}

fun DraftRejection.toText(): UiText = when (this) {
    is DraftRejection.ValidationReport -> UiText.of(R.string.my_courses_rejected_validation)
    DraftRejection.IdenticalToLatest -> UiText.of(R.string.my_courses_rejected_identical)
    DraftRejection.NotAuthor -> UiText.of(R.string.my_courses_rejected_not_author)
    DraftRejection.CourseNotFound -> UiText.of(R.string.my_courses_rejected_not_found)
    DraftRejection.TooLarge -> UiText.of(R.string.my_courses_rejected_too_large)
    DraftRejection.FileMissing -> UiText.of(R.string.my_courses_rejected_file_missing)
}

fun MyCoursesInlineNotice.toText(): UiText = when (this) {
    MyCoursesInlineNotice.UNREACHABLE -> UiText.of(R.string.my_courses_notice_unreachable)
    MyCoursesInlineNotice.SERVER_FAILURE -> UiText.of(R.string.my_courses_notice_server)
    MyCoursesInlineNotice.FILE_TOO_LARGE -> UiText.of(R.string.my_courses_notice_file_too_large)
    MyCoursesInlineNotice.FILE_UNREADABLE -> UiText.of(R.string.my_courses_notice_file_unreadable)
}

// Etiquetas traducidas para exportar el informe de validación como texto plano («Copiar informe»).
fun reportTextLabels(resolver: StringResolver): ReportTextLabels = ReportTextLabels(
    errorsHeading = UiText.of(R.string.my_courses_report_errors_heading).resolve(resolver),
    warningsHeading = UiText.of(R.string.my_courses_report_warnings_heading).resolve(resolver),
    rootPathLabel = UiText.of(R.string.my_courses_report_root_path).resolve(resolver),
    emptyReportMessage = UiText.of(R.string.my_courses_report_empty).resolve(resolver),
)
