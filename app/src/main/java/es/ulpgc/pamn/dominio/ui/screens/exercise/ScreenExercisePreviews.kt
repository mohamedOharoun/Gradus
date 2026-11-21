package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.study.ModuleStudy
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import kotlin.random.Random

// Previews de ScreenExercise, una por etapa, con el módulo «grafos» del curso de ejemplo:
// lección, 3 ejercicios y una prueba de 3 preguntas en la que hay que acertar 2.

private val random = Random(3)

private fun grafos(change: ModuleStudy.() -> ModuleStudy = { this }) =
    ModuleStudy.start(demoCourse, "grafos", random)!!.change()

// Responde bien (opción 0) o mal (opción 1) y sigue; en emparejar, une cada uno con el suyo.
private fun ModuleStudy.answered(right: Boolean = true) = update {
    if (it.question is Matching) {
        (0..2).fold(it) { state, i -> state.tapLeft(i).tapRight(i) }.check()
    } else {
        it.choose(if (right) 0 else 1).check()
    }
}.next(random)

private fun ModuleStudy.atTestIntro() = next(random).answered().answered().answered()

@Composable
private fun Preview(study: ModuleStudy) {
    GradusTheme { ScreenExercise(study, ExerciseActions()) }
}

@LightDarkPreviews
@Composable
private fun LessonPreview() = Preview(grafos())

@LightDarkPreviews
@Composable
private fun ExplanationPreview() = Preview(grafos { next(random).update { it.choose(1).check() } })

@LightDarkPreviews
@Composable
private fun TestIntroPreview() = Preview(grafos { atTestIntro() })

@LightDarkPreviews
@Composable
private fun TestQuestionPreview() = Preview(grafos { atTestIntro().startTest(random).update { it.choose(0) } })

@LightDarkPreviews
@Composable
private fun PassedPreview() = Preview(grafos { atTestIntro().startTest(random).answered().answered().answered() })

@LightDarkPreviews
@Composable
private fun FailedPreview() =
    Preview(grafos { atTestIntro().startTest(random).answered(right = false).answered(right = false).answered(right = false) })
