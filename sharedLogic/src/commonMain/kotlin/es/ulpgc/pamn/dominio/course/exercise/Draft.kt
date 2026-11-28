package es.ulpgc.pamn.dominio.course.exercise

// La respuesta a medias de una pregunta, mientras el estudiante elige. Un tipo por cada tipo de
// pregunta. `answer` es la respuesta lista para corregir, o null si todavía está incompleta.
sealed interface Draft {
    val answer: Answer?
}

data class SingleDraft(val choice: Int? = null) : Draft {
    override val answer: Answer? get() = choice?.let { Answer.Single(it) }
}

data class MultipleDraft(val choices: Set<Int> = emptySet()) : Draft {
    override val answer: Answer? get() = if (choices.isEmpty()) null else Answer.Multiple(choices)

    fun toggle(index: Int) = copy(choices = if (index in choices) choices - index else choices + index)
}

// Emparejar. `rightOrder`: cómo se enseña la columna derecha (se baraja al empezar y ya no cambia).
// `pairs`: izquierda → derecha ya unidas. `pendingLeft`/`pendingRight`: el elemento tocado que
// espera a su pareja del otro lado (como mucho uno de los dos).
data class MatchingDraft(
    val rightOrder: List<Int>,
    val pairs: Map<Int, Int> = emptyMap(),
    val pendingLeft: Int? = null,
    val pendingRight: Int? = null,
) : Draft {
    override val answer: Answer? get() = if (pairs.size == rightOrder.size) Answer.Matches(pairs) else null

    // Tocar a la izquierda: si ya estaba unido, se desune; si hay uno de la derecha esperando, se
    // unen; si era el que esperaba, se suelta; si no, pasa a ser el que espera.
    fun tapLeft(left: Int): MatchingDraft = when {
        left in pairs -> copy(pairs = pairs - left).released()
        pendingRight != null -> join(left, pendingRight)
        pendingLeft == left -> released()
        else -> copy(pendingLeft = left, pendingRight = null)
    }

    // Lo mismo, tocando a la derecha.
    fun tapRight(right: Int): MatchingDraft = when {
        right in pairs.values -> copy(pairs = pairs.filterValues { it != right }).released()
        pendingLeft != null -> join(pendingLeft, right)
        pendingRight == right -> released()
        else -> copy(pendingRight = right, pendingLeft = null)
    }

    private fun join(left: Int, right: Int) = copy(pairs = pairs + (left to right)).released()

    private fun released() = copy(pendingLeft = null, pendingRight = null)
}
