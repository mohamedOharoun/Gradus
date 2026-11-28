package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.RenameOutcome
import es.ulpgc.pamn.dominio.account.ServerFieldError
import es.ulpgc.pamn.dominio.account.Session
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

// Cambiar el nombre (PATCH /api/users/me, backend/.../web/UserController.rename). Ajustes, fase 2.
class HttpAccountGatewayRenameTest {

    private val session = Session("tok-123", "2026-12-22T10:00:00Z", Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))

    // ── La petición ──

    @Test
    fun `it patches the account of the token with the new name`() = runTest {
        var sent: HttpRequestData? = null
        var sentBody = ""
        gatewayAnswering { request ->
            sent = request
            sentBody = request.body.toByteArray().decodeToString()
            json(HttpStatusCode.OK, USER_JSON)
        }.rename(session, "Ana Ruiz")

        assertEquals(HttpMethod.Patch, sent?.method)
        assertEquals("http://test/api/users/me", sent?.url.toString())
        assertEquals("Bearer tok-123", sent?.headers?.get(HttpHeaders.Authorization))
        // Mismos campos que RenameUserRequest.java.
        assertEquals(Json.parseToJsonElement("""{"name":"Ana Ruiz"}"""), Json.parseToJsonElement(sentBody))
    }

    // ── Las respuestas ──

    @Test
    fun `200 is renamed with the account as the server saved it`() = runTest {
        val body = """{"id": "7f3c", "name": "Ana Ruiz", "email": "ana@ulpgc.es", "createdAt": "2026-11-17T10:00:00Z"}"""
        assertEquals(
            RenameOutcome.Renamed(Account("7f3c", "Ana Ruiz", "ana@ulpgc.es", "2026-11-17T10:00:00Z")),
            gatewayAnswering { json(HttpStatusCode.OK, body) }.rename(session, " Ana Ruiz "),
        )
    }

    @Test
    fun `400 is rejected with the errors of the name`() = runTest {
        val body = """{"errors": [{"field": "name", "code": "TOO_SHORT", "message": "…"}]}"""
        assertEquals(
            RenameOutcome.Rejected(listOf(ServerFieldError("name", "TOO_SHORT"))),
            gatewayAnswering { json(HttpStatusCode.BadRequest, body) }.rename(session, "A"),
        )
    }

    // La sesión ya no vale (caducada, cerrada desde otro móvil al cambiar la contraseña…).
    @Test
    fun `401 is an expired session`() = runTest {
        val body = """{"errors": [{"field": "authorization", "code": "UNAUTHENTICATED", "message": "…"}]}"""
        assertEquals(RenameOutcome.SessionExpired, gatewayAnswering { json(HttpStatusCode.Unauthorized, body) }.rename(session, "Ana"))
    }

    @Test
    fun `5xx is a server failure`() = runTest {
        assertEquals(RenameOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.rename(session, "Ana"))
    }

    @Test
    fun `a body that is not understood is a server failure`() = runTest {
        assertEquals(RenameOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.OK, "{}") }.rename(session, "Ana"))
    }

    @Test
    fun `no connection is unreachable`() = runTest {
        assertEquals(RenameOutcome.Unreachable, gatewayAnswering { throw IOException("sin red") }.rename(session, "Ana"))
    }

    @Test
    fun `a timeout is unreachable`() = runTest {
        assertEquals(RenameOutcome.Unreachable, gatewayAnswering { throw HttpRequestTimeoutException("url", 10_000) }.rename(session, "Ana"))
    }
}
