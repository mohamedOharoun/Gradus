package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Tarea 4 de buscar-cursos.md: qué etiquetas se ofrecen en la hoja de filtros y en qué orden.
class TagSuggestionsTest {

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

    // grafos en 3 cursos, árboles en 2 y recursión en 1.
    private val results = listOf(
        course("c1", "grafos", "árboles", "recursión"),
        course("c2", "grafos", "árboles"),
        course("c3", "grafos"),
    )

    // ── Orden ──

    @Test
    fun `tags that are in more courses come first`() {
        val suggestions = TagSuggestions.from(results, selected = emptyList())

        assertEquals(listOf("grafos", "árboles", "recursión"), suggestions.all)
    }

    @Test
    fun `a tie is broken alphabetically ignoring accents`() {
        val tied = listOf(course("c1", "zorros", "bases de datos", "árboles"))

        val suggestions = TagSuggestions.from(tied, selected = emptyList())

        assertEquals(listOf("árboles", "bases de datos", "zorros"), suggestions.all)
    }

    @Test
    fun `a tie is broken alphabetically ignoring case`() {
        val tied = listOf(course("c1", "Zorros", "bases", "Árboles"))

        val suggestions = TagSuggestions.from(tied, selected = emptyList())

        assertEquals(listOf("Árboles", "bases", "Zorros"), suggestions.all)
    }

    @Test
    fun `two tags that differ only by an accent keep a fixed order whatever the input order`() {
        val accentFirst = listOf(course("c1", "árbol", "arbol"))
        val plainFirst = listOf(course("c1", "arbol", "árbol"))

        assertEquals(listOf("arbol", "árbol"), TagSuggestions.from(accentFirst, selected = emptyList()).all)
        assertEquals(listOf("arbol", "árbol"), TagSuggestions.from(plainFirst, selected = emptyList()).all)
    }

    @Test
    fun `selected tags come first in the order they were chosen`() {
        val suggestions = TagSuggestions.from(results, selected = listOf("recursión", "árboles"))

        assertEquals(listOf("recursión", "árboles", "grafos"), suggestions.all)
    }

    @Test
    fun `a selected tag that is not in the results is still offered`() {
        val suggestions = TagSuggestions.from(results, selected = listOf("sql"))

        assertEquals(listOf("sql", "grafos", "árboles", "recursión"), suggestions.all)
    }

    // ── Mismas etiquetas ──

    @Test
    fun `the same tag with other case counts once and shows its first form`() {
        val mixed = listOf(
            course("c1", "Grafos", "sql"),
            course("c2", "grafos"),
            course("c3", "GRAFOS"),
        )

        val suggestions = TagSuggestions.from(mixed, selected = emptyList())

        assertEquals(listOf("Grafos", "sql"), suggestions.all)
    }

    @Test
    fun `a selected tag is shown as it was selected`() {
        val mixed = listOf(course("c1", "Grafos"))

        val suggestions = TagSuggestions.from(mixed, selected = listOf("grafos"))

        assertEquals(listOf("grafos"), suggestions.all)
    }

    @Test
    fun `a course that repeats a tag counts it once`() {
        val repeated = listOf(
            course("c1", "redes", "Redes", "redes"),
            course("c2", "grafos"),
            course("c3", "grafos"),
        )

        val suggestions = TagSuggestions.from(repeated, selected = emptyList())

        assertEquals(listOf("grafos", "redes"), suggestions.all)
    }

    @Test
    fun `blank tags are ignored`() {
        val blank = listOf(course("c1", "", "   ", "grafos"))

        val suggestions = TagSuggestions.from(blank, selected = listOf("  "))

        assertEquals(listOf("grafos"), suggestions.all)
    }

    // ── Cuántas se ven ──

    private val nine = listOf(course("c1", "a", "b", "c", "d", "e", "f", "g", "h", "i"))

    @Test
    fun `only seven tags are shown and the rest are hidden`() {
        val suggestions = TagSuggestions.from(nine, selected = emptyList())

        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g"), suggestions.shown)
        assertEquals(listOf("h", "i"), suggestions.hidden)
        assertTrue(suggestions.hasHidden)
        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g", "h", "i"), suggestions.all)
    }

    @Test
    fun `nothing is hidden when there are seven or fewer`() {
        val suggestions = TagSuggestions.from(results, selected = emptyList())

        assertTrue(suggestions.hidden.isEmpty())
        assertFalse(suggestions.hasHidden)
    }

    @Test
    fun `how many are shown can be changed`() {
        val suggestions = TagSuggestions.from(nine, selected = emptyList(), shown = 3)

        assertEquals(listOf("a", "b", "c"), suggestions.shown)
        assertEquals(6, suggestions.hidden.size)
    }

    @Test
    fun `all the selected tags are shown even when there are more than seven`() {
        val selected = listOf("s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8")
        val withExtra = listOf(course("c1", "grafos"))

        val suggestions = TagSuggestions.from(withExtra, selected)

        assertEquals(selected, suggestions.shown)
        assertEquals(listOf("grafos"), suggestions.hidden)
    }

    // ── Nada ──

    @Test
    fun `without results or selected tags both lists are empty`() {
        val suggestions = TagSuggestions.from(emptyList(), selected = emptyList())

        assertTrue(suggestions.shown.isEmpty())
        assertTrue(suggestions.hidden.isEmpty())
        assertTrue(suggestions.all.isEmpty())
        assertFalse(suggestions.hasHidden)
    }
}
