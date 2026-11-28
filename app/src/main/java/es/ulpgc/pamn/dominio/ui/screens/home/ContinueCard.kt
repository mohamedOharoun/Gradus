package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.home.HomeCourse
import es.ulpgc.pamn.dominio.ui.components.CourseInitialTile
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.TileTone
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// «Continúa donde lo dejaste» (Figma «Inicio»): el título de la sección y la tarjeta del curso. Solo se
// pinta con un curso que tenga siguiente módulo (homeSummary ya lo elige así: continueWith).
@Composable
fun ContinueSection(course: HomeCourse, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.home_continue_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        ContinueCard(course, onContinue)
    }
}

// La tarjeta: inicial grande, título y autor, «12 de 20 conceptos dominados», la barra, «• Siguiente: …» y
// «Continuar ›». Toda la tarjeta se toca. Para TalkBack es un solo botón con la frase entera y la acción
// «Continuar» (la barra y la flecha son decorativas: lo dicen las palabras).
@Composable
fun ContinueCard(course: HomeCourse, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val title = course.course.title
    val author = course.course.publication?.author?.name
    val next = course.next?.title.orEmpty()
    // Fuera de semantics {}: ahí no se puede leer strings.xml.
    val description = continueDescription(title, author, course.masteredCount, course.total, next).asString()
    val continueLabel = stringResource(R.string.action_continue)
    Surface(
        onClick = onContinue,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = continueLabel) { onContinue(); true }
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CourseTitle(course, title, author)
            Text(
                text = masteredOfText(course.masteredCount, course.total).asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { if (course.total == 0) 0f else course.masteredCount.toFloat() / course.total },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                // outlineVariant (el borde de la tarjeta): en oscuro, surfaceVariant es el mismo color que la
                // tarjeta y la pista no se vería.
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            // La caja ocupa todo lo que deja «Continuar ›» (que así queda a la derecha); dentro, la etiqueta
            // mide lo que su texto y solo se corta si de verdad no cabe.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).padding(end = 12.dp)) { NextModuleLabel(next) }
                ContinueLabel(continueLabel)
            }
        }
    }
}

@Composable
private fun CourseTitle(course: HomeCourse, title: String, author: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CourseInitialTile(courseInitial(title), TileTone.of(course.course.id), size = 56.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (author != null) {
                Text(author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// «• Siguiente: Grafos» en ámbar (el color de la frontera en el camino: lo que toca estudiar).
@Composable
private fun NextModuleLabel(moduleTitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
        Text(
            text = nextModuleText(moduleTitle).asString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// «Continuar ›»: parece un enlace, pero el toque lo recoge toda la tarjeta.
@Composable
private fun ContinueLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@LightDarkPreviews
@Composable
private fun ContinueSectionPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomePreviewData.summary.continueWith?.let {
                ContinueSection(it, onContinue = {}, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
