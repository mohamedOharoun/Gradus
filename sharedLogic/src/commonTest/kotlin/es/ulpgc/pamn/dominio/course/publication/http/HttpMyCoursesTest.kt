package es.ulpgc.pamn.dominio.course.publication.http

import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.publication.CoursePublicationGateways
import es.ulpgc.pamn.dominio.course.publication.MyCoursesOutcome
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Listar cursos propios (GET /api/users/me/courses) y factory CoursePublicationGateways.
class HttpMyCoursesTest {

    @Test
    fun `my courses sends GET to users me courses with bearer token and default timeout`() = runTest {
        var sent: HttpRequestData? = null
        publicationGatewayAnswering { request ->
            sent = request
            json(HttpStatusCode.OK, """{"courses": []}""")
        }.myCourses(TEST_SESSION)

        assertEquals(HttpMethod.Get, sent?.method)
        assertEquals("/api/users/me/courses", sent?.url?.encodedPath)
        assertEquals("Bearer $TEST_TOKEN", sent?.headers?.get(HttpHeaders.Authorization))
        assertEquals(10_000L, sent?.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis)
    }

    @Test
    fun `200 returns found with author course summaries`() = runTest {
        val body = """{"courses": [$SUMMARY_JSON]}"""
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.OK, body) }.myCourses(TEST_SESSION)

        assertEquals(
            MyCoursesOutcome.Found(
                listOf(
                    CourseSummary(
                        id = "0b8f",
                        version = 2,
                        author = Author("5c1d", "Ana"),
                        publishedAt = "2026-11-18T08:35:00Z",
                        title = "Introducción a los grafos",
                        description = "Recorridos básicos",
                        language = "es",
                        tags = listOf("grafos", "algoritmos"),
                        moduleCount = 2,
                    ),
                ),
            ),
            outcome,
        )
    }

    @Test
    fun `200 with empty list returns found with no courses`() = runTest {
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.OK, """{"courses": []}""") }
            .myCourses(TEST_SESSION)

        val found = assertIs<MyCoursesOutcome.Found>(outcome)
        assertTrue(found.courses.isEmpty())
    }

    @Test
    fun `200 ignores unknown extra fields such as future course metrics`() = runTest {
        val body = """{"courses": [$SUMMARY_WITH_EXTRA_METRICS_JSON], "extra": true}"""
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.OK, body) }.myCourses(TEST_SESSION)

        val found = assertIs<MyCoursesOutcome.Found>(outcome)
        assertEquals("0b8f", found.courses.single().id)
    }

    @Test
    fun `401 is session expired`() = runTest {
        val outcome = publicationGatewayAnswering {
            json(HttpStatusCode.Unauthorized, apiErrorJson("authorization", "UNAUTHENTICATED"))
        }.myCourses(TEST_SESSION)

        assertEquals(MyCoursesOutcome.SessionExpired, outcome)
    }

    @Test
    fun `500 and unexpected statuses are server failure`() = runTest {
        assertEquals(
            MyCoursesOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.myCourses(TEST_SESSION),
        )
        assertEquals(
            MyCoursesOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.Forbidden, "{}") }.myCourses(TEST_SESSION),
        )
    }

    @Test
    fun `malformed 200 body is server failure`() = runTest {
        assertEquals(
            MyCoursesOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.OK, "<html>error</html>") }.myCourses(TEST_SESSION),
        )
        assertEquals(
            MyCoursesOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.OK, "{}") }.myCourses(TEST_SESSION),
        )
    }

    @Test
    fun `network error and timeout are unreachable`() = runTest {
        assertEquals(
            MyCoursesOutcome.Unreachable,
            publicationGatewayAnswering { throw IOException("sin red") }.myCourses(TEST_SESSION),
        )
        assertEquals(
            MyCoursesOutcome.Unreachable,
            publicationGatewayAnswering { throw HttpRequestTimeoutException("url", 10_000) }.myCourses(TEST_SESSION),
        )
    }

    @Test
    fun `session token never appears in my courses outcomes`() = runTest {
        val outcomes = listOf(
            publicationGatewayAnswering { json(HttpStatusCode.OK, """{"courses": [$SUMMARY_JSON]}""") }.myCourses(TEST_SESSION),
            publicationGatewayAnswering { json(HttpStatusCode.Unauthorized, "{}") }.myCourses(TEST_SESSION),
            publicationGatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.myCourses(TEST_SESSION),
            publicationGatewayAnswering { throw IOException("sin red") }.myCourses(TEST_SESSION),
        )

        outcomes.forEach { outcome ->
            assertFalse(TEST_TOKEN in outcome.toString())
        }
    }

    @Test
    fun `http factory builds HttpCoursePublicationGateway`() {
        assertIs<HttpCoursePublicationGateway>(CoursePublicationGateways.http("http://localhost:7070"))
    }
}
