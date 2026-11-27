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

    @Test
    fun `invalidating a cached course forces the next read to fetch from the server`() = runTest {
        val updated = graphs.copy(title = "Grafos v2")
        var current = graphs
        val server = FakeCourseGateway(courseAnswer = { CourseOutcome.Found(current) })
        val gateway = CachingCourseGateway(server)

        assertEquals(CourseOutcome.Found(graphs), gateway.course("g"))
        current = updated
        gateway.invalidate("g")

        assertNull(gateway.cached("g"))
        assertEquals(CourseOutcome.Found(updated), gateway.course("g"))
        assertEquals(updated, gateway.cached("g"))
        assertEquals(listOf("g", "g"), server.requestedCourses)
    }

    @Test
    fun `invalidating one course keeps other cached courses untouched`() = runTest {
        val trees = Course("t", "Árboles", listOf(Module("m1", "Raíz")))
        val server = FakeCourseGateway(listOf(graphs, trees))
        val gateway = CachingCourseGateway(server)

        gateway.course("g")
        gateway.course("t")
        gateway.invalidate("g")

        assertNull(gateway.cached("g"))
        assertEquals(trees, gateway.cached("t"))
        assertEquals(CourseOutcome.Found(trees), gateway.course("t"))
        assertEquals(listOf("g", "t"), server.requestedCourses)
    }

    @Test
    fun `invalidating an uncached course is idempotent and leaves cache intact`() = runTest {
        val server = FakeCourseGateway(listOf(graphs))
        val gateway = CachingCourseGateway(server)

        gateway.course("g")
        gateway.invalidate("missing")
        gateway.invalidate("missing")

        assertEquals(graphs, gateway.cached("g"))
    }

    @Test
    fun `caching gateway works through the CourseCacheInvalidator functional interface`() = runTest {
        val server = FakeCourseGateway(listOf(graphs))
        val gateway = CachingCourseGateway(server)
        val invalidator: CourseCacheInvalidator = gateway

        gateway.course("g")
        invalidator.invalidate("g")

        assertNull(gateway.cached("g"))
    }
}
