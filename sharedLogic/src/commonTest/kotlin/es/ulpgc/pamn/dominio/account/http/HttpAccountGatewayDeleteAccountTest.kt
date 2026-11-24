package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.DeleteAccountOutcome
import es.ulpgc.pamn.dominio.account.ServerFieldError
import es.ulpgc.pamn.dominio.account.Session
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// Eliminar la cuenta (DELETE /api/users/me, backend/.../web/UserController.deleteAccount).
class HttpAccountGatewayDeleteAccountTest {

    private val session = Session("tok-123", "2026-12-22T10:00:00Z", Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))

    // ── La petición ──

    // DELETE con cuerpo (decisión J): la contraseña confirma que es su dueño quien lo pide.
    @Test
    fun `it deletes the account of the token sending the password`() = runTest {
        var sent: HttpRequestData? = null
        var sentBody = ""
        gatewayAnswering { request ->
            sent = request
            sentBody = request.body.toByteArray().decodeToString()
            respond("", HttpStatusCode.NoContent)
        }.deleteAccount(session, "Gradus2026!")

        assertEquals(HttpMethod.Delete, sent?.method)
        assertEquals("http://test/api/users/me", sent?.url.toString())
        assertEquals("Bearer tok-123", sent?.headers?.get(HttpHeaders.Authorization))
        // Mismos campos que DeleteAccountRequest.java.
        assertEquals(Json.parseToJsonElement("""{"password":"Gradus2026!"}"""), Json.parseToJsonElement(sentBody))
    }

    // ── Las respuestas ──

    @Test
    fun `204 is deleted`() = runTest {
        assertEquals(DeleteAccountOutcome.Deleted, gatewayAnswering { respond("", HttpStatusCode.NoContent) }.deleteAccount(session, "Gradus2026!"))
    }

    @Test
    fun `400 is rejected`() = runTest {
        val body = """{"errors": [{"field": "password", "code": "REQUIRED", "message": "…"}]}"""
        assertEquals(
            DeleteAccountOutcome.Rejected(listOf(ServerFieldError("password", "REQUIRED"))),
            gatewayAnswering { json(HttpStatusCode.BadRequest, body) }.deleteAccount(session, ""),
        )
    }

    @Test
    fun `403 is a wrong password`() = runTest {
        val body = """{"errors": [{"field": "password", "code": "WRONG_PASSWORD", "message": "…"}]}"""
        assertEquals(DeleteAccountOutcome.WrongPassword, gatewayAnswering { json(HttpStatusCode.Forbidden, body) }.deleteAccount(session, "x"))
    }

    // Decisión I: quien ha publicado cursos no puede eliminar la cuenta.
    @Test
    fun `409 is the author of courses`() = runTest {
        val body = """{"errors": [{"field": "account", "code": "AUTHOR_OF_COURSES", "message": "…"}]}"""
        assertEquals(DeleteAccountOutcome.AuthorOfCourses, gatewayAnswering { json(HttpStatusCode.Conflict, body) }.deleteAccount(session, "Gradus2026!"))
    }

    @Test
    fun `401 is an expired session`() = runTest {
        assertEquals(DeleteAccountOutcome.SessionExpired, gatewayAnswering { json(HttpStatusCode.Unauthorized, "{}") }.deleteAccount(session, "Gradus2026!"))
    }

    @Test
    fun `5xx is a server failure`() = runTest {
        assertEquals(DeleteAccountOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.deleteAccount(session, "Gradus2026!"))
    }

    @Test
    fun `no connection is unreachable`() = runTest {
        assertEquals(DeleteAccountOutcome.Unreachable, gatewayAnswering { throw IOException("sin red") }.deleteAccount(session, "Gradus2026!"))
    }

    @Test
    fun `a timeout is unreachable`() = runTest {
        assertEquals(DeleteAccountOutcome.Unreachable, gatewayAnswering { throw HttpRequestTimeoutException("url", 10_000) }.deleteAccount(session, "Gradus2026!"))
    }

    @Test
    fun `the password never appears in toString`() {
        assertFalse(DeleteAccountRequestDto("Gradus2026!").toString().contains("Gradus2026!"))
    }
}
