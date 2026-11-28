package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.course.progress.UnenrollOutcome
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

// Petición, respuestas y errores de DELETE /api/progress/enrollments/{courseId} en HttpProgressGateway.
class HttpProgressGatewayUnenrollTest {

    private val courseId = "0b8f6d2e-1111-4111-8111-000000000001"

    private suspend fun sentFor(id: String): HttpRequestData {
        var sent: HttpRequestData? = null
        progressGatewayAnswering {
            sent = it
            respond("", HttpStatusCode.NoContent)
        }.unenroll(testSession, id)
        return sent!!
    }

    private suspend fun outcomeOf(status: HttpStatusCode, body: String = ""): UnenrollOutcome =
        progressGatewayAnswering { json(status, body) }.unenroll(testSession, courseId)

    // ── La petición ──

    @Test
    fun `it sends DELETE to the enrollment route with bearer token and no body`() = runTest {
        val sent = sentFor(courseId)

        assertEquals(HttpMethod.Delete, sent.method)
        assertEquals("/api/progress/enrollments/$courseId", sent.url.encodedPath)
        assertEquals("Bearer $TEST_TOKEN", sent.headers[HttpHeaders.Authorization])
        assertIs<OutgoingContent.NoContent>(sent.body)
        assertNull(sent.body.contentType)
    }

    @Test
    fun `it encodes a slash in courseId as a single path segment`() = runTest {
        val sent = sentFor("../users")

        assertEquals("/api/progress/enrollments/..%2Fusers", sent.url.encodedPath)
    }

    @Test
    fun `it encodes special characters in courseId within a single segment`() = runTest {
        val sent = sentFor("a b?c#d")

        assertEquals("/api/progress/enrollments/a%20b%3Fc%23d", sent.url.encodedPath)
        assertEquals("", sent.url.encodedQuery)
        assertEquals("", sent.url.fragment)
    }

    // ── Las respuestas (sin leer el cuerpo) ──

    @Test
    fun `204 with empty body is unenrolled`() = runTest {
        val gateway = progressGatewayAnswering { respond("", HttpStatusCode.NoContent) }
        assertEquals(UnenrollOutcome.Unenrolled, gateway.unenroll(testSession, courseId))
    }

    @Test
    fun `204 is unenrolled without reading any unexpected body`() = runTest {
        val gateway = progressGatewayAnswering { respond("no es json", HttpStatusCode.NoContent) }
        assertEquals(UnenrollOutcome.Unenrolled, gateway.unenroll(testSession, courseId))
    }

    @Test
    fun `401 is session expired`() = runTest {
        val body = """{"errors": [{"field": "authorization", "code": "UNAUTHENTICATED", "message": "…"}]}"""
        assertEquals(UnenrollOutcome.SessionExpired, outcomeOf(HttpStatusCode.Unauthorized, body))
    }

    @Test
    fun `401 is session expired whatever the body`() = runTest {
        assertEquals(UnenrollOutcome.SessionExpired, outcomeOf(HttpStatusCode.Unauthorized, "basura"))
    }

    @Test
    fun `404 is a server failure`() = runTest {
        val body = """{"errors": [{"field": "courseId", "code": "COURSE_NOT_FOUND", "message": "…"}]}"""
        assertEquals(UnenrollOutcome.ServerFailure, outcomeOf(HttpStatusCode.NotFound, body))
    }

    @Test
    fun `400 is a server failure`() = runTest {
        assertEquals(UnenrollOutcome.ServerFailure, outcomeOf(HttpStatusCode.BadRequest, "{}"))
    }

    @Test
    fun `5xx is a server failure`() = runTest {
        assertEquals(UnenrollOutcome.ServerFailure, outcomeOf(HttpStatusCode.InternalServerError, "{}"))
        assertEquals(UnenrollOutcome.ServerFailure, outcomeOf(HttpStatusCode.ServiceUnavailable, "{}"))
    }

    @Test
    fun `unexpected 200 is a server failure`() = runTest {
        assertEquals(UnenrollOutcome.ServerFailure, outcomeOf(HttpStatusCode.OK, "{}"))
    }

    // ── Excepciones y cancelación (callServer) ──

    @Test
    fun `no connection is unreachable`() = runTest {
        val gateway = progressGatewayAnswering { throw IOException("sin red") }
        assertEquals(UnenrollOutcome.Unreachable, gateway.unenroll(testSession, courseId))
    }

    @Test
    fun `a timeout is unreachable`() = runTest {
        val gateway = progressGatewayAnswering {
            throw HttpRequestTimeoutException("http://test/api/progress/enrollments/$courseId", 10_000)
        }
        assertEquals(UnenrollOutcome.Unreachable, gateway.unenroll(testSession, courseId))
    }

    @Test
    fun `any other exception is a server failure`() = runTest {
        val gateway = progressGatewayAnswering { throw IllegalStateException("raro") }
        assertEquals(UnenrollOutcome.ServerFailure, gateway.unenroll(testSession, courseId))
    }

    @Test
    fun `cancellation exception is rethrown`() = runTest {
        val gateway = progressGatewayAnswering { throw CancellationException("cancelado") }
        assertFailsWith<CancellationException> {
            gateway.unenroll(testSession, courseId)
        }
    }

    // ── Tipo sellado exhaustivo ──

    @Test
    fun `every unenroll outcome is handled exhaustively`() {
        val outcomes = listOf(
            UnenrollOutcome.Unenrolled,
            UnenrollOutcome.SessionExpired,
            UnenrollOutcome.Unreachable,
            UnenrollOutcome.ServerFailure,
        )
        val labels = outcomes.map { outcome ->
            when (outcome) {
                UnenrollOutcome.Unenrolled -> "unenrolled"
                UnenrollOutcome.SessionExpired -> "session-expired"
                UnenrollOutcome.Unreachable -> "unreachable"
                UnenrollOutcome.ServerFailure -> "server-failure"
            }
        }
        assertEquals(listOf("unenrolled", "session-expired", "unreachable", "server-failure"), labels)
    }
}
