package es.ulpgc.pamn.dominio.course.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Tarea 1 de buscar-cursos.md: la consulta de búsqueda es un valor inmutable con funciones puras.
class CourseQueryTest {

    // ── La consulta vacía ──

    @Test
    fun `the empty query has no text nor tags nor active filters`() {
        assertEquals("", CourseQuery.EMPTY.text)
        assertTrue(CourseQuery.EMPTY.tags.isEmpty())
        assertFalse(CourseQuery.EMPTY.hasText)
        assertTrue(CourseQuery.EMPTY.activeFilters.isEmpty())
    }

    // ── Texto ──

    @Test
    fun `the text is kept exactly as it is typed`() {
        assertEquals("  grafos ", CourseQuery.EMPTY.withText("  grafos ").text)
    }

    @Test
    fun `a blank text counts as no text`() {
        assertFalse(CourseQuery.EMPTY.withText("   ").hasText)
        assertTrue(CourseQuery.EMPTY.withText("grafos").hasText)
    }

    // ── Etiquetas ──

    @Test
    fun `toggling a tag that is not chosen adds it`() {
        assertEquals(listOf("grafos"), CourseQuery.EMPTY.toggleTag("grafos").tags)
    }

    @Test
    fun `toggling a tag that is already chosen removes it`() {
        val query = CourseQuery.EMPTY.toggleTag("grafos").toggleTag("grafos")
        assertTrue(query.tags.isEmpty())
    }

    @Test
    fun `tags keep the order in which they were chosen`() {
        val query = CourseQuery.EMPTY.toggleTag("grafos").toggleTag("algoritmos").toggleTag("árboles")
        assertEquals(listOf("grafos", "algoritmos", "árboles"), query.tags)
    }

    @Test
    fun `the same tag with other case or extra spaces is the same tag`() {
        val query = CourseQuery.EMPTY.toggleTag("Grafos").toggleTag("  GRAFOS ")
        assertTrue(query.tags.isEmpty(), "the second toggle removes the first one")
    }

    @Test
    fun `a tag keeps the case it was chosen with`() {
        val query = CourseQuery.EMPTY.toggleTag("Grafos").toggleTag("algoritmos")
        assertEquals(listOf("Grafos", "algoritmos"), query.tags)
    }

    @Test
    fun `a tag is stored with its extra spaces tidied`() {
        assertEquals(listOf("teoría de grafos"), CourseQuery.EMPTY.toggleTag("  teoría  de grafos ").tags)
    }

    @Test
    fun `spaces inside a tag are collapsed when comparing`() {
        val query = CourseQuery.EMPTY.toggleTag("teoría  de grafos").toggleTag("Teoría de   Grafos")
        assertTrue(query.tags.isEmpty())
    }

    @Test
    fun `a blank tag is ignored`() {
        assertEquals(CourseQuery.EMPTY, CourseQuery.EMPTY.toggleTag("   "))
        assertEquals(CourseQuery.EMPTY, CourseQuery.EMPTY.withoutTag(""))
    }

    // ── Quitar etiquetas ──

    @Test
    fun `without tag removes it whatever its case`() {
        val query = CourseQuery.EMPTY.toggleTag("Grafos").toggleTag("algoritmos").withoutTag("GRAFOS")
        assertEquals(listOf("algoritmos"), query.tags)
    }

    @Test
    fun `without a tag that is not chosen changes nothing`() {
        val query = CourseQuery.EMPTY.toggleTag("grafos")
        assertEquals(query, query.withoutTag("árboles"))
    }

    @Test
    fun `without filters removes the tags but keeps the text`() {
        val query = CourseQuery.EMPTY.withText("estructuras").toggleTag("grafos").toggleTag("árboles").withoutFilters()
        assertEquals(CourseQuery(text = "estructuras"), query)
    }

    // ── Filtros activos (los chips y el número de «Filtros 2») ──

    @Test
    fun `active filters are one tag chip per tag in the order they were chosen`() {
        val query = CourseQuery.EMPTY.toggleTag("grafos").toggleTag("Árboles")
        assertEquals(listOf(ActiveFilter.Tag("grafos"), ActiveFilter.Tag("Árboles")), query.activeFilters)
    }

    @Test
    fun `the text is not an active filter`() {
        val query = CourseQuery.EMPTY.withText("grafos").toggleTag("algoritmos")
        assertEquals(1, query.activeFilters.size)
    }

    // ── Inmutabilidad ──

    @Test
    fun `changing a query gives a new one and leaves the original untouched`() {
        val original = CourseQuery.EMPTY.withText("a").toggleTag("grafos")
        original.toggleTag("árboles")
        original.withText("b")
        original.withoutFilters()
        assertEquals(CourseQuery(text = "a", tags = listOf("grafos")), original)
    }
}
