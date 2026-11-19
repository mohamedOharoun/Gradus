package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.Question
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
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

fun summaryText(correct: Int, total: Int): String = when (correct) {
    total -> "¡Todas bien! Has acertado las $total."
    0 -> "Esta vez no has acertado ninguna de las $total. Repasa y vuelve a intentarlo."
    else -> "Has acertado $correct de $total."
}
