package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Tarea 7a de buscar-cursos.md: el estado de la hoja de filtros (borrador, sugerencias y «Ver todas»).
class FilterSheetStateTest {

    private fun course(id: String, vararg tags: String) = CourseSummary(
        id = id,
        version = 1,
        author = Author(id = "u1", name = "Ana Ruiz"),
        publishedAt = "2026-10-03T08:35:00Z",
        title = "Curso $id",
        description = "",
        language = "es",
        tags = tags.toList(),
        moduleCount = 10,
    )

    // grafos en 3 cursos, árboles en 2 y recursión en 1: caben todas en las 7 que se ven.
    private val results = listOf(
        course("c1", "grafos", "árboles", "recursión"),
        course("c2", "grafos", "árboles"),
        course("c3", "grafos"),
    )

    // Nueve etiquetas con un curso cada una: salen a..g y quedan h, i ocultas.
    private val manyTags = listOf(course("c1", "a", "b", "c", "d", "e", "f", "g", "h", "i"))

    private fun opened(applied: CourseQuery = CourseQuery.EMPTY) = FilterSheetState.open(applied, results)

    // ── open ──

    @Test
    fun `open copies the applied query into the draft`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        val state = FilterSheetState.open(applied, results)

        assertEquals(applied, state.draft)
    }

    @Test
    fun `open starts with the full list collapsed`() {
        assertFalse(opened().showingAll)
    }

    @Test
    fun `open puts the applied tags first in the suggestions`() {
        val state = opened(CourseQuery(tags = listOf("recursión")))

        assertEquals(listOf("recursión", "grafos", "árboles"), state.visibleTags)
    }

    @Test
    fun `open shows an applied tag even if the results do not have it`() {
        val state = opened(CourseQuery(tags = listOf("química")))

        assertEquals("química", state.visibleTags.first())
        assertTrue(state.isSelected("química"))
    }

    // ── toggle e isSelected ──

    @Test
    fun `toggle selects and unselects a tag`() {
        val selected = opened().toggle("grafos")
        assertTrue(selected.isSelected("grafos"))
        assertEquals(listOf("grafos"), selected.draft.tags)

        val unselected = selected.toggle("grafos")
        assertFalse(unselected.isSelected("grafos"))
        assertEquals(emptyList(), unselected.draft.tags)
    }

    @Test
    fun `toggle ignores case and extra spaces`() {
        val state = opened(CourseQuery(tags = listOf("grafos"))).toggle("  GRAFOS ")

        assertFalse(state.isSelected("grafos"))
        assertEquals(emptyList(), state.draft.tags)
    }

    @Test
    fun `isSelected ignores case and extra spaces`() {
        val state = opened().toggle("Grafos")

        assertTrue(state.isSelected("  grafos "))
    }

    @Test
    fun `isSelected is false for a tag that is not in the draft`() {
        assertFalse(opened().toggle("grafos").isSelected("árboles"))
    }

    @Test
    fun `toggle with a blank tag changes nothing`() {
        val state = opened()

        assertEquals(state, state.toggle("   "))
    }

    @Test
    fun `toggle does not touch the applied query`() {
        val applied = CourseQuery(tags = listOf("grafos"))

        opened(applied).toggle("árboles")

        assertEquals(CourseQuery(tags = listOf("grafos")), applied)
    }

    @Test
    fun `suggestions do not change when toggling`() {
        val state = opened()

        assertEquals(state.suggestions, state.toggle("recursión").suggestions)
        assertEquals(state.visibleTags, state.toggle("recursión").visibleTags)
    }

    // ── reset ──

    @Test
    fun `reset empties the tags and keeps the text`() {
        val state = opened(CourseQuery(text = "red", tags = listOf("grafos", "árboles")))

        assertEquals(CourseQuery(text = "red"), state.reset().draft)
    }

    @Test
    fun `reset does not touch the suggestions`() {
        val state = opened(CourseQuery(tags = listOf("recursión")))

        assertEquals(state.suggestions, state.reset().suggestions)
    }

    @Test
    fun `reset does not collapse the full list`() {
        val state = FilterSheetState.open(CourseQuery(tags = listOf("a")), manyTags).showAll()

        assertTrue(state.reset().showingAll)
    }

    @Test
    fun `canReset is false without tags and true with them`() {
        assertFalse(opened().canReset)
        assertTrue(opened().toggle("grafos").canReset)
        assertTrue(opened(CourseQuery(tags = listOf("grafos"))).canReset)
    }

    @Test
    fun `canReset is false after reset`() {
        assertFalse(opened(CourseQuery(tags = listOf("grafos"))).reset().canReset)
    }

    @Test
    fun `canReset ignores the text`() {
        assertFalse(opened(CourseQuery(text = "red")).canReset)
    }

    // ── Ver todas ──

    @Test
    fun `without hidden tags there is no see all`() {
        val state = opened()

        assertFalse(state.canShowAll)
        assertEquals(listOf("grafos", "árboles", "recursión"), state.visibleTags)
    }

    @Test
    fun `with hidden tags only the first seven are visible and see all is offered`() {
        val state = FilterSheetState.open(CourseQuery.EMPTY, manyTags)

        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g"), state.visibleTags)
        assertTrue(state.canShowAll)
    }

    @Test
    fun `showAll reveals the hidden tags and stops offering see all`() {
        val state = FilterSheetState.open(CourseQuery.EMPTY, manyTags).showAll()

        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g", "h", "i"), state.visibleTags)
        assertFalse(state.canShowAll)
        assertTrue(state.showingAll)
    }

    @Test
    fun `showAll twice gives the same state`() {
        val once = FilterSheetState.open(CourseQuery.EMPTY, manyTags).showAll()

        assertSame(once, once.showAll())
    }

    @Test
    fun `toggle keeps the full list open`() {
        val state = FilterSheetState.open(CourseQuery.EMPTY, manyTags).showAll().toggle("h")

        assertTrue(state.showingAll)
        assertEquals(9, state.visibleTags.size)
    }

    // ── queryToApply (decisión A) ──

    @Test
    fun `queryToApply returns the draft with the typed text`() {
        val state = opened().toggle("grafos")

        assertEquals(
            CourseQuery(text = "red", tags = listOf("grafos")),
            state.queryToApply(typedText = "red", applied = CourseQuery.EMPTY),
        )
    }

    @Test
    fun `queryToApply returns the new filters when the text did not change`() {
        val applied = CourseQuery(text = "red")
        val state = opened(applied).toggle("grafos")

        assertEquals(
            CourseQuery(text = "red", tags = listOf("grafos")),
            state.queryToApply(typedText = "red", applied = applied),
        )
    }

    @Test
    fun `queryToApply returns null when nothing changed`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        assertNull(opened(applied).queryToApply(typedText = "red", applied = applied))
    }

    @Test
    fun `queryToApply returns the query when only the typed text changed`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        assertEquals(
            CourseQuery(text = "redes", tags = listOf("grafos")),
            opened(applied).queryToApply(typedText = "redes", applied = applied),
        )
    }

    @Test
    fun `queryToApply uses the typed text and not the text the sheet was opened with`() {
        val applied = CourseQuery(text = "red")
        val state = opened(applied).toggle("grafos")

        assertEquals("redes", state.queryToApply(typedText = "redes", applied = applied)?.text)
    }

    @Test
    fun `queryToApply returns the query without tags when every filter was removed`() {
        val applied = CourseQuery(tags = listOf("grafos"))

        assertEquals(CourseQuery.EMPTY, opened(applied).reset().queryToApply(typedText = "", applied = applied))
    }

    @Test
    fun `queryToApply is null if a tag was removed and chosen again`() {
        val applied = CourseQuery(tags = listOf("grafos", "árboles"))
        val state = opened(applied).toggle("grafos").toggle("grafos")  // el borrador queda [árboles, grafos]

        assertEquals(listOf("árboles", "grafos"), state.draft.tags)
        assertNull(state.queryToApply(typedText = "", applied = applied))
    }

    @Test
    fun `queryToApply is null if a tag was chosen again with other capitals`() {
        val applied = CourseQuery(tags = listOf("grafos"))
        val state = opened(applied).toggle("grafos").toggle("Grafos")

        assertNull(state.queryToApply(typedText = "", applied = applied))
    }

    @Test
    fun `queryToApply is not null when the tags are different even with the same count`() {
        val applied = CourseQuery(tags = listOf("grafos"))
        val state = opened(applied).toggle("grafos").toggle("árboles")

        assertEquals(
            CourseQuery(tags = listOf("árboles")),
            state.queryToApply(typedText = "", applied = applied),
        )
    }

    // El adaptador HTTP recorta el texto: «red » y «red» piden lo mismo, así que no se repite la búsqueda.

    @Test
    fun `queryToApply is null if the typed text only differs by surrounding spaces`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        assertNull(opened(applied).queryToApply(typedText = "  red ", applied = applied))
    }

    @Test
    fun `queryToApply is null if the applied text has the extra spaces`() {
        val applied = CourseQuery(text = "red ")

        assertNull(opened(applied).queryToApply(typedText = "red", applied = applied))
    }

    @Test
    fun `queryToApply is null if the typed text is blank and no text was applied`() {
        assertNull(opened().queryToApply(typedText = "   ", applied = CourseQuery.EMPTY))
    }

    @Test
    fun `queryToApply does not ignore spaces inside the text`() {
        val applied = CourseQuery(text = "red neuronal")

        assertEquals(
            CourseQuery(text = "redneuronal"),
            opened(applied).queryToApply(typedText = "redneuronal", applied = applied),
        )
    }

    @Test
    fun `queryToApply keeps the typed text as typed when something else changed`() {
        val state = opened().toggle("grafos")

        assertEquals(
            CourseQuery(text = "red ", tags = listOf("grafos")),
            state.queryToApply(typedText = "red ", applied = CourseQuery.EMPTY),
        )
    }
}
