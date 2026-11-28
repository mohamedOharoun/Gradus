package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.ui.components.BackButton
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.moduleCountText
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Cabecera tintada de la ficha de un curso (docs/tareas/ficha-curso-ui.md, tareas C3–C4).
// Sin estado de negocio: recibe CourseDetailState.Ready y avisa hacia fuera con lambdas.
@Composable
fun CourseDetailHeader(
    state: CourseDetailState.Ready,
    onBack: () -> Unit,
    onOpenUnenrollDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val course = state.course
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeaderTopBar(
                    showMenu = state.enrolled,
                    onBack = onBack,
                    onOpenUnenrollDialog = onOpenUnenrollDialog,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    course.authorText()?.let {
                        Text(
                            text = it.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.enrolled) {
                    EnrolledProgress(state)
                } else {
                    UnenrolledStats(state)
                }
            }
        }
    }
}

@Composable
private fun HeaderTopBar(
    showMenu: Boolean,
    onBack: () -> Unit,
    onOpenUnenrollDialog: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        if (showMenu) {
            CourseOptionsMenu(onOpenUnenrollDialog = onOpenUnenrollDialog)
        }
    }
}

@Composable
private fun CourseOptionsMenu(onOpenUnenrollDialog: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.detail_more_options),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            // Decisión C-6: «Pausar curso» es provisional y solo cierra el menú.
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.detail_pause_course),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                onClick = { menuExpanded = false },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.detail_unenroll),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    menuExpanded = false
                    onOpenUnenrollDialog()
                },
            )
        }
    }
}

// Estadísticas para el estudiante no inscrito: número de módulos y, si hay fecha, publicación.
@Composable
private fun UnenrolledStats(state: CourseDetailState.Ready) {
    val course = state.course
    val shortDate = course.publishedShortDate()
    val spokenModules = moduleCountText(state.totalModules).asString()
    val spokenPublished = course.spokenPublishedText()?.asString()

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn(
                value = "${state.totalModules}",
                label = modulesStatLabel(state.totalModules).asString(),
                spokenDescription = spokenModules,
                modifier = Modifier.weight(1f),
            )
            if (shortDate != null) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                StatColumn(
                    value = shortDate.asString(),
                    label = stringResource(R.string.detail_stat_published),
                    spokenDescription = spokenPublished ?: shortDate.asString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    spokenDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = spokenDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Fecha de publicación y bloque de progreso para el estudiante ya inscrito.
@Composable
private fun EnrolledProgress(state: CourseDetailState.Ready) {
    val course = state.course
    val publishedIn = course.publishedInText()
    val spokenPublished = course.spokenPublishedText()?.asString()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (publishedIn != null) {
            Text(
                text = publishedIn.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (spokenPublished != null) {
                    Modifier.semantics { contentDescription = spokenPublished }
                } else {
                    Modifier
                },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = progressPercentText(state.progressPercent).asString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
            Text(
                text = masteredModulesText(state.masteredCount, state.totalModules).asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Previews ──

private val previewPublishedCourse = demoCourse.copy(
    publication = Publication(Author("u1", "Ana Ruiz"), version = 3, publishedAt = "2026-10-03T08:35:00Z"),
)

@LightDarkPreviews
@Composable
private fun CourseDetailHeaderUnenrolledPreview() {
    GradusTheme {
        CourseDetailHeader(
            state = CourseDetailState.Ready(previewPublishedCourse, enrolled = false),
            onBack = {},
            onOpenUnenrollDialog = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun CourseDetailHeaderEnrolledPreview() {
    GradusTheme {
        CourseDetailHeader(
            state = CourseDetailState.Ready(
                course = previewPublishedCourse,
                mastered = demoMastered,
                enrolled = true,
            ),
            onBack = {},
            onOpenUnenrollDialog = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun CourseDetailHeaderNoPublicationPreview() {
    GradusTheme {
        CourseDetailHeader(
            state = CourseDetailState.Ready(demoCourse, enrolled = false),
            onBack = {},
            onOpenUnenrollDialog = {},
        )
    }
}
