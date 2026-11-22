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

    // ── Pedir lo mismo (tarea 8a: la comparación pasa de FilterSheetState a CourseQuery) ──

    @Test
    fun `two queries with the same text and tags ask the same`() {
        val a = CourseQuery(text = "red", tags = listOf("grafos"))
        val b = CourseQuery(text = "red", tags = listOf("grafos"))

        assertTrue(a.asksTheSameAs(b))
    }

    @Test
    fun `the empty queries ask the same`() {
        assertTrue(CourseQuery.EMPTY.asksTheSameAs(CourseQuery()))
    }

    // El adaptador HTTP recorta el texto: «red » y «red» piden lo mismo.
    @Test
    fun `text that only differs by surrounding spaces asks the same on either side`() {
        val plain = CourseQuery(text = "red")
        val spaced = CourseQuery(text = "  red ")

        assertTrue(plain.asksTheSameAs(spaced))
        assertTrue(spaced.asksTheSameAs(plain))
    }

    @Test
    fun `a blank text asks the same as no text`() {
        assertTrue(CourseQuery(text = "   ").asksTheSameAs(CourseQuery.EMPTY))
    }

    @Test
    fun `spaces inside the text do make a different query`() {
        assertFalse(CourseQuery(text = "red neuronal").asksTheSameAs(CourseQuery(text = "redneuronal")))
    }

    @Test
    fun `a different text asks something else`() {
        assertFalse(CourseQuery(text = "red").asksTheSameAs(CourseQuery(text = "redes")))
    }

    // El curso debe tener todas las etiquetas, así que el orden no cambia lo que se pide.
    @Test
    fun `the same tags in another order ask the same`() {
        val a = CourseQuery(tags = listOf("grafos", "árboles"))
        val b = CourseQuery(tags = listOf("árboles", "grafos"))

        assertTrue(a.asksTheSameAs(b))
    }

    @Test
    fun `the same tags with other case or extra spaces ask the same`() {
        val a = CourseQuery(tags = listOf("teoría de grafos"))
        val b = CourseQuery(tags = listOf("  Teoría   de GRAFOS "))

        assertTrue(a.asksTheSameAs(b))
    }

    @Test
    fun `different tags with the same count ask something else`() {
        val a = CourseQuery(tags = listOf("grafos"))
        val b = CourseQuery(tags = listOf("árboles"))

        assertFalse(a.asksTheSameAs(b))
    }

    @Test
    fun `one tag more or less asks something else`() {
        val one = CourseQuery(tags = listOf("grafos"))
        val two = CourseQuery(tags = listOf("grafos", "árboles"))

        assertFalse(one.asksTheSameAs(two))
        assertFalse(two.asksTheSameAs(one))
    }

    @Test
    fun `asking the same does not depend on what the other query is compared to`() {
        val a = CourseQuery(text = "red ", tags = listOf("grafos", "Árboles"))
        val b = CourseQuery(text = "red", tags = listOf("árboles", "grafos"))

        assertEquals(a.asksTheSameAs(b), b.asksTheSameAs(a))
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
