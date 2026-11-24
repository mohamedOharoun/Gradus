package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.ChangePasswordOutcome
import es.ulpgc.pamn.dominio.account.PasswordChange
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

// Cambiar la contraseña (PUT /api/users/me/password, backend/.../web/UserController.changePassword).
class HttpAccountGatewayChangePasswordTest {

    private val session = Session("tok-123", "2026-12-22T10:00:00Z", Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))
    private val change = PasswordChange(currentPassword = "Gradus2026!", newPassword = "Nueva2026?")

    // ── La petición ──

    @Test
    fun `it puts both passwords with the token`() = runTest {
        var sent: HttpRequestData? = null
        var sentBody = ""
        gatewayAnswering { request ->
            sent = request
            sentBody = request.body.toByteArray().decodeToString()
            respond("", HttpStatusCode.NoContent)
        }.changePassword(session, change)

        assertEquals(HttpMethod.Put, sent?.method)
        assertEquals("http://test/api/users/me/password", sent?.url.toString())
        assertEquals("Bearer tok-123", sent?.headers?.get(HttpHeaders.Authorization))
        // Mismos campos que ChangePasswordRequest.java.
        assertEquals(
            Json.parseToJsonElement("""{"currentPassword":"Gradus2026!","newPassword":"Nueva2026?"}"""),
            Json.parseToJsonElement(sentBody),
        )
    }

    // ── Las respuestas ──

    @Test
    fun `204 is changed`() = runTest {
        assertEquals(ChangePasswordOutcome.Changed, gatewayAnswering { respond("", HttpStatusCode.NoContent) }.changePassword(session, change))
    }

    @Test
    fun `400 is rejected with the errors of each field`() = runTest {
        val body = """{"errors": [{"field": "newPassword", "code": "TOO_SHORT", "message": "…"},
                                  {"field": "newPassword", "code": "MISSING_DIGIT", "message": "…"}]}"""
        assertEquals(
            ChangePasswordOutcome.Rejected(listOf(ServerFieldError("newPassword", "TOO_SHORT"), ServerFieldError("newPassword", "MISSING_DIGIT"))),
            gatewayAnswering { json(HttpStatusCode.BadRequest, body) }.changePassword(session, change),
        )
    }

    @Test
    fun `403 is a wrong current password`() = runTest {
        val body = """{"errors": [{"field": "currentPassword", "code": "WRONG_PASSWORD", "message": "…"}]}"""
        assertEquals(ChangePasswordOutcome.WrongPassword, gatewayAnswering { json(HttpStatusCode.Forbidden, body) }.changePassword(session, change))
    }

    @Test
    fun `401 is an expired session`() = runTest {
        assertEquals(ChangePasswordOutcome.SessionExpired, gatewayAnswering { json(HttpStatusCode.Unauthorized, "{}") }.changePassword(session, change))
    }

    @Test
    fun `5xx is a server failure`() = runTest {
        assertEquals(ChangePasswordOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.changePassword(session, change))
    }

    @Test
    fun `no connection is unreachable`() = runTest {
        assertEquals(ChangePasswordOutcome.Unreachable, gatewayAnswering { throw IOException("sin red") }.changePassword(session, change))
    }

    @Test
    fun `a timeout is unreachable`() = runTest {
        assertEquals(ChangePasswordOutcome.Unreachable, gatewayAnswering { throw HttpRequestTimeoutException("url", 10_000) }.changePassword(session, change))
    }

    @Test
    fun `the passwords never appear in toString`() {
        listOf(change.toString(), ChangePasswordRequestDto("Gradus2026!", "Nueva2026?").toString()).forEach {
            assertFalse(it.contains("Gradus2026!"))
            assertFalse(it.contains("Nueva2026?"))
        }
    }
}
