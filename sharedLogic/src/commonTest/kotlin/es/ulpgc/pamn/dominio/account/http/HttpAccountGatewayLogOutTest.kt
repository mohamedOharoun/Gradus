package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.LogOutOutcome
import es.ulpgc.pamn.dominio.account.Session
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

// Cerrar sesión (DELETE /api/sessions/current, backend/.../web/SessionController). Lo usa Ajustes.
class HttpAccountGatewayLogOutTest {

    private val session = Session(
        token = "tok-123",
        expiresAt = "2026-12-22T10:00:00Z",
        account = Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"),
    )

    // ── La petición ──

    @Test
    fun `it deletes the current session sending its token`() = runTest {
        var sent: HttpRequestData? = null
        gatewayAnswering { sent = it; respond("", HttpStatusCode.NoContent) }.logOut(session)

        assertEquals(HttpMethod.Delete, sent?.method)
        assertEquals("http://test/api/sessions/current", sent?.url.toString())
        assertEquals("Bearer tok-123", sent?.headers?.get(HttpHeaders.Authorization))
    }

    // ── Las respuestas ──

    @Test
    fun `204 is logged out`() = runTest {
        assertEquals(LogOutOutcome.LoggedOut, gatewayAnswering { respond("", HttpStatusCode.NoContent) }.logOut(session))
    }

    // El token ya no valía (caducado o cerrado en otro sitio): para el usuario, ya está fuera.
    @Test
    fun `401 is logged out too`() = runTest {
        val body = """{"errors": [{"field": "authorization", "code": "UNAUTHENTICATED", "message": "…"}]}"""
        assertEquals(LogOutOutcome.LoggedOut, gatewayAnswering { json(HttpStatusCode.Unauthorized, body) }.logOut(session))
    }

    @Test
    fun `5xx is a server failure`() = runTest {
        assertEquals(LogOutOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.logOut(session))
    }

    @Test
    fun `an unexpected status is a server failure`() = runTest {
        assertEquals(LogOutOutcome.ServerFailure, gatewayAnswering { json(HttpStatusCode.BadRequest, "{}") }.logOut(session))
    }

    @Test
    fun `no connection is unreachable`() = runTest {
        assertEquals(LogOutOutcome.Unreachable, gatewayAnswering { throw IOException("sin red") }.logOut(session))
    }

    @Test
    fun `a timeout is unreachable`() = runTest {
        assertEquals(LogOutOutcome.Unreachable, gatewayAnswering { throw HttpRequestTimeoutException("url", 10_000) }.logOut(session))
    }
}
