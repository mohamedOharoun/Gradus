package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.compose.runtime.Composable
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
import es.ulpgc.pamn.dominio.course.publication.countByFilter
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.screens.ScreenMyCourses
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import kotlin.time.Instant

// Previews de la pantalla «Mis cursos» en claro y oscuro para todos los estados principales.
// Aparte de ScreenMyCourses.kt para mantener ese fichero pequeño y limpio.

private val previewInstant = Instant.parse("2026-11-27T18:00:00Z")
private val previewAuthor = Author(id = "user-1", name = "Ana Ruiz")

private fun sampleSummary(
    id: String,
    title: String,
    moduleCount: Int,
    version: Int = 1,
) = CourseSummary(
    id = id,
    version = version,
    author = previewAuthor,
    publishedAt = "2026-11-20T10:00:00Z",
    title = title,
    description = "",
    language = "es",
    tags = emptyList(),
    moduleCount = moduleCount,
)

private fun sampleDraft(
    id: String,
    title: String,
    moduleCount: Int?,
    status: DraftStatus,
    targetCourseId: String? = null,
) = CourseDraft(
    id = id,
    userId = previewAuthor.id,
    targetCourseId = targetCourseId,
    filePath = "$id.json",
    title = title,
    moduleCount = moduleCount,
    status = status,
    updatedAt = previewInstant,
)

private val fullSampleItems = listOf(
    AuthorCourseItem(
        published = sampleSummary(id = "c-1", title = "Kotlin para Android", moduleCount = 18),
    ),
    AuthorCourseItem(
        published = sampleSummary(
            id = "c-2",
            title = "Programación funcional",
            moduleCount = 10,
            version = 2,
        ),
        draft = sampleDraft(
            id = "d-1",
            title = "Programación funcional",
            moduleCount = 12,
            status = DraftStatus.Accepted(
                CourseReport(
                    warnings = listOf(
                        CourseIssue(
                            code = "REMOVED_IDS",
                            path = "/modules",
                            message = "Se han eliminado módulos respecto a la versión anterior.",
                        ),
                    ),
                ),
            ),
            targetCourseId = "c-2",
        ),
    ),
    AuthorCourseItem(
        draft = sampleDraft(
            id = "d-2",
            title = "Redes de computadores",
            moduleCount = 12,
            status = DraftStatus.Pending(PendingAction.VALIDATING),
        ),
    ),
    AuthorCourseItem(
        draft = sampleDraft(
            id = "d-3",
            title = "Bases de datos",
            moduleCount = 11,
            status = DraftStatus.Rejected(
                DraftRejection.ValidationReport(
                    CourseReport(
                        errors = listOf(
                            CourseIssue(
                                code = "MISSING_FIELD",
                                path = "/modules/0/title",
                                message = "Falta el título del módulo.",
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

@LightDarkPreviews
@Composable
private fun ScreenMyCoursesFullListPreview() {
    GradusTheme {
        ScreenMyCourses(
            state = MyCoursesUiState(
                items = fullSampleItems,
                allCount = fullSampleItems.size,
                filter = AuthorCourseFilter.ALL,
                counts = fullSampleItems.countByFilter(),
            ),
            actions = MyCoursesActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenMyCoursesEmptyPreview() {
    GradusTheme {
        ScreenMyCourses(
            state = MyCoursesUiState(),
            actions = MyCoursesActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenMyCoursesFilterEmptyPreview() {
    val onlyPublished = listOf(
        AuthorCourseItem(
            published = sampleSummary(id = "c-1", title = "Kotlin para Android", moduleCount = 18),
        ),
    )
    GradusTheme {
        ScreenMyCourses(
            state = MyCoursesUiState(
                items = emptyList(),
                allCount = onlyPublished.size,
                filter = AuthorCourseFilter.REJECTED,
                counts = onlyPublished.countByFilter(),
            ),
            actions = MyCoursesActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenMyCoursesLoadingPreview() {
    GradusTheme {
        ScreenMyCourses(
            state = MyCoursesUiState(isLoading = true),
            actions = MyCoursesActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenMyCoursesOfflineNoticePreview() {
    val offlineDrafts = listOf(
        AuthorCourseItem(
            draft = sampleDraft(
                id = "d-10",
                title = "Compiladores",
                moduleCount = 8,
                status = DraftStatus.Pending(
                    action = PendingAction.PUBLISHING,
                    notice = PendingNotice.NETWORK,
                ),
            ),
        ),
    )
    GradusTheme {
        ScreenMyCourses(
            state = MyCoursesUiState(
                items = offlineDrafts,
                allCount = offlineDrafts.size,
                filter = AuthorCourseFilter.ALL,
                counts = offlineDrafts.countByFilter(),
                inlineNotice = MyCoursesInlineNotice.UNREACHABLE,
            ),
            actions = MyCoursesActions(),
        )
    }
}
