package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Regla 2 de la especificación (§4): un concepto se desbloquea cuando TODOS sus prerrequisitos están dominados.
class ConceptStateTest {

    // Conjuntos → Relaciones → (Grafos, Árboles); Grafos → BFS; Grafos y Árboles → DFS.
    private val course = Course(
        id = "ed",
        title = "Estructuras de datos",
        concepts = listOf(
            Concept("conjuntos", "Conjuntos"),
            Concept("relaciones", "Relaciones", requires = listOf("conjuntos")),
            Concept("grafos", "Grafos", requires = listOf("relaciones")),
            Concept("arboles", "Árboles", requires = listOf("relaciones")),
            Concept("bfs", "BFS", requires = listOf("grafos")),
            Concept("dfs", "DFS", requires = listOf("grafos", "arboles")),
        ),
    )

    // ── Estado de cada concepto ──

    @Test
    fun `with nothing mastered, only the concepts without prerequisites are in the frontier`() {
        assertEquals(ConceptState.FRONTIER, course.stateOf("conjuntos", mastered = emptySet()))
        assertEquals(ConceptState.LOCKED, course.stateOf("relaciones", mastered = emptySet()))
    }

    @Test
    fun `a mastered concept is mastered`() {
        assertEquals(ConceptState.MASTERED, course.stateOf("conjuntos", mastered = setOf("conjuntos")))
    }

    @Test
    fun `a concept whose only prerequisite is mastered is in the frontier`() {
        assertEquals(ConceptState.FRONTIER, course.stateOf("relaciones", mastered = setOf("conjuntos")))
    }

    @Test
    fun `with several prerequisites, one missing keeps it locked`() {
        val mastered = setOf("conjuntos", "relaciones", "grafos")
        assertEquals(ConceptState.LOCKED, course.stateOf("dfs", mastered))
    }

    @Test
    fun `with all its prerequisites mastered it is in the frontier`() {
        val mastered = setOf("conjuntos", "relaciones", "grafos", "arboles")
        assertEquals(ConceptState.FRONTIER, course.stateOf("dfs", mastered))
    }

    // Regla 5 (provisional): fallar un repaso no vuelve a bloquear. Lo dominado sigue dominado
    // aunque falte algún prerrequisito.
    @Test
    fun `mastered wins even if a prerequisite is not mastered`() {
        assertEquals(ConceptState.MASTERED, course.stateOf("relaciones", mastered = setOf("relaciones")))
    }

    @Test
    fun `states for gives the state of every concept, in the order of the course`() {
        val states = course.statesFor(mastered = setOf("conjuntos", "relaciones"))
        assertEquals(
            listOf(
                "conjuntos" to ConceptState.MASTERED,
                "relaciones" to ConceptState.MASTERED,
                "grafos" to ConceptState.FRONTIER,
                "arboles" to ConceptState.FRONTIER,
                "bfs" to ConceptState.LOCKED,
                "dfs" to ConceptState.LOCKED,
            ),
            states.entries.map { it.key to it.value },
        )
    }

    // ── Qué falta para desbloquear (para el aviso del nodo bloqueado) ──

    @Test
    fun `missing prerequisites are the ones not yet mastered`() {
        val missing = course.missingFor("dfs", mastered = setOf("conjuntos", "relaciones", "grafos"))
        assertEquals(listOf("Árboles"), missing.map { it.title })
    }

    @Test
    fun `nothing is missing for a concept in the frontier`() {
        assertTrue(course.missingFor("relaciones", mastered = setOf("conjuntos")).isEmpty())
    }

    // ── Buscar un concepto ──

    @Test
    fun `a concept is found by its id`() {
        assertEquals("Grafos", course.concept("grafos")?.title)
        assertNull(course.concept("no-existe"))
    }

    // ── Preguntas de un concepto ──

    @Test
    fun `the questions of a concept are only its own, in the order of the course`() {
        val first = SingleChoice("q1", "grafos", "¿?", listOf("Sí", "No"), correct = 0)
        val other = SingleChoice("q2", "bfs", "¿?", listOf("Sí", "No"), correct = 1)
        val second = SingleChoice("q3", "grafos", "¿?", listOf("Sí", "No"), correct = 1)
        val withQuestions = course.copy(questions = listOf(first, other, second))
        assertEquals(listOf(first, second), withQuestions.questionsFor("grafos"))
        assertTrue(withQuestions.questionsFor("conjuntos").isEmpty())
    }

    @Test
    fun `the mastered count only counts concepts of the course`() {
        assertEquals(2, course.masteredCount(mastered = setOf("conjuntos", "grafos", "otro-curso")))
    }
}
