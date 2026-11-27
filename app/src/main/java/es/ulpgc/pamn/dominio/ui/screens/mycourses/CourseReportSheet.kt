package es.ulpgc.pamn.dominio.ui.screens.mycourses

import android.content.ClipData
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.text.ResourcesResolver
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Hoja inferior de motivos de rechazo y avisos previos a publicar. Sigue el mismo patrón de cierre
// animado que FilterSheet: `hideThen` evita que desaparezca de golpe al pulsar ✕ y `closing` impide
// avisar dos veces si llegan dos gestos seguidos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseReportSheet(
    openReport: OpenCourseReport,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    fun hideThen(action: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (scope.isActive) action() }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!closing) {
                closing = true
                onDismiss()
            }
        },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        CourseReportSheetContent(
            openReport = openReport,
            onClose = { hideThen(onDismiss) },
        )
    }
}

// Contenido de la hoja separado de la ventana modal para poder previsualizarlo y probarlo directo.
@Composable
fun CourseReportSheetContent(
    openReport: OpenCourseReport,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var copied by remember(openReport) { mutableStateOf(false) }
    val title = stringResource(
        if (openReport.isRejection) R.string.my_courses_report_reasons_title
        else R.string.my_courses_report_warnings_title,
    )
    val report = openReport.report

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        ReportSheetHeader(
            title = title,
            courseTitle = openReport.courseTitle,
            onClose = onClose,
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (report.errors.isEmpty() && report.warnings.isEmpty()) {
                Text(
                    text = stringResource(R.string.my_courses_report_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            if (report.errors.isNotEmpty()) {
                IssueSection(
                    heading = stringResource(
                        R.string.my_courses_report_section_count,
                        stringResource(R.string.my_courses_report_errors_heading),
                        report.errors.size,
                    ),
                    iconRes = R.drawable.ic_error_outline,
                    headerColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    issues = report.errors,
                )
            }
            if (report.warnings.isNotEmpty()) {
                IssueSection(
                    heading = stringResource(
                        R.string.my_courses_report_section_count,
                        stringResource(R.string.my_courses_report_warnings_heading),
                        report.warnings.size,
                    ),
                    iconRes = R.drawable.ic_warning,
                    headerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    issues = report.warnings,
                )
            }
        }
        Button(
            onClick = {
                val plainText = report.formatAsPlainText(
                    reportTextLabels(ResourcesResolver(resources)),
                )
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(title, plainText)))
                    copied = true
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
                .heightIn(min = 48.dp),
        ) {
            Icon(
                painter = painterResource(if (copied) R.drawable.ic_check else R.drawable.ic_content_copy),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (copied) R.string.my_courses_report_copied else R.string.my_courses_report_copy,
                ),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun ReportSheetHeader(
    title: String,
    courseTitle: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.my_courses_report_close),
            )
        }
    }
    if (courseTitle.isNotBlank()) {
        Text(
            text = courseTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
    } else {
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun IssueSection(
    heading: String,
    @DrawableRes iconRes: Int,
    headerColor: Color,
    containerColor: Color,
    contentColor: Color,
    issues: List<CourseIssue>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = headerColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = heading,
                style = MaterialTheme.typography.titleSmall,
                color = headerColor,
                modifier = Modifier.semantics { heading() },
            )
        }
        issues.forEach { issue ->
            IssueCard(
                issue = issue,
                containerColor = containerColor,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun IssueCard(
    issue: CourseIssue,
    containerColor: Color,
    contentColor: Color,
) {
    val displayPath = issue.path.ifBlank { stringResource(R.string.my_courses_report_root_path) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = displayPath,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "[${issue.code}]",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = issue.message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@LightDarkPreviews
@Composable
private fun CourseReportSheetRejectionPreview() {
    val openReport = OpenCourseReport(
        courseTitle = "Bases de datos",
        isRejection = true,
        report = CourseReport(
            errors = listOf(
                CourseIssue(
                    code = "MISSING_FIELD",
                    path = "/modules/0/title",
                    message = "Falta el título del módulo.",
                ),
                CourseIssue(
                    code = "CYCLE_DETECTED",
                    path = "",
                    message = "El grafo de prerrequisitos contiene un ciclo.",
                ),
            ),
            warnings = listOf(
                CourseIssue(
                    code = "SHORT_DESCRIPTION",
                    path = "/description",
                    message = "La descripción es muy breve.",
                ),
            ),
        ),
    )
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            CourseReportSheetContent(
                openReport = openReport,
                onClose = {},
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@LightDarkPreviews
@Composable
private fun CourseReportSheetWarningsPreview() {
    val openReport = OpenCourseReport(
        courseTitle = "Sistemas operativos",
        isRejection = false,
        report = CourseReport(
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
        ),
    )
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            CourseReportSheetContent(
                openReport = openReport,
                onClose = {},
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
