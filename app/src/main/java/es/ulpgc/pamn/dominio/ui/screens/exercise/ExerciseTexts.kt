package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.Question
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.mastery.MasteryResult
import es.ulpgc.pamn.dominio.course.mastery.MasteryTest
import es.ulpgc.pamn.dominio.course.study.StudyStage
import es.ulpgc.pamn.dominio.ui.components.joinedWithAnd
import es.ulpgc.pamn.dominio.ui.text.UiText

// Textos de la práctica (UiText, docs/tareas/ajustes.md, fase 3, paso 8b). Lo que escribe el autor
// (opciones, parejas, el título del módulo) va como dato y no se traduce (decisión R).

// Qué hay que hacer, debajo del enunciado.
fun Question.hint(): UiText = when (this) {
    is SingleChoice -> UiText.of(R.string.exercise_hint_single)
    is MultipleChoice -> UiText.of(R.string.exercise_hint_multiple)
    is Matching -> UiText.of(R.string.exercise_hint_matching)
}

// La solución, para enseñarla cuando se falla. Cada idioma pone sus comillas («» o “”).
fun Question.solution(): UiText = when (this) {
    is SingleChoice -> UiText.of(R.string.exercise_solution_single, options[correct])
    is MultipleChoice -> {
        val right = correct.sorted().map { options[it] }
        if (right.size == 1) UiText.of(R.string.exercise_solution_multiple_one, right.single())
        else UiText.of(R.string.exercise_solution_multiple, joinedWithAnd(right.map { UiText.of(R.string.quoted, it) }))
    }
    is Matching -> UiText.of(R.string.exercise_solution_pairs, pairs.joinToString(", ") { "${it.left} → ${it.right}" })
}

// Antes de la prueba de dominio: «Responde 3 preguntas sobre «Grafos». …».
fun testIntroText(moduleTitle: String, test: MasteryTest): UiText {
    val questions = UiText.plural(R.plurals.test_questions, test.questions.size)
    return if (test.minCorrect == test.questions.size) UiText.of(R.string.test_intro_all, questions, moduleTitle)
    else UiText.of(R.string.test_intro_min, questions, moduleTitle, test.minCorrect)
}

// Al terminar la prueba de dominio.
// `when (val r = result)`: copia local para que Kotlin sepa que no cambia (result viene de otro módulo).
fun StudyStage.Finished.title(): UiText = when (val r = result) {
    MasteryResult.Passed -> UiText.of(R.string.test_passed_title)
    is MasteryResult.Failed -> UiText.plural(R.plurals.test_failed_title, r.missing)
}

fun StudyStage.Finished.body(moduleTitle: String): UiText = when (result) {
    MasteryResult.Passed -> UiText.of(R.string.test_passed_body, correctCount, total, moduleTitle)
    is MasteryResult.Failed -> UiText.of(R.string.test_failed_body, correctCount, total)
}
