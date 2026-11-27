package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import kotlin.time.Instant

// Tarjeta de un curso creado por el usuario en «Mis cursos». Representa tanto cursos publicados
// como borradores (nuevos o de nueva versión) en una sola tarjeta, sin estado propio.
@Composable
fun AuthorCourseCard(
    item: AuthorCourseItem,
    actions: MyCoursesActions,
    modifier: Modifier = Modifier,
    metrics: AuthorCourseMetrics? = null,
) {
    val draft = item.draft
    val status = draft?.status
    val title = item.titleText().asString()
    val borderColor = if (status is DraftStatus.Rejected) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val hasActions = status !is DraftStatus.Pending
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (draft != null) 6.dp else 16.dp,
                bottom = if (hasActions) 8.dp else 16.dp,
            ),
        ) {
            AuthorCourseHeader(
                title = title,
                status = status,
                draft = draft,
                onRequestDiscard = actions.onRequestDiscard,
            )
            CourseMetricsLine(metrics)
            if (draft == null) Spacer(Modifier.height(6.dp))
            AuthorCourseBody(item = item, status = status)
            if (hasActions) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                AuthorCourseActionsRow(item = item, status = status, actions = actions)
            }
        }
    }
}

@Composable
private fun AuthorCourseHeader(
    title: String,
    status: DraftStatus?,
    draft: CourseDraft?,
    onRequestDiscard: (CourseDraft) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        StatusBadge(status = status)
        if (draft != null) {
            IconButton(
                onClick = { onRequestDiscard(draft) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.my_courses_discard_description, title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DraftStatus?) {
    val (container, content) = statusBadgeColors(status)
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = status.statusBadgeText().asString(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun statusBadgeColors(status: DraftStatus?): Pair<Color, Color> = when (status) {
    null -> GradusTheme.colors.publishedContainer to GradusTheme.colors.onPublishedContainer
    is DraftStatus.Accepted ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    is DraftStatus.Pending ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    is DraftStatus.Rejected ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

// Hueco reservado para futuras métricas de estudiantes/valoración: no dibuja nada cuando es null.
@Composable
private fun CourseMetricsLine(metrics: AuthorCourseMetrics?) {
    if (metrics == null) return
}

@Composable
private fun AuthorCourseBody(
    item: AuthorCourseItem,
    status: DraftStatus?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val publishedVersion = item.publishedVersion
        if (publishedVersion != null && item.draft != null) {
            Text(
                text = publishedVersionText(publishedVersion).asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (status) {
            null -> ModulesLine(item.displayModuleCount)
            is DraftStatus.Accepted -> {
                Text(
                    text = readySubtitle(item.displayModuleCount).asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is DraftStatus.Pending -> {
                ModulesLine(item.displayModuleCount)
                PendingDetails(status)
            }
            is DraftStatus.Rejected -> {
                ModulesLine(item.displayModuleCount)
                RejectedReasonBox(status.reason)
            }
        }
    }
}

@Composable
private fun ModulesLine(moduleCount: Int?) {
    if (moduleCount == null) return
    Text(
        text = modulesText(moduleCount).asString(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PendingDetails(status: DraftStatus.Pending) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_schedule),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = status.action.toText().asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    status.notice.toText()?.let { notice ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_error_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp),
            )
            Text(
                text = notice.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun RejectedReasonBox(reason: DraftRejection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_error_outline),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )
            Text(
                text = reason.toText().asString(),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AuthorCourseActionsRow(
    item: AuthorCourseItem,
    status: DraftStatus?,
    actions: MyCoursesActions,
) {
    val draft = item.draft
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { actions.onUploadNewVersion(item) },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_upload),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.my_courses_upload_new_version),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (status is DraftStatus.Accepted && draft != null) {
            if (status.report.hasWarnings) {
                TextButton(
                    onClick = { actions.onOpenReport(item) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.my_courses_see_warnings,
                            status.report.warnings.size,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            Button(
                onClick = { actions.onPublish(draft.id) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.my_courses_publish),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        if (status is DraftStatus.Rejected && status.reason is DraftRejection.ValidationReport) {
            OutlinedButton(
                onClick = { actions.onOpenReport(item) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.my_courses_see_reasons),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

private val previewInstant = Instant.parse("2026-11-27T18:00:00Z")
private val previewAuthor = Author(id = "user-1", name = "Ana Ruiz")

private fun previewSummary(
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

private fun previewDraft(
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

private val previewWarningsReport = CourseReport(
    warnings = listOf(
        CourseIssue(
            code = "REMOVED_IDS",
            path = "/modules",
            message = "Se han eliminado módulos respecto a la versión anterior.",
        ),
        CourseIssue(
            code = "SHORT_DESCRIPTION",
            path = "/description",
            message = "La descripción es muy breve.",
        ),
    ),
)

@LightDarkPreviews
@Composable
private fun AuthorCourseCardPrimaryStatesPreview() {
    val items = listOf(
        AuthorCourseItem(
            published = previewSummary(id = "c-1", title = "Kotlin para Android", moduleCount = 18),
        ),
        AuthorCourseItem(
            draft = previewDraft(
                id = "d-1",
                title = "Sistemas operativos",
                moduleCount = 9,
                status = DraftStatus.Accepted(previewWarningsReport),
            ),
        ),
        AuthorCourseItem(
            draft = previewDraft(
                id = "d-2",
                title = "Redes de computadores",
                moduleCount = 12,
                status = DraftStatus.Pending(PendingAction.VALIDATING),
            ),
        ),
        AuthorCourseItem(
            draft = previewDraft(
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
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { AuthorCourseCard(item = it, actions = MyCoursesActions()) }
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun AuthorCourseCardVariantsPreview() {
    val items = listOf(
        AuthorCourseItem(
            draft = previewDraft(
                id = "d-4",
                title = "Algoritmos y grafos",
                moduleCount = 14,
                status = DraftStatus.Accepted(),
            ),
        ),
        AuthorCourseItem(
            published = previewSummary(
                id = "c-2",
                title = "Programación funcional",
                moduleCount = 10,
                version = 2,
            ),
            draft = previewDraft(
                id = "d-5",
                title = "Programación funcional",
                moduleCount = 12,
                status = DraftStatus.Accepted(),
                targetCourseId = "c-2",
            ),
        ),
        AuthorCourseItem(
            draft = previewDraft(
                id = "d-6",
                title = "Compiladores",
                moduleCount = 8,
                status = DraftStatus.Pending(
                    action = PendingAction.PUBLISHING,
                    notice = PendingNotice.NETWORK,
                ),
            ),
        ),
        AuthorCourseItem(
            published = previewSummary(
                id = "c-3",
                title = "Estructuras de datos",
                moduleCount = 20,
                version = 3,
            ),
            draft = previewDraft(
                id = "d-7",
                title = "Estructuras de datos",
                moduleCount = 20,
                status = DraftStatus.Rejected(DraftRejection.IdenticalToLatest),
                targetCourseId = "c-3",
            ),
        ),
    )
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { AuthorCourseCard(item = it, actions = MyCoursesActions()) }
            }
        }
    }
}
