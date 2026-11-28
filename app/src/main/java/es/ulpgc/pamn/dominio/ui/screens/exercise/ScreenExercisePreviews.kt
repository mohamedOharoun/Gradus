package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.study.PhotoStep
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission
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

// El paso con foto en cada estado (docs/tareas/ejercicio-foto.md, paso 5c). La foto de las previews no
// existe: se ve el recuadro gris de NotePhoto.
private val photoCourse = Course(
    "foto", "Con foto",
    listOf(
        Module(
            "grafos", "Grafos",
            steps = listOf(
                Step.Photo(
                    "foto",
                    "Recorre en anchura el grafo de la lección empezando por **A**. Escribe el orden de visita.",
                    "Orden: A, B, C, D, E. La cola pasa por [A] → [B, C] → [C, D] → [D, E] → [E].",
                ),
            ),
        ),
    ),
)

private fun photoStep(change: (PhotoStep) -> PhotoStep = { it }) =
    ModuleStudy.start(photoCourse, "grafos", random)!!.updatePhoto("foto", change)

private fun PhotoStep.sending() = take("foto.jpg").send()

@Composable
private fun PhotoPreview(study: ModuleStudy, ui: PhotoStepUi = PhotoStepUi()) {
    GradusTheme { ScreenExercise(study, ExerciseActions(), photo = ui) }
}

@LightDarkPreviews
@Composable
private fun PhotoEmptyPreview() = PhotoPreview(photoStep())

@LightDarkPreviews
@Composable
private fun PhotoPreparingPreview() = PhotoPreview(photoStep(), PhotoStepUi(notice = PhotoNotice.PREPARING))

@LightDarkPreviews
@Composable
private fun PhotoReadyPreview() = PhotoPreview(photoStep { it.take("foto.jpg") })

@LightDarkPreviews
@Composable
private fun PhotoSendingPreview() = PhotoPreview(photoStep { it.sending() })

@LightDarkPreviews
@Composable
private fun PhotoOfflinePreview() = PhotoPreview(photoStep { it.sending().finished(SubmitOutcome.Unreachable) })

@LightDarkPreviews
@Composable
private fun PhotoRejectedPreview() =
    PhotoPreview(photoStep { it.sending().finished(SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG)) })

@LightDarkPreviews
@Composable
private fun PhotoSubmittedPreview() =
    PhotoPreview(photoStep { it.sending().finished(SubmitOutcome.Submitted(Submission("s1", "2026-11-25T10:00:00Z"))) })

// Decisión Z: el curso de ejemplo no está en el servidor.
@LightDarkPreviews
@Composable
private fun PhotoNotPublishedPreview() = PhotoPreview(photoStep(), PhotoStepUi(canSubmit = false))
