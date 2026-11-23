package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// El gateway falso de las previews y los tests: busca y devuelve los cursos que se le dan.
class FakeCourseGatewayTest {

    private val graphs = Course("g", "Introducción a los grafos", listOf(Module("m1", "Uno"), Module("m2", "Dos")), description = "BFS y DFS")
    private val trees = Course("t", "Árboles", listOf(Module("m1", "Uno")))
    private val gateway = FakeCourseGateway(listOf(graphs, trees))

    @Test
    fun `it finds the courses whose title or description contain the text, ignoring case`() = runTest {
        assertEquals(listOf("g"), found(gateway.search("GRAFOS")).map { it.id })
        assertEquals(listOf("g"), found(gateway.search("dfs")).map { it.id })
        assertEquals(listOf("g", "t"), found(gateway.search("")).map { it.id })
    }

    @Test
    fun `each result describes its course`() = runTest {
        val result = found(gateway.search("grafos")).single()
        assertEquals("Introducción a los grafos", result.title)
        assertEquals(2, result.moduleCount)
    }

    @Test
    fun `it gives a course by its id, or not found`() = runTest {
        assertEquals(CourseOutcome.Found(trees), gateway.course("t"))
        assertEquals(CourseOutcome.NotFound, gateway.course("no-existe"))
    }

    @Test
    fun `it can be told what to answer`() = runTest {
        val offline = FakeCourseGateway(searchAnswer = { _, _ -> SearchOutcome.Unreachable }, courseAnswer = { CourseOutcome.Unreachable })
        assertEquals(SearchOutcome.Unreachable, offline.search("x"))
        assertEquals(CourseOutcome.Unreachable, offline.course("x"))
    }

    @Test
    fun `it remembers what it was asked`() = runTest {
        gateway.search("grafos", page = 1)
        gateway.course("t")
        assertEquals(listOf(CourseQuery(text = "grafos") to 1), gateway.searches)
        assertEquals(listOf("t"), gateway.requestedCourses)
    }

    // El curso debe tener todas las etiquetas pedidas, no solo alguna.
    @Test
    fun `it only finds the courses that have all the tags`() = runTest {
        val tagged = FakeCourseGateway(
            listOf(
                Course("a", "A", emptyList(), tags = listOf("grafos", "árboles")),
                Course("b", "B", emptyList(), tags = listOf("grafos")),
            ),
        )
        assertEquals(listOf("a", "b"), found(tagged.search(CourseQuery().toggleTag("grafos"))).map { it.id })
        assertEquals(listOf("a"), found(tagged.search(CourseQuery().toggleTag("grafos").toggleTag("árboles"))).map { it.id })
    }

    @Test
    fun `tags are compared ignoring case and extra spaces`() = runTest {
        val tagged = FakeCourseGateway(listOf(Course("a", "A", emptyList(), tags = listOf("Grafos"))))
        assertEquals(listOf("a"), found(tagged.search(CourseQuery().toggleTag("  grafos "))).map { it.id })
    }

    // ── Creador y fecha ──

    @Test
    fun `an author that is part of the name finds every course`() = runTest {
        assertEquals(listOf("g", "t"), found(gateway.search(CourseQuery(author = "PRUEBA"))).map { it.id })
    }

    @Test
    fun `an author that is not in the name finds nothing`() = runTest {
        assertTrue(found(gateway.search(CourseQuery(author = "luis"))).isEmpty())
    }

    @Test
    fun `a blank author does not filter`() = runTest {
        assertEquals(listOf("g", "t"), found(gateway.search(CourseQuery(author = "   "))).map { it.id })
    }

    @Test
    fun `the author is compared ignoring case and extra spaces`() = runTest {
        assertEquals(listOf("g", "t"), found(gateway.search(CourseQuery(author = " autor   DE prueba "))).map { it.id })
        assertTrue(found(gateway.search(CourseQuery(author = "autor  de  luis"))).isEmpty())
    }

    // Como el backend: no quita tildes.
    @Test
    fun `the author is compared without removing accents`() = runTest {
        assertEquals(listOf("g", "t"), found(gateway.search(CourseQuery(author = "prueba"))).map { it.id })
        assertTrue(found(gateway.search(CourseQuery(author = "pruébá"))).isEmpty())
    }

    @Test
    fun `the author and the text must both match`() = runTest {
        assertEquals(listOf("t"), found(gateway.search(CourseQuery(text = "árboles", author = "prueba"))).map { it.id })
        assertTrue(found(gateway.search(CourseQuery(text = "árboles", author = "luis"))).isEmpty())
    }

    // Sin reloj en commonMain no se puede filtrar por fecha (haría falta kotlinx-datetime): se ignora.
    @Test
    fun `the published date is ignored`() = runTest {
        PublishedWithin.entries.forEach { within ->
            assertEquals(listOf("g", "t"), found(gateway.search(CourseQuery(published = within))).map { it.id })
        }
    }

    @Test
    fun `every result has the fixed test author`() = runTest {
        assertEquals(listOf("Autor de prueba"), found(gateway.search("")).map { it.author.name }.distinct())
    }

    @Test
    fun `with no courses every search is empty`() = runTest {
        assertTrue(found(FakeCourseGateway().search("x")).isEmpty())
    }

    private fun found(outcome: SearchOutcome): List<CourseSummary> {
        assertIs<SearchOutcome.Found>(outcome)
        return outcome.page.items
    }
}
