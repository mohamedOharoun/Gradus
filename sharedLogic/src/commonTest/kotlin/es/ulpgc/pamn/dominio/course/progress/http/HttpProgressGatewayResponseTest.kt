package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.course.progress.SkippedProgress
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// Cómo se traduce cada respuesta (o fallo) de POST /api/progress/sync a un SyncOutcome.
class HttpProgressGatewayResponseTest {

    private suspend fun outcomeOf(status: HttpStatusCode, body: String): SyncOutcome =
        progressGatewayAnswering { json(status, body) }.sync(testSession, noPending)

    // ── 200 ──

    @Test
    fun `200 is synced with what was read`() = runTest {
        val outcome = outcomeOf(HttpStatusCode.OK, SYNC_OK_JSON)
        assertIs<SyncOutcome.Synced>(outcome)
        val enrollment = outcome.response.remote.enrollments.single()
        assertEquals("0b8f6d2e", enrollment.enrollment.courseId)
        assertEquals(listOf("bfs"), enrollment.mastered.map { it.moduleId })
        assertEquals(
            listOf(
                SkippedProgress.WholeEnrollment("9a1c", "COURSE_NOT_FOUND"),
                SkippedProgress.SingleModule("0b8f6d2e", " ", "REQUIRED"),
            ),
            outcome.response.skipped,
        )
    }

    @Test
    fun `a 200 that cannot be read is a server failure`() = runTest {
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.OK, """{"skipped": []}"""))
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.OK, "no es json"))
    }

    // El servidor manda application/json sin charset: el texto debe leerse como UTF-8.
    @Test
    fun `a 200 with accents is read as UTF-8`() = runTest {
        val body = """{"enrollments": [{"courseId": "c1", "enrolledAt": "2026-11-24T10:00:00Z",
            "mastered": [{"moduleId": "señal-árbol", "masteredAt": "2026-11-24T10:20:00Z"}]}], "skipped": []}"""
        val gateway = progressGatewayAnswering {
            respond(
                body.encodeToByteArray(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val outcome = gateway.sync(testSession, noPending)
        assertIs<SyncOutcome.Synced>(outcome)
        assertEquals(listOf("señal-árbol"), outcome.response.remote.enrollments.single().mastered.map { it.moduleId })
    }

    // ── Estado antes que cuerpo ──

    @Test
    fun `401 is session expired`() = runTest {
        val body = """{"errors": [{"field": "authorization", "code": "UNAUTHENTICATED", "message": "…"}]}"""
        assertEquals(SyncOutcome.SessionExpired, outcomeOf(HttpStatusCode.Unauthorized, body))
    }

    // El estado manda: aunque el cuerpo no sea JSON, un 401 sigue siendo sesión caducada.
    @Test
    fun `401 is session expired whatever the body`() = runTest {
        assertEquals(SyncOutcome.SessionExpired, outcomeOf(HttpStatusCode.Unauthorized, "basura"))
    }

    @Test
    fun `400 is a server failure`() = runTest {
        val body = """{"errors": [{"field": "body", "code": "MALFORMED_BODY", "message": "…"}]}"""
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.BadRequest, body))
    }

    // Javalin responde el 413 con text/plain, no con el formato de errores.
    @Test
    fun `413 in plain text is a server failure`() = runTest {
        val gateway = progressGatewayAnswering {
            respond("Content Too Large", HttpStatusCode.PayloadTooLarge, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        assertEquals(SyncOutcome.ServerFailure, gateway.sync(testSession, noPending))
    }

    @Test
    fun `5xx is a server failure`() = runTest {
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.InternalServerError, "{}"))
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.ServiceUnavailable, "{}"))
    }

    // Solo el 200 se lee: un 5xx con un cuerpo válido no se convierte en Synced.
    @Test
    fun `a valid body is not read when the status is not 200`() = runTest {
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.InternalServerError, SYNC_OK_JSON))
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.BadRequest, SYNC_OK_JSON))
    }

    @Test
    fun `an unknown status is a server failure`() = runTest {
        assertEquals(SyncOutcome.ServerFailure, outcomeOf(HttpStatusCode.NotFound, "{}"))
    }

    // ── Excepciones (callServer) ──

    @Test
    fun `no connection is unreachable`() = runTest {
        val gateway = progressGatewayAnswering { throw IOException("sin red") }
        assertEquals(SyncOutcome.Unreachable, gateway.sync(testSession, noPending))
    }

    @Test
    fun `a timeout is unreachable`() = runTest {
        val gateway = progressGatewayAnswering { throw HttpRequestTimeoutException("http://test/api/progress/sync", 10_000) }
        assertEquals(SyncOutcome.Unreachable, gateway.sync(testSession, noPending))
    }

    @Test
    fun `any other exception is a server failure`() = runTest {
        val gateway = progressGatewayAnswering { throw IllegalStateException("raro") }
        assertEquals(SyncOutcome.ServerFailure, gateway.sync(testSession, noPending))
    }
}
