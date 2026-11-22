package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.ui.components.TagLabel

// Las piezas de la ficha lista (ScreenCourseDetail), separadas para que ningún fichero sea largo.

// Título, autor, etiquetas, «2 módulos · nov 2026» y la descripción completa.
@Composable
fun CourseDetailHeader(state: CourseDetailState.Ready, modifier: Modifier = Modifier) {
    val course = state.course
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            course.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        course.authorText()?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (course.tags.isNotEmpty()) {
            // Todas las etiquetas (en la tarjeta de Buscar caben 3): aquí hay sitio.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                course.tags.forEach { TagLabel(it) }
            }
        }
        Text(
            course.metaText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // «nov 2026» se lee mal en voz alta: TalkBack oye «publicado en noviembre de 2026».
            modifier = Modifier.semantics { contentDescription = course.spokenMetaText() },
        )
        if (course.description.isNotBlank()) {
            Text(
                course.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// «Qué vas a aprender»: los módulos en el orden del camino, numerados; los dominados, con ✓.
@Composable
fun LearnList(state: CourseDetailState.Ready, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Qué vas a aprender",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        state.modules.forEachIndexed { index, module ->
            LearnItem(number = index + 1, module = module, mastered = state.isMastered(module))
        }
    }
}

// Un módulo de la lista. TalkBack lo lee de una vez: «1, Representar un grafo, dominado».
@Composable
private fun LearnItem(number: Int, module: Module, mastered: Boolean) {
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$number, ${module.title}"
            if (mastered) stateDescription = "dominado"
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = if (mastered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (mastered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (mastered) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
                } else {
                    Text("$number", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Text(module.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
