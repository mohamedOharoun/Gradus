package es.ulpgc.pamn.dominio.course

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
        assertEquals(listOf("grafos" to 1), gateway.searches)
        assertEquals(listOf("t"), gateway.requestedCourses)
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
