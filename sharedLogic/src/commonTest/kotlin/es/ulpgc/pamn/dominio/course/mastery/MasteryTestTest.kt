package es.ulpgc.pamn.dominio.course.mastery

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.exercise.PracticeSession
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// La prueba de dominio de un módulo: se sortean `questions` ejercicios de su reserva y se aprueba
// acertando al menos `minCorrect` (backend/docs/especificacion-cursos-v1.md).
class MasteryTestTest {

    // Reserva de 6 ejercicios de opción única; la buena es siempre la 0.
    private val pool = (1..6).map { SingleChoice("r$it", "Pregunta $it", listOf("Bien", "Mal"), correct = 0) }

    private val withDefault = Module("bfs", "BFS", review = pool)
    private val withOwn = Module("dfs", "DFS", review = pool, mastery = Mastery(questions = 2, minCorrect = 2))
    private val withoutReview = Module("intro", "Intro")

    private val course = Course(
        "c", "Curso", listOf(withDefault, withOwn, withoutReview),
        defaultMastery = Mastery(questions = 4, minCorrect = 3),
    )

    private fun testFor(moduleId: String, seed: Int = 1) = course.masteryTest(moduleId, Random(seed))!!

    // ── Qué prueba toca ──

    @Test
    fun `a module without its own mastery uses the course default`() {
        assertEquals(Mastery(4, 3), course.masteryFor(withDefault))
        val test = testFor("bfs")
        assertEquals(4, test.questions.size)
        assertEquals(3, test.minCorrect)
    }

    @Test
    fun `a module with its own mastery uses it`() {
        assertEquals(Mastery(2, 2), course.masteryFor(withOwn))
        assertEquals(2, testFor("dfs").questions.size)
    }

    @Test
    fun `there is no test for a module without review pool, or one that does not exist`() {
        assertNull(course.masteryTest("intro", Random(1)))
        assertNull(course.masteryTest("no-existe", Random(1)))
    }

    // ── El sorteo ──

    @Test
    fun `the questions are drawn from the review pool, without repeating`() {
        val ids = testFor("bfs").questions.map { it.id }
        assertTrue(ids.all { id -> pool.any { it.id == id } })
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `the same seed draws the same questions`() {
        assertEquals(testFor("bfs", seed = 7).questions, testFor("bfs", seed = 7).questions)
    }

    // Cada intento es un sorteo nuevo (con 6 de reserva y 4 preguntas, alguna semilla cambia el orden o las preguntas).
    @Test
    fun `another draw can give other questions`() {
        val draws = (1..10).map { testFor("bfs", seed = it).questions }.toSet()
        assertTrue(draws.size > 1)
    }

    // Si la reserva fuese más pequeña que la prueba (el validador del servidor no lo deja pasar),
    // se usa la reserva entera y no se piden más aciertos que preguntas.
    @Test
    fun `a pool smaller than the test gives a test as big as the pool`() {
        val small = Module("m", "M", review = pool.take(2))
        val test = Course("c", "C", listOf(small), defaultMastery = Mastery(4, 3)).masteryTest("m", Random(1))!!
        assertEquals(2, test.questions.size)
        assertEquals(2, test.minCorrect)
    }

    // ── Aprobar o no ──

    @Test
    fun `reaching just the minimum passes`() {
        assertEquals(MasteryResult.Passed, testFor("bfs").resultFor(correctCount = 3))
        assertEquals(MasteryResult.Passed, testFor("bfs").resultFor(correctCount = 4))
    }

    @Test
    fun `below the minimum fails, saying how many were missing`() {
        assertEquals(MasteryResult.Failed(missing = 1), testFor("bfs").resultFor(correctCount = 2))
        assertEquals(MasteryResult.Failed(missing = 3), testFor("bfs").resultFor(correctCount = 0))
    }

    // ── Hacer la prueba con la práctica que ya existe ──

    @Test
    fun `the test is answered as a practice with its questions`() {
        val test = testFor("bfs")
        val session = test.start(Random(1))
        assertEquals(test.questions, session.exercises.map { it.question })
    }

    @Test
    fun `the result of a finished practice counts its right answers`() {
        val test = testFor("bfs")
        // Acierta las 3 primeras (opción 0) y falla la última (opción 1).
        val session = (0 until 4).fold(test.start(Random(1))) { s, i -> s.update { it.choose(if (i < 3) 0 else 1).check() }.next() }
        assertEquals(MasteryResult.Passed, test.resultOf(session))
    }

    @Test
    fun `an unfinished practice has no result yet`() {
        val test = testFor("bfs")
        val started: PracticeSession = test.start(Random(1)).update { it.choose(0).check() }
        assertNull(test.resultOf(started))
        assertFalse(started.isFinished)
    }
}
