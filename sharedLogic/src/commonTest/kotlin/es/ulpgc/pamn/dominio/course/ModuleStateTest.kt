package es.ulpgc.pamn.dominio.course

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Regla 2 de la especificación (§4): un módulo se desbloquea cuando TODOS sus prerrequisitos están dominados.
class ModuleStateTest {

    // Conjuntos → Relaciones → (Grafos, Árboles); Grafos → BFS; Grafos y Árboles → DFS.
    private val course = Course(
        id = "ed",
        title = "Estructuras de datos",
        modules = listOf(
            Module("conjuntos", "Conjuntos"),
            Module("relaciones", "Relaciones", requires = listOf("conjuntos")),
            Module("grafos", "Grafos", requires = listOf("relaciones")),
            Module("arboles", "Árboles", requires = listOf("relaciones")),
            Module("bfs", "BFS", requires = listOf("grafos")),
            Module("dfs", "DFS", requires = listOf("grafos", "arboles")),
        ),
    )

    // ── Estado de cada módulo ──

    @Test
    fun `with nothing mastered, only the modules without prerequisites are in the frontier`() {
        assertEquals(ModuleState.FRONTIER, course.stateOf("conjuntos", mastered = emptySet()))
        assertEquals(ModuleState.LOCKED, course.stateOf("relaciones", mastered = emptySet()))
    }

    @Test
    fun `a mastered module is mastered`() {
        assertEquals(ModuleState.MASTERED, course.stateOf("conjuntos", mastered = setOf("conjuntos")))
    }

    @Test
    fun `a module whose only prerequisite is mastered is in the frontier`() {
        assertEquals(ModuleState.FRONTIER, course.stateOf("relaciones", mastered = setOf("conjuntos")))
    }

    @Test
    fun `with several prerequisites, one missing keeps it locked`() {
        val mastered = setOf("conjuntos", "relaciones", "grafos")
        assertEquals(ModuleState.LOCKED, course.stateOf("dfs", mastered))
    }

    @Test
    fun `with all its prerequisites mastered it is in the frontier`() {
        val mastered = setOf("conjuntos", "relaciones", "grafos", "arboles")
        assertEquals(ModuleState.FRONTIER, course.stateOf("dfs", mastered))
    }

    // Regla 5 (provisional): fallar un repaso no vuelve a bloquear. Lo dominado sigue dominado
    // aunque falte algún prerrequisito.
    @Test
    fun `mastered wins even if a prerequisite is not mastered`() {
        assertEquals(ModuleState.MASTERED, course.stateOf("relaciones", mastered = setOf("relaciones")))
    }

    @Test
    fun `states for gives the state of every module, in the order of the course`() {
        val states = course.statesFor(mastered = setOf("conjuntos", "relaciones"))
        assertEquals(
            listOf(
                "conjuntos" to ModuleState.MASTERED,
                "relaciones" to ModuleState.MASTERED,
                "grafos" to ModuleState.FRONTIER,
                "arboles" to ModuleState.FRONTIER,
                "bfs" to ModuleState.LOCKED,
                "dfs" to ModuleState.LOCKED,
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
    fun `nothing is missing for a module in the frontier`() {
        assertTrue(course.missingFor("relaciones", mastered = setOf("conjuntos")).isEmpty())
    }

    // ── Buscar un módulo ──

    @Test
    fun `a module is found by its id`() {
        assertEquals("Grafos", course.module("grafos")?.title)
        assertNull(course.module("no-existe"))
    }

    @Test
    fun `the mastered count only counts modules of the course`() {
        assertEquals(2, course.masteredCount(mastered = setOf("conjuntos", "grafos", "otro-curso")))
    }
}
