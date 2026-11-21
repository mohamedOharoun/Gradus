package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// El modelo del curso tal como lo define el backend (course-v1.schema.json): módulos con su parte
// formativa (`steps`: lecciones y ejercicios), su reserva (`review`) y su prueba de dominio.
class CourseModelTest {

    private val queue = SingleChoice("bfs-f1", "¿Qué usa BFS?", listOf("Cola", "Pila"), correct = 0, explanation = "BFS va por **capas**.")
    private val steps = MultipleChoice("bfs-f2", "¿Qué hace BFS al sacar u?", listOf("Examina vecinos", "Nada"), correct = setOf(0))
    private val reserve = Matching("bfs-r1", "Une", listOf(MatchPair("BFS", "Cola"), MatchPair("DFS", "Pila")))

    private val bfs = Module(
        id = "bfs",
        title = "Recorrido en anchura",
        requires = listOf("representacion"),
        steps = listOf(
            Step.Lesson("bfs-l1", "El recorrido en anchura explora por capas."),
            Step.Exercise(queue),
            Step.Lesson("bfs-l2", "Cuesta O(V + E)."),
            Step.Exercise(steps),
        ),
        review = listOf(reserve),
    )

    private val course = Course(
        id = "grafos",
        title = "Introducción a los grafos",
        modules = listOf(Module("representacion", "Representación"), bfs),
        description = "Cómo se guarda un grafo y cómo se recorre.",
        language = "es",
        tags = listOf("grafos", "algoritmos"),
        defaultMastery = Mastery(questions = 4, minCorrect = 3),
    )

    // ── Pasos de un módulo ──

    @Test
    fun `the exercises of a module are its exercise steps, in order, without the lessons`() {
        assertEquals(listOf(queue, steps), bfs.exercises)
    }

    @Test
    fun `the questions to practise a module are its exercises`() {
        assertEquals(listOf(queue, steps), course.questionsFor("bfs"))
        assertTrue(course.questionsFor("representacion").isEmpty())
        assertTrue(course.questionsFor("no-existe").isEmpty())
    }

    @Test
    fun `an exercise step has the id of its question`() {
        assertEquals("bfs-f1", Step.Exercise(queue).id)
    }

    @Test
    fun `the review pool is kept apart from the steps`() {
        assertEquals(listOf(reserve), bfs.review)
        assertTrue(reserve !in bfs.exercises)
    }

    // ── Explicación de cada ejercicio ──

    @Test
    fun `an exercise keeps its explanation, and has none by default`() {
        assertEquals("BFS va por **capas**.", queue.explanation)
        assertEquals("", steps.explanation)
    }

    // ── Prueba de dominio ──

    @Test
    fun `a module without its own mastery uses none (the course default applies)`() {
        assertNull(bfs.mastery)
        assertEquals(Mastery(4, 3), course.defaultMastery)
    }

    @Test
    fun `a mastery test needs at least one question`() {
        assertFailsWith<IllegalArgumentException> { Mastery(questions = 0, minCorrect = 0) }
    }

    @Test
    fun `a mastery test needs at least one right answer`() {
        assertFailsWith<IllegalArgumentException> { Mastery(questions = 4, minCorrect = 0) }
    }

    @Test
    fun `a mastery test cannot ask for more right answers than questions`() {
        assertFailsWith<IllegalArgumentException> { Mastery(questions = 3, minCorrect = 4) }
    }

    // ── Datos del curso ──

    @Test
    fun `a course keeps its description, language and tags`() {
        assertEquals("Cómo se guarda un grafo y cómo se recorre.", course.description)
        assertEquals("es", course.language)
        assertEquals(listOf("grafos", "algoritmos"), course.tags)
    }
}
