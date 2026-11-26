package es.ulpgc.pamn.dominio.submission.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.PhotoSubmission
import es.ulpgc.pamn.dominio.submission.SubmissionGateways
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Entregar una foto (POST /api/courses/{id}/exercises/{exerciseId}/submissions, multipart:
// backend/.../web/submission/SubmissionController; docs/tareas/ejercicio-foto.md, paso 4d).
class HttpSubmissionGatewayTest {

    private val session = Session("tok-123", "2026-12-25T10:00:00Z", Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 13, 10, 45, 45, 7)
    private val photo = PhotoSubmission("0b6f3c2e-8d1a-4f5b-9c7e-2a4d6f8b0c1e", 3, "repr-foto", jpeg)

    private fun gatewayAnswering(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpSubmissionGateway(gradusHttpClient(MockEngine(handler)), baseUrl = "http://test")

    private suspend fun answering(status: HttpStatusCode, body: String = "") =
        gatewayAnswering { json(status, body) }.submit(session, photo)

    private fun error(field: String, code: String) = """{"errors":[{"field":"$field","code":"$code","message":"…"}]}"""

    // Como lo devuelve SubmissionResponse.java (los campos que la app no usa se ignoran).
    private val created = """{"id":"a1b2","courseId":"0b6f","courseVersion":3,"exerciseId":"repr-foto",
        "submittedAt":"2026-11-25T10:00:00.123456Z","status":"PENDING","photoBytes":10}"""

    // ── La petición ──

    @Test
    fun `it posts to the submissions of that exercise with the session token`() = runTest {
        var sent: HttpRequestData? = null
        gatewayAnswering { request -> sent = request; json(HttpStatusCode.Created, created) }.submit(session, photo)

        assertEquals(HttpMethod.Post, sent?.method)
        assertEquals(
            "http://test/api/courses/0b6f3c2e-8d1a-4f5b-9c7e-2a4d6f8b0c1e/exercises/repr-foto/submissions",
            sent?.url.toString(),
        )
        assertEquals("Bearer tok-123", sent?.headers?.get(HttpHeaders.Authorization))
        assertTrue(sent!!.body.contentType.toString().startsWith("multipart/form-data"), sent!!.body.contentType.toString())
    }

    @Test
    fun `the version goes as a text field`() = runTest {
        val body = sentBody()
        val version = Regex("""Content-Disposition: form-data; name="?version"?\r\n(?:[^\r\n]+\r\n)*\r\n3\r\n""")
        assertTrue(version.containsMatchIn(body.decodeToString()), body.decodeToString())
    }

    // El servidor solo ve la foto si llega como fichero (con filename): paso 3e-2.
    @Test
    fun `the photo goes as a JPEG file with a file name`() = runTest {
        val text = sentBody().decodeToString()
        val header = Regex("""Content-Disposition: form-data; name="?photo"?; filename="?foto\.jpg"?""")
        assertTrue(header.containsMatchIn(text), text)
        assertTrue("Content-Type: image/jpeg" in text, text)
    }

    // Byte a byte, también los que parecen saltos de línea o guiones (13, 10, 45, 45).
    @Test
    fun `the photo bytes are sent untouched`() = runTest {
        assertTrue(sentBody().containsSequence(jpeg))
    }

    // Decisión U: 2 MB con mala cobertura pueden tardar más que los 10 s de las peticiones JSON.
    @Test
    fun `the request may take up to a minute`() = runTest {
        var sent: HttpRequestData? = null
        gatewayAnswering { request -> sent = request; json(HttpStatusCode.Created, created) }.submit(session, photo)
        assertEquals(60_000L, sent?.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis)
    }

    // ── Las respuestas ──

    @Test
    fun `201 is a submission with its id and date`() = runTest {
        assertEquals(
            SubmitOutcome.Submitted(Submission(id = "a1b2", submittedAt = "2026-11-25T10:00:00.123456Z")),
            answering(HttpStatusCode.Created, created),
        )
    }

    @Test
    fun `400 INVALID_PHOTO is a photo that is not a JPEG`() = runTest {
        assertEquals(SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG), answering(HttpStatusCode.BadRequest, error("photo", "INVALID_PHOTO")))
    }

    @Test
    fun `400 REQUIRED on the photo is a missing photo`() = runTest {
        assertEquals(SubmitOutcome.PhotoRejected(PhotoRejection.MISSING), answering(HttpStatusCode.BadRequest, error("photo", "REQUIRED")))
    }

    @Test
    fun `413 is a photo that is too large`() = runTest {
        assertEquals(SubmitOutcome.PhotoRejected(PhotoRejection.TOO_LARGE), answering(HttpStatusCode.PayloadTooLarge, error("photo", "PHOTO_TOO_LARGE")))
    }

    // Una versión que falta o no vale, o un multipart roto, es un fallo de la app, no de la foto.
    @Test
    fun `any other 400 is a server failure`() = runTest {
        assertEquals(SubmitOutcome.ServerFailure, answering(HttpStatusCode.BadRequest, error("version", "INVALID_PARAMETER")))
        assertEquals(SubmitOutcome.ServerFailure, answering(HttpStatusCode.BadRequest, error("body", "MALFORMED_BODY")))
    }

    @Test
    fun `404 is a course, version or exercise that no longer exists`() = runTest {
        assertEquals(SubmitOutcome.NotFound, answering(HttpStatusCode.NotFound, error("exerciseId", "EXERCISE_NOT_FOUND")))
        assertEquals(SubmitOutcome.NotFound, answering(HttpStatusCode.NotFound, error("id", "COURSE_NOT_FOUND")))
    }

    @Test
    fun `401 is an expired session`() = runTest {
        assertEquals(SubmitOutcome.SessionExpired, answering(HttpStatusCode.Unauthorized, error("authorization", "UNAUTHENTICATED")))
    }

    @Test
    fun `an unexpected status is a server failure`() = runTest {
        assertEquals(SubmitOutcome.ServerFailure, answering(HttpStatusCode.InternalServerError))
        assertEquals(SubmitOutcome.ServerFailure, answering(HttpStatusCode.OK, created))
    }

    @Test
    fun `a body that cannot be read is a server failure`() = runTest {
        assertEquals(SubmitOutcome.ServerFailure, answering(HttpStatusCode.Created, "<html>Error</html>"))
        assertEquals(SubmitOutcome.ServerFailure, answering(HttpStatusCode.BadRequest, "{}"))
    }

    // Más de 2 MiB: el servidor responde 413 sin leerla y corta la conexión, y OkHttp lo vería como un
    // fallo de red (comprobado contra el servidor real). Por eso no se envía: se rechaza aquí.
    @Test
    fun `a photo over the maximum is too large without sending anything`() = runTest {
        var requests = 0
        val tooLarge = PhotoSubmission(photo.courseId, 3, "repr-foto", ByteArray(PhotoSubmission.MAX_BYTES + 1))
        val outcome = gatewayAnswering { requests++; json(HttpStatusCode.Created, created) }.submit(session, tooLarge)

        assertEquals(SubmitOutcome.PhotoRejected(PhotoRejection.TOO_LARGE), outcome)
        assertEquals(0, requests)
    }

    @Test
    fun `a photo of exactly the maximum is sent`() = runTest {
        val largest = PhotoSubmission(photo.courseId, 3, "repr-foto", ByteArray(PhotoSubmission.MAX_BYTES))
        assertIs<SubmitOutcome.Submitted>(gatewayAnswering { json(HttpStatusCode.Created, created) }.submit(session, largest))
    }

    @Test
    fun `without connection the server is unreachable`() = runTest {
        assertEquals(SubmitOutcome.Unreachable, gatewayAnswering { throw IOException("sin red") }.submit(session, photo))
    }

    @Test
    fun `running out of time is unreachable`() = runTest {
        assertEquals(SubmitOutcome.Unreachable, gatewayAnswering { throw HttpRequestTimeoutException("url", 60_000) }.submit(session, photo))
    }

    @Test
    fun `http builds the HTTP adapter`() {
        assertIs<HttpSubmissionGateway>(SubmissionGateways.http("http://10.0.2.2:7070"))
    }

    // ── Ayudas ──

    private suspend fun sentBody(): ByteArray {
        var body = ByteArray(0)
        gatewayAnswering { request -> body = request.body.toByteArray(); json(HttpStatusCode.Created, created) }.submit(session, photo)
        return body
    }

    private fun ByteArray.containsSequence(part: ByteArray): Boolean =
        (0..size - part.size).any { start -> part.indices.all { this[start + it] == part[it] } }
}
