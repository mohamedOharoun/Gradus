package es.ulpgc.pamn.dominio.course.exercise

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Una práctica: las preguntas de un módulo, una detrás de otra, hasta el resumen.
class PracticeSessionTest {

    private val q1 = SingleChoice("q1", "¿Qué usa BFS?", listOf("Pila", "Cola"), correct = 1)
    private val q2 = MultipleChoice("q2", "¿Cuáles?", listOf("A", "B", "C"), correct = setOf(0, 2))
    private val q3 = SingleChoice("q3", "¿Y DFS?", listOf("Pila", "Cola"), correct = 0)

    private fun start() = PracticeSession.start(listOf(q1, q2, q3), Random(1))

    @Test
    fun `a new practice starts at the first question with nothing done`() {
        val session = start()
        assertEquals(q1, session.current?.question)
        assertEquals(1, session.position)
        assertEquals(3, session.total)
        assertEquals(0, session.completedCount)
        assertEquals(0, session.correctCount)
        assertFalse(session.isFinished)
    }

    @Test
    fun `answering changes the current exercise`() {
        val session = start().update { it.choose(1) }
        assertEquals(SingleDraft(choice = 1), session.current?.draft)
    }

    @Test
    fun `it cannot move on before checking`() {
        val session = start().update { it.choose(1) }
        assertFalse(session.canContinue)
        assertEquals(session, session.next())
    }

    @Test
    fun `after checking it moves on to the next question`() {
        val session = start().update { it.choose(1).check() }
        assertTrue(session.canContinue)
        assertEquals(1, session.completedCount)
        val next = session.next()
        assertEquals(q2, next.current?.question)
        assertEquals(2, next.position)
    }

    @Test
    fun `after the last question the practice is finished, with its result`() {
        val finished = start()
            .update { it.choose(1).check() }.next() // bien
            .update { it.choose(0).check() }.next() // mal: falta la C
            .update { it.choose(0).check() }.next() // bien
        assertTrue(finished.isFinished)
        assertNull(finished.current)
        assertEquals(3, finished.completedCount)
        assertEquals(2, finished.correctCount)
    }

    @Test
    fun `a finished practice does not change`() {
        val finished = start()
            .update { it.choose(1).check() }.next()
            .update { it.choose(0).check() }.next()
            .update { it.choose(0).check() }.next()
        assertEquals(finished, finished.next())
        assertEquals(finished, finished.update { it.choose(1) })
    }

    @Test
    fun `a practice without questions is finished from the start`() {
        val empty = PracticeSession.start(emptyList(), Random(1))
        assertTrue(empty.isFinished)
        assertEquals(0, empty.total)
    }
}
