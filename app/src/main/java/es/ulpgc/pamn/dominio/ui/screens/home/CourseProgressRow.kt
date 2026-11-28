package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.home.HomeCourse
import es.ulpgc.pamn.dominio.ui.components.CourseInitialTile
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.TileTone
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Una fila de «Tus cursos» (Figma «Inicio»): inicial, título, «autor · 7 de 20» y «35 %». Toda la fila se
// toca (abre el curso). Para TalkBack es un solo botón: «Kotlin desde cero, Equipo PAMN, 7 de 20 conceptos
// dominados», con la acción «Abrir curso».
@Composable
fun CourseProgressRow(course: HomeCourse, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = course.course.title
    val author = course.course.publication?.author?.name
    // Fuera de semantics {}: ahí no se puede leer strings.xml.
    val description = courseRowDescription(title, author, course.masteredCount, course.total).asString()
    val openLabel = stringResource(R.string.course_open)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = openLabel) { onClick(); true }
            }
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CourseInitialTile(courseInitial(title), TileTone.of(course.course.id))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = courseDetailText(author, course.masteredCount, course.total).asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = percentText(course.percent).asString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@LightDarkPreviews
@Composable
private fun CourseProgressRowPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                HomePreviewData.courses.forEach { CourseProgressRow(it, onClick = {}) }
            }
        }
    }
}
