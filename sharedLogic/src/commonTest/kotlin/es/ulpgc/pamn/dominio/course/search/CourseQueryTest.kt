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

    // ── Creador (tarea 11a-1, decisión B) ──

    @Test
    fun `the empty query has no author and any publication date`() {
        assertEquals("", CourseQuery.EMPTY.author)
        assertFalse(CourseQuery.EMPTY.hasAuthor)
        assertEquals(PublishedWithin.ANY, CourseQuery.EMPTY.published)
    }

    // El campo «Creador» lo enseña tal cual; recortarlo es cosa del adaptador HTTP, como el texto.
    @Test
    fun `the author is kept exactly as it is typed`() {
        assertEquals("  ana  ruiz ", CourseQuery.EMPTY.withAuthor("  ana  ruiz ").author)
    }

    @Test
    fun `a blank author does not filter`() {
        assertFalse(CourseQuery.EMPTY.withAuthor("   ").hasAuthor)
        assertTrue(CourseQuery.EMPTY.withAuthor("Ana").hasAuthor)
    }

    @Test
    fun `with author keeps the rest of the query`() {
        val query = CourseQuery(text = "red", tags = listOf("grafos"), published = PublishedWithin.LAST_MONTH)

        assertEquals(
            CourseQuery(text = "red", tags = listOf("grafos"), author = "Ana", published = PublishedWithin.LAST_MONTH),
            query.withAuthor("Ana"),
        )
    }

    @Test
    fun `without author empties it and keeps the rest`() {
        val query = CourseQuery(text = "red", tags = listOf("grafos"), author = "Ana", published = PublishedWithin.LAST_YEAR)

        assertEquals(
            CourseQuery(text = "red", tags = listOf("grafos"), published = PublishedWithin.LAST_YEAR),
            query.withoutAuthor(),
        )
    }

    @Test
    fun `without author when there is none changes nothing`() {
        val query = CourseQuery(text = "red", tags = listOf("grafos"))

        assertEquals(query, query.withoutAuthor())
    }

    // ── Fecha de publicación (tarea 11a-1, decisión A) ──

    @Test
    fun `with published sets the date and keeps the rest`() {
        val query = CourseQuery(text = "red", tags = listOf("grafos"), author = "Ana")

        assertEquals(
            CourseQuery(text = "red", tags = listOf("grafos"), author = "Ana", published = PublishedWithin.LAST_MONTH),
            query.withPublished(PublishedWithin.LAST_MONTH),
        )
    }

    // Así se quita el chip de la fecha: «Cualquiera» no filtra.
    @Test
    fun `with published any removes the date`() {
        val query = CourseQuery(text = "red", published = PublishedWithin.LAST_YEAR)

        assertEquals(CourseQuery(text = "red"), query.withPublished(PublishedWithin.ANY))
    }

    // ── Chips del creador y de la fecha (decisión C) ──

    // El orden es el de las secciones de la hoja: Etiquetas, Creador y Fecha de publicación.
    @Test
    fun `active filters are the tags then the author then the date`() {
        val query = CourseQuery(
            text = "red",
            tags = listOf("grafos", "árboles"),
            author = "Ana",
            published = PublishedWithin.LAST_YEAR,
        )

        val expected = listOf(
            ActiveFilter.Tag("grafos"),
            ActiveFilter.Tag("árboles"),
            ActiveFilter.Author("Ana"),
            ActiveFilter.Published(PublishedWithin.LAST_YEAR),
        )
        assertEquals(expected, query.activeFilters)
    }

    @Test
    fun `the author chip has its spaces tidied and keeps its capitals`() {
        val query = CourseQuery.EMPTY.withAuthor("  Ana   Ruiz ")

        assertEquals(listOf<ActiveFilter>(ActiveFilter.Author("Ana Ruiz")), query.activeFilters)
    }

    @Test
    fun `a blank author has no chip`() {
        assertTrue(CourseQuery(author = "   ").activeFilters.isEmpty())
    }

    @Test
    fun `any publication date has no chip`() {
        assertTrue(CourseQuery(published = PublishedWithin.ANY).activeFilters.isEmpty())
    }

    @Test
    fun `last month and last year have their own chip`() {
        assertEquals(
            listOf<ActiveFilter>(ActiveFilter.Published(PublishedWithin.LAST_MONTH)),
            CourseQuery(published = PublishedWithin.LAST_MONTH).activeFilters,
        )
        assertEquals(
            listOf<ActiveFilter>(ActiveFilter.Published(PublishedWithin.LAST_YEAR)),
            CourseQuery(published = PublishedWithin.LAST_YEAR).activeFilters,
        )
    }

    // «Restablecer» (decisión D).
    @Test
    fun `without filters also empties the author and the date and keeps the text`() {
        val query = CourseQuery(
            text = "red",
            tags = listOf("grafos"),
            author = "Ana",
            published = PublishedWithin.LAST_MONTH,
        )

        assertEquals(CourseQuery(text = "red"), query.withoutFilters())
    }

    // ── Pedir lo mismo con creador y fecha ──
    // El autor se compara como lo normaliza el backend: sin mayúsculas ni espacios de sobra.

    @Test
    fun `the same author with other case or extra spaces asks the same`() {
        val plain = CourseQuery(author = "Ana Ruiz")
        val messy = CourseQuery(author = "  ana   RUIZ ")

        assertTrue(plain.asksTheSameAs(messy))
        assertTrue(messy.asksTheSameAs(plain))
    }

    @Test
    fun `a blank author asks the same as no author`() {
        assertTrue(CourseQuery(author = "   ").asksTheSameAs(CourseQuery.EMPTY))
    }

    @Test
    fun `a different author asks something else`() {
        assertFalse(CourseQuery(author = "Ana").asksTheSameAs(CourseQuery(author = "Luis")))
        assertFalse(CourseQuery(author = "Ana").asksTheSameAs(CourseQuery.EMPTY))
        assertFalse(CourseQuery.EMPTY.asksTheSameAs(CourseQuery(author = "Ana")))
    }

    // El backend no quita tildes en el autor («angel» no encuentra a «Ángel»): no piden lo mismo.
    @Test
    fun `an author with other accents asks something else`() {
        assertFalse(CourseQuery(author = "Ángel").asksTheSameAs(CourseQuery(author = "angel")))
    }

    @Test
    fun `the same publication date asks the same`() {
        val a = CourseQuery(text = "red", published = PublishedWithin.LAST_YEAR)
        val b = CourseQuery(text = "red", published = PublishedWithin.LAST_YEAR)

        assertTrue(a.asksTheSameAs(b))
    }

    @Test
    fun `a different publication date asks something else`() {
        val month = CourseQuery(published = PublishedWithin.LAST_MONTH)

        assertFalse(month.asksTheSameAs(CourseQuery(published = PublishedWithin.LAST_YEAR)))
        assertFalse(month.asksTheSameAs(CourseQuery.EMPTY))
        assertFalse(CourseQuery.EMPTY.asksTheSameAs(month))
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
