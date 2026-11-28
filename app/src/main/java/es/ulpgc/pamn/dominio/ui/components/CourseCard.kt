package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.data.demoSearchResults
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Un resultado de la búsqueda: título, autor, hasta 3 etiquetas y «20 módulos · oct 2026».
// Sin estado: pinta el curso y avisa con onClick. Los textos salen de CourseTexts.kt.
@Composable
fun CourseCard(
    course: CourseSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tags = course.tagsPreview()
    // Fuera de semantics {}: ahí no se puede leer strings.xml (no es un composable).
    val spoken = course.spokenText().asString()
    val openLabel = stringResource(R.string.course_open)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                // Toda la tarjeta se pulsa. Va dentro de la Surface para que la ondulación del
                // toque se vea encima del fondo y siga sus esquinas redondeadas.
                .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onClick)
                // Una sola cosa para TalkBack: lee la frase entera en vez de cada texto suelto.
                .clearAndSetSemantics { contentDescription = spoken }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.detail_by_author, course.author.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tags.visible.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.visible.forEach { TagLabel(it) }
                    if (tags.hidden > 0) TagLabel("+${tags.hidden}")
                }
            }
            Text(
                text = course.metaText().asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Las previews enseñan cada caso de la tarjeta: los 3 cursos del Figma y los dos casos raros.
@LightDarkPreviews
@Composable
private fun CourseCardPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                demoSearchResults.forEach { CourseCard(it, onClick = {}) }
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun CourseCardEdgeCasesPreview() {
    val base = demoSearchResults.first()
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Más de 3 etiquetas: se ven 3 y «+2».
                CourseCard(
                    course = base.copy(tags = listOf("grafos", "árboles", "algoritmos", "kotlin", "android")),
                    onClick = {},
                )
                // Fecha que no se entiende: solo «20 módulos».
                CourseCard(course = base.copy(publishedAt = "pronto"), onClick = {})
            }
        }
    }
}
