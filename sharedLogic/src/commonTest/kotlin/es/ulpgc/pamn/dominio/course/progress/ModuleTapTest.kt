package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

// Combina el acceso (inscripción) con el estado del módulo: qué pasa al tocar un módulo del camino.
// La falta de inscripción va antes que «bloqueado» (decisión C-6).
class ModuleTapTest {

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

    private val enrolled = StudyAccess.Allowed
    private val notEnrolled = StudyAccess.NotEnrolled

    // ── Sin inscripción: siempre «sin inscripción», sea cual sea el estado ──

    @Test
    fun `not enrolled and frontier gives not enrolled`() {
        val tap = course.tapModule("conjuntos", mastered = emptySet(), access = notEnrolled)
        assertEquals(ModuleTap.NotEnrolled, tap)
    }

    @Test
    fun `not enrolled and mastered gives not enrolled`() {
        val tap = course.tapModule("conjuntos", mastered = setOf("conjuntos"), access = notEnrolled)
        assertEquals(ModuleTap.NotEnrolled, tap)
    }

    @Test
    fun `not enrolled and locked gives not enrolled`() {
        val tap = course.tapModule("relaciones", mastered = emptySet(), access = notEnrolled)
        assertEquals(ModuleTap.NotEnrolled, tap)
    }

    // ── Inscrito ──

    @Test
    fun `enrolled and frontier opens the practice`() {
        val tap = course.tapModule("conjuntos", mastered = emptySet(), access = enrolled)
        assertEquals(ModuleTap.OpenPractice, tap)
    }

    @Test
    fun `enrolled and mastered opens the practice`() {
        val tap = course.tapModule("conjuntos", mastered = setOf("conjuntos"), access = enrolled)
        assertEquals(ModuleTap.OpenPractice, tap)
    }

    @Test
    fun `enrolled and locked gives what is missing`() {
        val mastered = setOf("conjuntos", "relaciones", "grafos")
        val tap = course.tapModule("dfs", mastered, enrolled)
        assertIs<ModuleTap.Locked>(tap)
        assertEquals(listOf("Árboles"), tap.missing.map { it.title })
    }

    // ── Módulo inexistente ──

    @Test
    fun `unknown module gives null when enrolled`() {
        assertNull(course.tapModule("no-existe", mastered = emptySet(), access = enrolled))
    }

    @Test
    fun `unknown module gives null when not enrolled`() {
        assertNull(course.tapModule("no-existe", mastered = emptySet(), access = notEnrolled))
    }

    // ── Casos límite ──

    // missingFor da los prerrequisitos directos (no Conjuntos ni Relaciones) y en el orden de `requires`.
    @Test
    fun `locked lists only the direct prerequisites not mastered in the order of requires`() {
        val tap = course.tapModule("dfs", mastered = emptySet(), access = enrolled)
        assertIs<ModuleTap.Locked>(tap)
        assertEquals(listOf("Grafos", "Árboles"), tap.missing.map { it.title })
    }

    // Regla 5 (provisional): lo dominado sigue dominado aunque falte un prerrequisito.
    @Test
    fun `mastered wins over a missing prerequisite when enrolled`() {
        val tap = course.tapModule("relaciones", mastered = setOf("relaciones"), access = enrolled)
        assertEquals(ModuleTap.OpenPractice, tap)
    }

    @Test
    fun `mastered from another course does not unlock`() {
        val tap = course.tapModule("relaciones", mastered = setOf("otro-curso"), access = enrolled)
        assertIs<ModuleTap.Locked>(tap)
        assertEquals(listOf("Conjuntos"), tap.missing.map { it.title })
    }
}
