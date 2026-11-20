package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MatchingDraft
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.PracticeSession
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.exercise.isSelected
import es.ulpgc.pamn.dominio.course.exercise.markFor
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.ui.components.AnswerOption
import es.ulpgc.pamn.dominio.ui.components.AnswerOptionKind
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.MatchingBoard
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import kotlin.random.Random

// Lo que la pantalla avisa hacia fuera (objeto de parámetros, como RegisterActions).
data class ExerciseActions(
    val onChoose: (option: Int) -> Unit = {},
    val onLeftClick: (index: Int) -> Unit = {},
    val onRightClick: (index: Int) -> Unit = {},
    val onCheck: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onClose: () -> Unit = {},
)

// La práctica de un módulo: una pregunta cada vez y, al acabar, el resumen.
@Composable
fun ScreenExercise(state: ExerciseUiState, actions: ExerciseActions, modifier: Modifier = Modifier) {
    val session = state.session
    val exercise = session.current
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ExerciseTopBar(session.completedCount, session.position, session.total, actions.onClose) },
        bottomBar = {
            if (exercise != null) {
                ExerciseBottomBar(exercise, isLast = session.position == session.total, actions.onCheck, actions.onContinue)
            }
        },
    ) { innerPadding ->
        val content = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        if (exercise == null) {
            PracticeSummary(state.moduleTitle, session.correctCount, session.total, actions.onClose, content)
        } else {
            Column(
                modifier = content.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // En horizontal o en tablet no se estira: como mucho 560 dp, centrado.
                QuestionContent(state.moduleTitle, exercise, actions, Modifier.widthIn(max = 560.dp).padding(20.dp))
            }
        }
    }
}

@Composable
private fun QuestionContent(moduleTitle: String, exercise: ExerciseState, actions: ExerciseActions, modifier: Modifier = Modifier) {
    val question = exercise.question
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(moduleTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(question.prompt, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(question.hint(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (question) {
            is SingleChoice -> Options(question.options, AnswerOptionKind.SINGLE, exercise, actions.onChoose)
            is MultipleChoice -> Options(question.options, AnswerOptionKind.MULTIPLE, exercise, actions.onChoose)
            is Matching -> MatchingBoard(
                question = question,
                draft = exercise.draft as MatchingDraft,
                checked = exercise.isChecked,
                onLeftClick = actions.onLeftClick,
                onRightClick = actions.onRightClick,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Options(options: List<String>, kind: AnswerOptionKind, exercise: ExerciseState, onChoose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, text ->
            AnswerOption(
                text = text,
                kind = kind,
                selected = exercise.isSelected(index),
                onClick = { onChoose(index) },
                mark = exercise.markFor(index),
                enabled = !exercise.isChecked,
            )
        }
    }
}

// ── Previews: con las preguntas del curso de ejemplo ──

private fun previewState(moduleId: String, change: (PracticeSession) -> PracticeSession = { it }) = ExerciseUiState(
    moduleTitle = demoCourse.module(moduleId)?.title.orEmpty(),
    session = change(PracticeSession.start(demoCourse.questionsFor(moduleId), Random(3))),
)

@LightDarkPreviews
@Composable
private fun ScreenExerciseSinglePreview() {
    GradusTheme { ScreenExercise(previewState("grafos") { s -> s.update { it.choose(1) } }, ExerciseActions()) }
}

@LightDarkPreviews
@Composable
private fun ScreenExerciseWrongPreview() {
    GradusTheme { ScreenExercise(previewState("grafos") { s -> s.update { it.choose(1).check() } }, ExerciseActions()) }
}

@LightDarkPreviews
@Composable
private fun ScreenExerciseMatchingPreview() {
    GradusTheme { ScreenExercise(previewState("hash") { s -> s.update { it.choose(0).check() }.next().update { it.tapLeft(0) } }, ExerciseActions()) }
}

@LightDarkPreviews
@Composable
private fun ScreenExerciseSummaryPreview() {
    GradusTheme {
        val finished = previewState("conjuntos") { s -> s.update { it.choose(0).check() }.next() }
        ScreenExercise(finished, ExerciseActions())
    }
}
