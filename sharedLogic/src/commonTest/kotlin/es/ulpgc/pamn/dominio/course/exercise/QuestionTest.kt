package es.ulpgc.pamn.dominio.course.exercise

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Cómo se corrige cada tipo de pregunta (especificación §7).
class QuestionTest {

    private val single = SingleChoice(
        id = "q1", conceptId = "bfs", prompt = "¿Qué estructura usa BFS?",
        options = listOf("Pila", "Cola", "Montículo"), correct = 1,
    )

    private val multiple = MultipleChoice(
        id = "q2", conceptId = "arboles", prompt = "¿Cuáles son árboles?",
        options = listOf("Árbol binario", "Grafo con ciclos", "Montículo"), correct = setOf(0, 2),
    )

    private val matching = Matching(
        id = "q3", conceptId = "bfs", prompt = "Une cada recorrido con lo que usa",
        pairs = listOf(MatchPair("BFS", "Cola"), MatchPair("DFS", "Pila"), MatchPair("Dijkstra", "Montículo")),
    )

    // ── Opción única ──

    @Test
    fun `single choice is right with the correct option`() {
        assertTrue(single.isCorrect(Answer.Single(1)))
    }

    @Test
    fun `single choice is wrong with another option`() {
        assertFalse(single.isCorrect(Answer.Single(0)))
    }

    // ── Selección múltiple: sin puntos parciales ──

    @Test
    fun `multiple choice is right with exactly the correct options`() {
        assertTrue(multiple.isCorrect(Answer.Multiple(setOf(2, 0))))
    }

    @Test
    fun `multiple choice is wrong if one correct option is missing`() {
        assertFalse(multiple.isCorrect(Answer.Multiple(setOf(0))))
    }

    @Test
    fun `multiple choice is wrong if a wrong option is added`() {
        assertFalse(multiple.isCorrect(Answer.Multiple(setOf(0, 1, 2))))
    }

    @Test
    fun `multiple choice is wrong with nothing chosen`() {
        assertFalse(multiple.isCorrect(Answer.Multiple(emptySet())))
    }

    // ── Emparejar: cada elemento de la izquierda (índice) con uno de la derecha (índice) ──

    @Test
    fun `matching is right when every pair is right`() {
        assertTrue(matching.isCorrect(Answer.Matches(mapOf(0 to 0, 1 to 1, 2 to 2))))
    }

    @Test
    fun `matching is wrong when two pairs are swapped`() {
        assertFalse(matching.isCorrect(Answer.Matches(mapOf(0 to 1, 1 to 0, 2 to 2))))
    }

    @Test
    fun `an incomplete matching is never right`() {
        assertFalse(matching.isCorrect(Answer.Matches(mapOf(0 to 0, 1 to 1))))
    }

    // Si dos elementos de la derecha tienen el mismo texto, da igual cuál de los dos se elija.
    @Test
    fun `matching compares the texts, so repeated texts are interchangeable`() {
        val repeated = Matching(
            "q4", "bfs", "Une",
            listOf(MatchPair("BFS", "Cola"), MatchPair("Cola de prioridad", "Cola"), MatchPair("DFS", "Pila")),
        )
        assertTrue(repeated.isCorrect(Answer.Matches(mapOf(0 to 1, 1 to 0, 2 to 2))))
    }

    // Para pintar cada flecha de su color al comprobar.
    @Test
    fun `each pair of a matching can be checked on its own`() {
        assertTrue(matching.isRightFor(left = 0, right = 0))
        assertFalse(matching.isRightFor(left = 0, right = 1))
        assertFalse(matching.isRightFor(left = 0, right = 9))
    }

    // ── Una respuesta de otro tipo nunca es correcta ──

    @Test
    fun `an answer of another type is wrong`() {
        assertFalse(single.isCorrect(Answer.Multiple(setOf(1))))
        assertFalse(multiple.isCorrect(Answer.Single(0)))
        assertFalse(matching.isCorrect(Answer.Single(0)))
    }

    // ── La columna derecha se baraja ──

    @Test
    fun `the right column is shuffled with every item once`() {
        assertEquals(listOf(0, 1, 2), matching.rightOrder(Random(7)).sorted())
    }

    @Test
    fun `the same seed gives the same order`() {
        assertEquals(matching.rightOrder(Random(42)), matching.rightOrder(Random(42)))
    }

    // Si quedara en el mismo orden que la izquierda, la respuesta sería trivial.
    @Test
    fun `the right column is never in the same order as the left one`() {
        (0 until 50).forEach { seed -> assertNotEquals(listOf(0, 1, 2), matching.rightOrder(Random(seed)), "semilla $seed") }
    }

    // ── Preguntas mal hechas: fallan al crearlas, no al corregirlas ──

    @Test
    fun `a question with an impossible correct option cannot be created`() {
        assertFailsWith<IllegalArgumentException> { single.copy(correct = 3) }
        assertFailsWith<IllegalArgumentException> { multiple.copy(correct = setOf(0, 5)) }
    }

    @Test
    fun `a multiple choice needs at least one correct option`() {
        assertFailsWith<IllegalArgumentException> { multiple.copy(correct = emptySet()) }
    }

    @Test
    fun `a question needs at least two options or pairs`() {
        assertFailsWith<IllegalArgumentException> { single.copy(options = listOf("Única"), correct = 0) }
        assertFailsWith<IllegalArgumentException> { matching.copy(pairs = listOf(MatchPair("A", "B"))) }
    }
}
