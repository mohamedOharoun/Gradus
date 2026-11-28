package es.ulpgc.pamn.dominio.course

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Un curso descargado se guarda en memoria: el camino y la práctica lo comparten sin volver a pedirlo.
class CachingCourseGatewayTest {

    private val graphs = Course("g", "Grafos", listOf(Module("m1", "Uno")))

    @Test
    fun `a downloaded course is asked to the server only once`() = runTest {
        val server = FakeCourseGateway(listOf(graphs))
        val gateway = CachingCourseGateway(server)

        assertEquals(CourseOutcome.Found(graphs), gateway.course("g"))
        assertEquals(CourseOutcome.Found(graphs), gateway.course("g"))
        assertEquals(listOf("g"), server.requestedCourses)
    }

    @Test
    fun `a downloaded course can be read without waiting`() = runTest {
        val gateway = CachingCourseGateway(FakeCourseGateway(listOf(graphs)))
        assertNull(gateway.cached("g"))
        gateway.course("g")
        assertEquals(graphs, gateway.cached("g"))
    }

    // Un fallo no se guarda: al reintentar, se vuelve a preguntar al servidor.
    @Test
    fun `failures are not kept`() = runTest {
        val server = FakeCourseGateway(courseAnswer = { CourseOutcome.Unreachable })
        val gateway = CachingCourseGateway(server)
        gateway.course("g")
        gateway.course("g")
        assertEquals(listOf("g", "g"), server.requestedCourses)
        assertNull(gateway.cached("g"))
    }

    // La búsqueda no se guarda: siempre se pregunta (pueden haberse publicado cursos nuevos).
    @Test
    fun `search always asks the server`() = runTest {
        val server = FakeCourseGateway(listOf(graphs))
        val gateway = CachingCourseGateway(server)
        gateway.search("grafos")
        gateway.search("grafos")
        assertEquals(2, server.searches.size)
    }
}
