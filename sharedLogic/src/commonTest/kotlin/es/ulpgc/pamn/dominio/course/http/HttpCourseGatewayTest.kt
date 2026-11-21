package es.ulpgc.pamn.dominio.course.http

import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.SearchOutcome
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Búsqueda (GET /api/courses) y descarga (GET /api/courses/{id}) con un servidor falso, sin red.
class HttpCourseGatewayTest {

    private fun gatewayAnswering(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpCourseGateway(gradusHttpClient(MockEngine(handler)), baseUrl = "http://test")

    // ── Búsqueda: la petición ──

    @Test
    fun `search asks for the text and the page, with the page size`() = runTest {
        var sent: HttpRequestData? = null
        gatewayAnswering { sent = it; json(HttpStatusCode.OK, EMPTY_PAGE) }.search("grafos y árboles", page = 2)

        assertEquals(HttpMethod.Get, sent?.method)
        assertEquals("/api/courses", sent?.url?.encodedPath)
        assertEquals("grafos y árboles", sent?.url?.parameters?.get("q"))
        assertEquals("2", sent?.url?.parameters?.get("page"))
        assertEquals("20", sent?.url?.parameters?.get("size"))
    }

    // Sin texto se piden todos los cursos: no se manda «q».
    @Test
    fun `a blank search does not send the text`() = runTest {
        var sent: HttpRequestData? = null
        gatewayAnswering { sent = it; json(HttpStatusCode.OK, EMPTY_PAGE) }.search("  ")
        assertTrue("q" !in sent!!.url.parameters.names())
    }

    // ── Búsqueda: las respuestas ──

    @Test
    fun `200 is found with the page of results`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.OK, ONE_RESULT_PAGE) }.search("grafos")
        assertIs<SearchOutcome.Found>(outcome)
        assertEquals(listOf("Introducción a los grafos"), outcome.page.items.map { it.title })
        assertEquals("Ana", outcome.page.items.single().author.name)
    }

    @Test
    fun `an empty page is found with no results`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.OK, EMPTY_PAGE) }.search("nada")
        assertIs<SearchOutcome.Found>(outcome)
        assertTrue(outcome.page.items.isEmpty())
    }

    // 400 INVALID_PARAMETER solo pasa si la app pide una página imposible: es un fallo, no del usuario.
    @Test
    fun `400 on search is a server failure`() = runTest {
        val body = """{"errors": [{"field": "page", "code": "INVALID_PARAMETER", "message": "…"}]}"""
        assertEquals(SearchOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.BadRequest, body) }.search("x", page = -1))
    }

    @Test
    fun `5xx on search is a server failure`() = runTest {
        assertEquals(SearchOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.search("x"))
    }

    @Test
    fun `a page that cannot be read is a server failure`() = runTest {
        assertEquals(SearchOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.OK, "[1, 2]") }.search("x"))
    }

    @Test
    fun `no connection on search is unreachable`() = runTest {
        assertEquals(SearchOutcome.Unreachable, gatewayAnswering { throw IOException("sin red") }.search("x"))
    }

    // ── Descarga de un curso ──

    @Test
    fun `course asks for the course by its id`() = runTest {
        var sent: HttpRequestData? = null
        gatewayAnswering { sent = it; json(HttpStatusCode.OK, EXAMPLE_COURSE_RESPONSE) }.course("0b8f-1")
        assertEquals(HttpMethod.Get, sent?.method)
        assertEquals("/api/courses/0b8f-1", sent?.url?.encodedPath)
    }

    // Un id raro no puede cambiar la ruta pedida (p. ej. «../users»): se codifica como un solo trozo.
    @Test
    fun `the id is sent as a single path segment`() = runTest {
        var sent: HttpRequestData? = null
        gatewayAnswering { sent = it; json(HttpStatusCode.NotFound, "{}") }.course("../users")
        assertEquals("/api/courses/..%2Fusers", sent?.url?.encodedPath)
    }

    @Test
    fun `200 is found with the course`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.OK, EXAMPLE_COURSE_RESPONSE) }.course("x")
        assertIs<CourseOutcome.Found>(outcome)
        assertEquals(listOf("representacion", "bfs"), outcome.course.modules.map { it.id })
    }

    @Test
    fun `404 is not found`() = runTest {
        val body = """{"errors": [{"field": "id", "code": "COURSE_NOT_FOUND", "message": "…"}]}"""
        assertEquals(CourseOutcome.NotFound, gatewayAnswering { json(HttpStatusCode.NotFound, body) }.course("x"))
    }

    @Test
    fun `a course in a newer format is unsupported`() = runTest {
        val body = """{"id": "x", "version": 1, "content": {"formatVersion": 2}}"""
        assertEquals(CourseOutcome.UnsupportedFormat, gatewayAnswering { json(HttpStatusCode.OK, body) }.course("x"))
    }

    @Test
    fun `a course that cannot be read is a server failure`() = runTest {
        val body = """{"id": "x", "version": 1, "content": {"formatVersion": 1, "title": "Sin módulos"}}"""
        assertEquals(CourseOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.OK, body) }.course("x"))
    }

    @Test
    fun `5xx on a course is a server failure`() = runTest {
        assertEquals(CourseOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.ServiceUnavailable, "{}") }.course("x"))
    }

    @Test
    fun `no connection on a course is unreachable`() = runTest {
        assertEquals(CourseOutcome.Unreachable, gatewayAnswering { throw IOException("sin red") }.course("x"))
    }

    private companion object {
        const val EMPTY_PAGE = """{"items": [], "page": 0, "size": 20, "total": 0}"""

        const val ONE_RESULT_PAGE = """
            {"items": [{"id": "c1", "version": 1, "author": {"id": "u1", "name": "Ana"},
                        "publishedAt": "2026-11-18T08:35:00Z", "formatVersion": 1,
                        "title": "Introducción a los grafos", "description": "…", "language": "es",
                        "tags": ["grafos"], "moduleCount": 2, "sha256": "9c1e"}],
             "page": 0, "size": 20, "total": 1}
        """
    }
}
