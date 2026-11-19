package es.ulpgc.pamn.dominio.course.exercise

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Una pregunta mientras se responde: elegir, cambiar de idea, comprobar.
class ExerciseStateTest {

    private val single = SingleChoice("q1", "bfs", "¿Qué usa BFS?", listOf("Pila", "Cola", "Montículo"), correct = 1)
    private val multiple = MultipleChoice("q2", "arboles", "¿Cuáles son árboles?", listOf("Binario", "Ciclo", "Montículo"), correct = setOf(0, 2))
    private val matching = Matching(
        "q3", "bfs", "Une",
        listOf(MatchPair("BFS", "Cola"), MatchPair("DFS", "Pila"), MatchPair("Dijkstra", "Montículo")),
    )

    private fun start(question: Question) = question.startExercise(Random(1))
    private fun ExerciseState.matching() = draft as MatchingDraft

    // ── Al empezar ──

    @Test
    fun `a new exercise has nothing chosen, cannot be checked and is not checked`() {
        listOf(single, multiple, matching).map(::start).forEach {
            assertFalse(it.canCheck, it.question.id)
            assertFalse(it.isChecked, it.question.id)
            assertNull(it.correct, it.question.id)
        }
    }

    @Test
    fun `each type of question starts with its own kind of draft`() {
        assertEquals(SingleDraft(), start(single).draft)
        assertEquals(MultipleDraft(), start(multiple).draft)
        assertTrue(start(matching).draft is MatchingDraft)
    }

    // ── Opción única ──

    @Test
    fun `choosing an option in single choice selects only that one`() {
        val state = start(single).choose(0).choose(2)
        assertEquals(SingleDraft(choice = 2), state.draft)
        assertTrue(state.canCheck)
    }

    // ── Selección múltiple ──

    @Test
    fun `choosing in multiple choice toggles the option`() {
        val state = start(multiple).choose(0).choose(2).choose(0)
        assertEquals(MultipleDraft(choices = setOf(2)), state.draft)
    }

    @Test
    fun `unchoosing everything in multiple choice cannot be checked`() {
        assertFalse(start(multiple).choose(1).choose(1).canCheck)
    }

    // ── Comprobar ──

    @Test
    fun `checking a right answer marks it as correct`() {
        val state = start(single).choose(1).check()
        assertTrue(state.isChecked)
        assertEquals(true, state.correct)
    }

    @Test
    fun `checking a wrong answer marks it as wrong`() {
        assertEquals(false, start(multiple).choose(0).check().correct)
    }

    @Test
    fun `it cannot be checked without answering`() {
        val state = start(single)
        assertEquals(state, state.check())
    }

    @Test
    fun `after checking, the answer cannot be changed`() {
        val checked = start(single).choose(1).check()
        assertEquals(checked, checked.choose(0))
        val checkedMatching = start(matching).tapLeft(0).tapRight(0).tapLeft(1).tapRight(1).tapLeft(2).tapRight(2).check()
        assertEquals(checkedMatching, checkedMatching.tapLeft(0))
    }

    // ── Emparejar: se toca uno de cada lado, en cualquier orden ──

    @Test
    fun `tapping a left item and then a right one joins them`() {
        val state = start(matching).tapLeft(0).tapRight(2)
        assertEquals(mapOf(0 to 2), state.matching().pairs)
        assertNull(state.matching().pendingLeft)
    }

    @Test
    fun `it also works starting from the right`() {
        assertEquals(mapOf(1 to 0), start(matching).tapRight(0).tapLeft(1).matching().pairs)
    }

    @Test
    fun `tapping the selected item again unselects it`() {
        val state = start(matching).tapLeft(0).tapLeft(0)
        assertNull(state.matching().pendingLeft)
        assertTrue(state.matching().pairs.isEmpty())
    }

    @Test
    fun `tapping another item of the same side changes the selection`() {
        assertEquals(2, start(matching).tapLeft(0).tapLeft(2).matching().pendingLeft)
    }

    @Test
    fun `tapping a joined item unjoins it`() {
        val joined = start(matching).tapLeft(0).tapRight(2)
        assertTrue(joined.tapLeft(0).matching().pairs.isEmpty())
        assertTrue(joined.tapRight(2).matching().pairs.isEmpty())
    }

    // Una vez desunidos, se pueden volver a unir de otra forma.
    @Test
    fun `an unjoined item can be joined again with another one`() {
        val state = start(matching).tapLeft(0).tapRight(2).tapLeft(0).tapLeft(0).tapRight(1)
        assertEquals(mapOf(0 to 1), state.matching().pairs)
    }

    @Test
    fun `matching can be checked only when every item is joined`() {
        val twoOfThree = start(matching).tapLeft(0).tapRight(0).tapLeft(1).tapRight(1)
        assertFalse(twoOfThree.canCheck)
        assertTrue(twoOfThree.tapLeft(2).tapRight(2).canCheck)
    }

    @Test
    fun `a right matching is correct`() {
        val state = start(matching).tapLeft(0).tapRight(0).tapLeft(1).tapRight(1).tapLeft(2).tapRight(2).check()
        assertEquals(true, state.correct)
    }

    // El orden de la derecha se decide al empezar y no cambia (sobrevive, por ejemplo, a girar la pantalla).
    @Test
    fun `the right column order is fixed when the exercise starts`() {
        val state = start(matching)
        assertEquals(matching.rightOrder(Random(1)), state.matching().rightOrder)
        assertEquals(state.matching().rightOrder, state.tapLeft(0).tapRight(1).matching().rightOrder)
    }

    // ── Marcas de cada opción (opción única y selección múltiple) ──

    @Test
    fun `an option knows if it is selected`() {
        val state = start(multiple).choose(0)
        assertTrue(state.isSelected(0))
        assertFalse(state.isSelected(1))
        assertTrue(start(single).choose(2).isSelected(2))
    }

    @Test
    fun `before checking no option has a mark`() {
        val state = start(single).choose(0)
        assertEquals(listOf(OptionMark.NONE, OptionMark.NONE, OptionMark.NONE), (0..2).map(state::markFor))
    }

    // Opción única fallada: la elegida en rojo y la buena en morado, para enseñar la respuesta.
    @Test
    fun `after a wrong single choice, the chosen one is wrong and the right one is shown`() {
        val state = start(single).choose(0).check()
        assertEquals(listOf(OptionMark.WRONG, OptionMark.CORRECT, OptionMark.NONE), (0..2).map(state::markFor))
    }

    // Buenas: 0 y 2. Se marcan 0 y 1: la 0 bien, la 1 mal, la 2 faltaba (se enseña como buena).
    @Test
    fun `after checking a multiple choice, each option shows what it was`() {
        val state = start(multiple).choose(0).choose(1).check()
        assertEquals(listOf(OptionMark.CORRECT, OptionMark.WRONG, OptionMark.CORRECT), (0..2).map(state::markFor))
    }

    // Las acciones de un tipo no hacen nada en otro.
    @Test
    fun `actions of another type of question do nothing`() {
        val single = start(single)
        assertEquals(single, single.tapLeft(0))
        val matching = start(matching)
        assertEquals(matching, matching.choose(0))
    }
}
