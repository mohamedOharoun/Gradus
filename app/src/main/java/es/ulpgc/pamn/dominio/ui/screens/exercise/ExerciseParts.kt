package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.ui.components.MarkdownText
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado

// Piezas de ScreenExercise, separadas para que ningún fichero sea largo.

// Arriba: cerrar, barra de progreso y «2 / 5». `itemOf`: cómo lo lee TalkBack («Paso 2 de 5» o «Pregunta 2 de 5»).
@Composable
fun ExerciseTopBar(
    completed: Int,
    position: Int,
    total: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes itemOf: Int = R.string.exercise_question_of,
) {
    val spoken = stringResource(itemOf, position, total)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 48 dp: en esta versión de Material 3, IconButton mide 40 dp y no llega a la zona táctil mínima.
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.exercise_close))
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
            modifier = Modifier.padding(end = 12.dp).semantics { contentDescription = spoken },
        )
    }
}

// Arriba, cuando no hay nada que contar (entrada a la prueba, resultado): solo cerrar.
@Composable
fun CloseBar(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // 48 dp: en esta versión de Material 3, IconButton mide 40 dp y no llega a la zona táctil mínima.
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.exercise_close))
        }
    }
}

// Abajo: el resultado y la explicación (tras comprobar) y el botón «Comprobar» / `continueText`.
@Composable
fun ExerciseBottomBar(
    exercise: ExerciseState,
    continueText: String,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomPanel(modifier) {
        exercise.correct?.let { correct -> ResultMessage(correct, exercise) }
        if (exercise.isChecked) {
            PrimaryButton(text = continueText, onClick = onContinue)
        } else {
            PrimaryButton(text = stringResource(R.string.exercise_check), onClick = onCheck, enabled = exercise.canCheck)
        }
    }
}

// Abajo, en una lección: solo «Continuar».
@Composable
fun ContinueBar(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    BottomPanel(modifier) { PrimaryButton(text = stringResource(R.string.action_continue), onClick = onContinue) }
}

// El panel de abajo, compartido con el paso con foto (PhotoStepBottomBar).
@Composable
internal fun BottomPanel(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// «¡Correcto!» o «No es correcto» + la solución y la explicación del autor. liveRegion: TalkBack
// lo lee al aparecer. Con una explicación larga, el panel se desplaza en vez de tapar la pregunta.
@Composable
private fun ResultMessage(correct: Boolean, exercise: ExerciseState) {
    Column(
        modifier = Modifier
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState())
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (correct) {
            Text(stringResource(R.string.exercise_correct), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            Text(stringResource(R.string.exercise_wrong), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text(exercise.question.solution().asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (exercise.question.explanation.isNotBlank()) {
            MarkdownText(
                exercise.question.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// Una pantalla de cierre o de paso: título, texto y uno o dos botones. Sirve para la entrada a la
// prueba de dominio, su resultado y el final de un módulo sin prueba.
@Composable
fun StudyMessage(
    title: String,
    body: String,
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite },
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(text = primaryText, onClick = onPrimary)
            secondaryText?.let {
                TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(it) }
            }
        }
    }
}
