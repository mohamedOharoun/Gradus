package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.Question
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.mastery.MasteryResult
import es.ulpgc.pamn.dominio.course.mastery.MasteryTest
import es.ulpgc.pamn.dominio.course.study.StudyStage
import es.ulpgc.pamn.dominio.ui.components.joinedWithAnd

// Textos de la práctica (en el código hasta que pasemos a strings.xml).

// Qué hay que hacer, debajo del enunciado.
fun Question.hint(): String = when (this) {
    is SingleChoice -> "Elige una respuesta."
    is MultipleChoice -> "Puede haber varias correctas: márcalas todas."
    is Matching -> "Toca un elemento de cada columna para unirlos. Toca uno unido para soltarlo."
}

// La solución, para enseñarla cuando se falla.
fun Question.solution(): String = when (this) {
    is SingleChoice -> "La respuesta era «${options[correct]}»."
    is MultipleChoice -> {
        val right = correct.sorted().map { "«${options[it]}»" }
        if (right.size == 1) "La correcta era ${right.single()}." else "Las correctas eran ${right.joinedWithAnd()}."
    }
    is Matching -> "Las parejas eran: " + pairs.joinToString(", ") { "${it.left} → ${it.right}" } + "."
}

// «1 pregunta», «4 preguntas».
private fun questions(count: Int) = if (count == 1) "1 pregunta" else "$count preguntas"

// Antes de la prueba de dominio.
fun testIntroText(moduleTitle: String, test: MasteryTest): String =
    "Responde ${questions(test.questions.size)} sobre «$moduleTitle». " +
        if (test.minCorrect == test.questions.size) "Para dominarlo, tienes que acertarlas todas."
        //   (espacio que no parte la línea): el número no se queda solo en otra línea.
        else "Para dominarlo, tienes que acertar al menos ${test.minCorrect}."

// Al terminar la prueba de dominio.
// `when (val r = result)`: copia local para que Kotlin sepa que no cambia (result viene de otro módulo).
fun StudyStage.Finished.title(): String = when (val r = result) {
    MasteryResult.Passed -> "¡Módulo dominado!"
    is MasteryResult.Failed -> if (r.missing == 1) "Te faltó 1 para aprobar" else "Te faltaron ${r.missing} para aprobar"
}

fun StudyStage.Finished.body(moduleTitle: String): String = when (result) {
    MasteryResult.Passed -> "Has acertado $correctCount de $total. Ya dominas «$moduleTitle» y se desbloquea lo que dependía de él."
    is MasteryResult.Failed -> "Has acertado $correctCount de $total. Repasa las explicaciones y vuelve a intentarlo: saldrán otras preguntas."
}
