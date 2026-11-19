package es.ulpgc.pamn.dominio.course.exercise

import kotlin.random.Random

// Una pregunta de un concepto (especificación §7). Cada tipo sabe corregirse (patrón Strategy):
// quien corrige no necesita saber de qué tipo es. Al ser sealed, un `when` sobre Question obliga
// a cubrir los tres tipos, y añadir uno nuevo avisa en todos los sitios que lo necesitan.
// Las preguntas mal hechas fallan al crearlas (require), no a mitad de una práctica.
sealed interface Question {
    val id: String
    val conceptId: String
    val prompt: String

    fun isCorrect(answer: Answer): Boolean
}

// Test de una sola respuesta. `correct` es el índice de la opción buena.
data class SingleChoice(
    override val id: String,
    override val conceptId: String,
    override val prompt: String,
    val options: List<String>,
    val correct: Int,
) : Question {
    init {
        require(options.size >= 2) { "La pregunta «$id» necesita al menos dos opciones." }
        require(correct in options.indices) { "La respuesta correcta de «$id» no es ninguna de sus opciones." }
    }

    override fun isCorrect(answer: Answer) = answer is Answer.Single && answer.choice == correct
}

// Varias respuestas correctas. Solo cuenta si se eligen exactamente esas (sin puntos parciales).
data class MultipleChoice(
    override val id: String,
    override val conceptId: String,
    override val prompt: String,
    val options: List<String>,
    val correct: Set<Int>,
) : Question {
    init {
        require(options.size >= 2) { "La pregunta «$id» necesita al menos dos opciones." }
        require(correct.isNotEmpty()) { "La pregunta «$id» necesita al menos una respuesta correcta." }
        require(correct.all { it in options.indices }) { "Alguna respuesta correcta de «$id» no es una de sus opciones." }
    }

    override fun isCorrect(answer: Answer) = answer is Answer.Multiple && answer.choices == correct
}

data class MatchPair(val left: String, val right: String)

// Unir cada elemento de la izquierda con el suyo de la derecha. Cada par de `pairs` es una pareja
// correcta; la interfaz enseña la columna derecha barajada (`rightOrder`).
data class Matching(
    override val id: String,
    override val conceptId: String,
    override val prompt: String,
    val pairs: List<MatchPair>,
) : Question {
    init {
        require(pairs.size >= 2) { "La pregunta «$id» necesita al menos dos parejas." }
    }

    // Correcta si están todas las parejas y cada una une textos que van juntos. Se comparan los
    // textos y no los índices: si dos elementos de la derecha dicen lo mismo, valen los dos.
    override fun isCorrect(answer: Answer): Boolean =
        answer is Answer.Matches &&
            answer.pairs.keys == pairs.indices.toSet() &&
            answer.pairs.all { (left, right) -> isRightFor(left, right) }

    // ¿Va bien este elemento de la izquierda con este de la derecha? (Una sola pareja.)
    fun isRightFor(left: Int, right: Int): Boolean =
        pairs.getOrNull(right)?.right == pairs.getOrNull(left)?.right && left in pairs.indices

    // Orden en que se enseña la columna derecha: barajado y nunca igual que el de la izquierda
    // (sería regalar la respuesta). Recibe el Random para que los tests puedan fijar la semilla.
    fun rightOrder(random: Random): List<Int> {
        val shuffled = pairs.indices.shuffled(random)
        return if (shuffled == pairs.indices.toList()) shuffled.drop(1) + shuffled.first() else shuffled
    }
}
