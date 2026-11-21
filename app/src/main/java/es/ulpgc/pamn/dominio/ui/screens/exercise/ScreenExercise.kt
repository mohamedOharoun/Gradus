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
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MatchingDraft
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.exercise.isSelected
import es.ulpgc.pamn.dominio.course.exercise.markFor
import es.ulpgc.pamn.dominio.course.study.ModuleStudy
import es.ulpgc.pamn.dominio.course.study.StepState
import es.ulpgc.pamn.dominio.course.study.StudyStage
import es.ulpgc.pamn.dominio.ui.components.AnswerOption
import es.ulpgc.pamn.dominio.ui.components.AnswerOptionKind
import es.ulpgc.pamn.dominio.ui.components.MarkdownText
import es.ulpgc.pamn.dominio.ui.components.MatchingBoard

// Lo que la pantalla avisa hacia fuera (objeto de parámetros, como RegisterActions).
data class ExerciseActions(
    val onChoose: (option: Int) -> Unit = {},
    val onLeftClick: (index: Int) -> Unit = {},
    val onRightClick: (index: Int) -> Unit = {},
    val onCheck: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onStartTest: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onClose: () -> Unit = {},
)

// Estudiar un módulo: sus lecciones y ejercicios, la prueba de dominio y su resultado.
// Sin estado: pinta la etapa de ModuleStudy en la que se esté. Las previews están en ScreenExercisePreviews.kt.
@Composable
fun ScreenExercise(study: ModuleStudy, actions: ExerciseActions, modifier: Modifier = Modifier) {
    val title = study.module.title
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StudyTopBar(study.stage, actions.onClose) },
        bottomBar = { StudyBottomBar(study, actions) },
    ) { innerPadding ->
        val content = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        when (val stage = study.stage) {
            is StudyStage.Learning -> when (val step = stage.steps.current) {
                is StepState.Reading -> Centered(content) { LessonContent(title, step.lesson) }
                is StepState.Answering -> Centered(content) { QuestionContent(title, step.exercise, actions) }
                null -> Unit
            }
            is StudyStage.TestIntro -> StudyMessage(
                title = "Prueba de dominio",
                body = testIntroText(title, stage.test),
                primaryText = "Empezar la prueba",
                onPrimary = actions.onStartTest,
                modifier = content,
                secondaryText = "Ahora no",
                onSecondary = actions.onClose,
            )
            is StudyStage.Testing -> stage.practice.current?.let { exercise ->
                Centered(content) { QuestionContent("Prueba de dominio · $title", exercise, actions) }
            }
            is StudyStage.Finished -> StudyMessage(
                title = stage.title(),
                body = stage.body(title),
                primaryText = if (study.isPassed) "Volver al camino" else "Repetir la prueba",
                onPrimary = if (study.isPassed) actions.onClose else actions.onRetry,
                modifier = content,
                secondaryText = if (study.isPassed) null else "Volver al camino",
                onSecondary = actions.onClose,
            )
            StudyStage.Done -> StudyMessage(
                title = "Has terminado «$title»",
                body = "Este módulo todavía no tiene prueba de dominio.",
                primaryText = "Volver al camino",
                onPrimary = actions.onClose,
                modifier = content,
            )
        }
    }
}

// Arriba: progreso de los pasos o de las preguntas de la prueba; en las demás etapas, sin barra.
@Composable
private fun StudyTopBar(stage: StudyStage, onClose: () -> Unit) {
    when (stage) {
        is StudyStage.Learning ->
            ExerciseTopBar(stage.steps.index, stage.steps.position, stage.steps.total, onClose, itemLabel = "Paso")
        is StudyStage.Testing ->
            ExerciseTopBar(stage.practice.completedCount, stage.practice.position, stage.practice.total, onClose)
        else -> CloseBar(onClose)
    }
}

// Abajo: «Continuar» en una lección; «Comprobar» y la explicación en un ejercicio.
@Composable
private fun StudyBottomBar(study: ModuleStudy, actions: ExerciseActions) {
    val stage = study.stage
    val exercise = study.currentExercise
    when {
        stage is StudyStage.Learning && stage.steps.current is StepState.Reading -> ContinueBar(actions.onContinue)
        exercise != null -> {
            val isLastTestQuestion = stage is StudyStage.Testing && stage.practice.position == stage.practice.total
            ExerciseBottomBar(exercise, if (isLastTestQuestion) "Ver resultado" else "Continuar", actions.onCheck, actions.onContinue)
        }
    }
}

// Desplazable y, en horizontal o en tablet, como mucho 560 dp de ancho, centrado.
@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 560.dp).padding(20.dp)) { content() }
    }
}

@Composable
private fun LessonContent(moduleTitle: String, lesson: Step.Lesson) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Encabezado: con TalkBack se puede saltar directamente al principio de cada lección.
        Text(
            moduleTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        MarkdownText(lesson.text)
    }
}

@Composable
private fun QuestionContent(label: String, exercise: ExerciseState, actions: ExerciseActions) {
    val question = exercise.question
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
