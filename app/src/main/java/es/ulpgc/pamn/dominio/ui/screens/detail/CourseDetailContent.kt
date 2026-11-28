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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.TagLabel
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Cuerpo desplazable de la ficha del curso debajo de la cabecera: descripción, lista de módulos
// y etiquetas al final (Decisiones C-1, C-2 y C-3).
@Composable
fun CourseDetailBody(state: CourseDetailState.Ready, modifier: Modifier = Modifier) {
    val course = state.course
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (course.description.isNotBlank()) {
            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (state.modules.isNotEmpty()) {
            LearnList(state)
        }
        if (course.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                course.tags.forEach { TagLabel(it) }
            }
        }
    }
}

// «Qué vas a aprender»: los módulos en el orden del camino, numerados; los dominados, con ✓.
@Composable
fun LearnList(state: CourseDetailState.Ready, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.detail_learn_title),
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
    val masteredText = stringResource(R.string.module_state_mastered)
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$number, ${module.title}"
            if (mastered) stateDescription = masteredText
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

@LightDarkPreviews
@Composable
private fun CourseDetailBodyPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CourseDetailBody(
                state = CourseDetailState.Ready(
                    course = demoCourse.copy(
                        description = "Conjuntos, relaciones, grafos y árboles paso a paso, con ejercicios interactivos.",
                    ),
                    mastered = demoMastered,
                    enrolled = true,
                ),
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
