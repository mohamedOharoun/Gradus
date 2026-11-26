package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.ui.components.actionLabel
import es.ulpgc.pamn.dominio.ui.components.joinedWithAnd
import es.ulpgc.pamn.dominio.ui.components.label
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// Los textos del camino de un curso (docs/tareas/ajustes.md, fase 3, paso 8a). Lo que escribe el autor
// (títulos de módulos) va como dato, sin traducir; solo se traducen las frases de la app.
class CoursePathTextsTest {

    private val graphs = Module("grafos", "Grafos")
    private val sets = Module("conjuntos", "Conjuntos")
    private val relations = Module("relaciones", "Relaciones")
    private val bfs = Module("bfs", "BFS")

    // ── Enumerar («A, B y C» / «A, B and C») ──

    @Test
    fun `one item is itself and none is empty`() {
        assertEquals(UiText.raw(""), joinedWithAnd(emptyList()))
        assertEquals(UiText.raw("Grafos"), joinedWithAnd(listOf("Grafos")))
    }

    @Test
    fun `two items are joined with and`() {
        assertEquals(UiText.of(R.string.list_and, "Grafos", "Conjuntos"), joinedWithAnd(listOf("Grafos", "Conjuntos")))
    }

    // «A, B y C»: las primeras con comas y la última con «y» (cada idioma pone su «y» y su coma).
    @Test
    fun `three items use commas and then and`() {
        assertEquals(
            UiText.of(R.string.list_and, UiText.of(R.string.list_comma, "Grafos", "Conjuntos"), "Relaciones"),
            joinedWithAnd(listOf("Grafos", "Conjuntos", "Relaciones")),
        )
    }

    // ── Avisos del camino ──

    @Test
    fun `a locked module says it is locked`() {
        assertEquals(UiText.of(R.string.course_locked, "BFS"), CourseNotice.Locked(bfs, emptyList()).toMessage())
    }

    @Test
    fun `a locked module says what to master before`() {
        assertEquals(
            UiText.of(R.string.course_locked_missing, "BFS", joinedWithAnd(listOf("Grafos", "Conjuntos"))),
            CourseNotice.Locked(bfs, listOf(graphs, sets)).toMessage(),
        )
    }

    // ── Problemas al abrir el curso ──

    @Test
    fun `each problem has its title and its explanation`() {
        assertEquals(CourseProblemText(UiText.of(R.string.course_not_found_title), UiText.of(R.string.course_not_found_body)), CourseProblem.NOT_FOUND.toText())
        assertEquals(CourseProblemText(UiText.of(R.string.course_unreachable_title), UiText.of(R.string.course_unreachable_body)), CourseProblem.UNREACHABLE.toText())
        assertEquals(CourseProblemText(UiText.of(R.string.course_unsupported_title), UiText.of(R.string.course_unsupported_body)), CourseProblem.UNSUPPORTED_FORMAT.toText())
        assertEquals(CourseProblemText(UiText.of(R.string.course_server_failure_title), UiText.of(R.string.course_server_failure_body)), CourseProblem.SERVER_FAILURE.toText())
    }

    // ── Los módulos del camino, para TalkBack ──

    @Test
    fun `each module state is said and has its action`() {
        assertEquals(UiText.of(R.string.module_state_mastered), ModuleState.MASTERED.label())
        assertEquals(UiText.of(R.string.module_state_frontier), ModuleState.FRONTIER.label())
        assertEquals(UiText.of(R.string.module_state_locked), ModuleState.LOCKED.label())
        assertEquals(UiText.of(R.string.module_action_review), ModuleState.MASTERED.actionLabel())
        assertEquals(UiText.of(R.string.module_action_practice), ModuleState.FRONTIER.actionLabel())
        assertEquals(UiText.of(R.string.module_action_missing), ModuleState.LOCKED.actionLabel())
    }
}
