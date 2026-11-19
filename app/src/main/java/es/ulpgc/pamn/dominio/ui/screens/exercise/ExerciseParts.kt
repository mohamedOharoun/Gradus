package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado

// Piezas de ScreenExercise, separadas para que ningún fichero sea largo.

// Arriba: cerrar, barra de progreso y «2 / 5».
@Composable
fun ExerciseTopBar(completed: Int, position: Int, total: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = "Cerrar la práctica")
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else completed.toFloat() / total },
            modifier = Modifier.weight(1f),
            color = EstadoDominado,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            drawStopIndicator = {},
        )
        Text(
            "$position / $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // TalkBack leería «2 barra 5»: mejor una frase.
            modifier = Modifier.padding(end = 12.dp).semantics { contentDescription = "Pregunta $position de $total" },
        )
    }
}

// Abajo: el resultado (tras comprobar) y el botón «Comprobar» / «Continuar».
@Composable
fun ExerciseBottomBar(
    exercise: ExerciseState,
    isLast: Boolean,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        exercise.correct?.let { correct -> ResultMessage(correct, exercise) }
        if (exercise.isChecked) {
            PrimaryButton(text = if (isLast) "Ver resultado" else "Continuar", onClick = onContinue)
        } else {
            PrimaryButton(text = "Comprobar", onClick = onCheck, enabled = exercise.canCheck)
        }
    }
}

// «¡Correcto!» o «No es correcto» + la solución. liveRegion: TalkBack lo lee al aparecer.
@Composable
private fun ResultMessage(correct: Boolean, exercise: ExerciseState) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (correct) {
            Text("¡Correcto!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            Text("No es correcto", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text(exercise.question.solution(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Al terminar: aciertos y volver al camino. También sirve para un concepto sin preguntas.
@Composable
fun PracticeSummary(conceptTitle: String, correct: Int, total: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            if (total == 0) conceptTitle else "Práctica terminada",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            if (total == 0) "Este concepto todavía no tiene preguntas." else summaryText(correct, total),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = "Volver al camino", onClick = onClose)
    }
}
