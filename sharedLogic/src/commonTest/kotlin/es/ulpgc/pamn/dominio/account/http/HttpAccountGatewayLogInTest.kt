package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Credentials
import es.ulpgc.pamn.dominio.account.LogInOutcome
import es.ulpgc.pamn.dominio.account.ServerFieldError
import es.ulpgc.pamn.dominio.account.Session
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// Inicio de sesión (POST /api/sessions, backend/.../web/SessionController).
class HttpAccountGatewayLogInTest {

    private val credentials = Credentials(email = "ana@ulpgc.es", password = "Gradus2026!")

    // ── La petición ──

    @Test
    fun `it posts the credentials as the JSON that the backend expects`() = runTest {
        var sent: HttpRequestData? = null
        var sentBody = ""
        val gateway = gatewayAnswering { request ->
            sent = request
            sentBody = request.body.toByteArray().decodeToString()
            json(HttpStatusCode.Created, SESSION_JSON)
        }

        gateway.logIn(credentials)

        assertEquals(HttpMethod.Post, sent?.method)
        assertEquals("http://test/api/sessions", sent?.url.toString())
        // Mismos campos que LogInRequest.java, ni uno más (el backend rechaza los desconocidos).
        assertEquals(
            Json.parseToJsonElement("""{"email":"ana@ulpgc.es","password":"Gradus2026!"}"""),
            Json.parseToJsonElement(sentBody),
        )
    }

    // ── Las respuestas ──

    @Test
    fun `201 is logged in with the token, its expiry and the account`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.Created, SESSION_JSON) }.logIn(credentials)
        assertEquals(
            LogInOutcome.LoggedIn(
                Session(
                    token = "s3cr3t-t0k3n",
                    expiresAt = "2026-12-18T10:00:00Z",
                    account = Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00.123456Z"),
                ),
            ),
            outcome,
        )
    }

    @Test
    fun `400 is rejected with every error of the server`() = runTest {
        val body = """
            {"errors": [
              {"field": "email", "code": "INVALID_EMAIL", "message": "No tiene un formato de correo válido."},
              {"field": "password", "code": "REQUIRED", "message": "Es obligatorio."}
            ]}
        """
        val outcome = gatewayAnswering { json(HttpStatusCode.BadRequest, body) }.logIn(credentials)
        assertEquals(
            LogInOutcome.Rejected(listOf(ServerFieldError("email", "INVALID_EMAIL"), ServerFieldError("password", "REQUIRED"))),
            outcome,
        )
    }

    @Test
    fun `401 is invalid credentials`() = runTest {
        val body = """{"errors": [{"field": "credentials", "code": "INVALID_CREDENTIALS", "message": "Correo o contraseña incorrectos."}]}"""
        assertEquals(LogInOutcome.InvalidCredentials, gatewayAnswering { json(HttpStatusCode.Unauthorized, body) }.logIn(credentials))
    }

    // ── Fallos ──

    @Test
    fun `a 5xx is a server failure`() = runTest {
        val outcome = gatewayAnswering { respond("Server Error", HttpStatusCode.InternalServerError) }.logIn(credentials)
        assertEquals(LogInOutcome.ServerFailure, outcome)
    }

    @Test
    fun `a 201 without a token is a server failure`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.Created, USER_JSON) }.logIn(credentials)
        assertEquals(LogInOutcome.ServerFailure, outcome)
    }

    @Test
    fun `without connection the server is unreachable`() = runTest {
        val outcome = gatewayAnswering { throw IOException("Connection refused") }.logIn(credentials)
        assertEquals(LogInOutcome.Unreachable, outcome)
    }

    @Test
    fun `a timeout means the server is unreachable`() = runTest {
        val outcome = gatewayAnswering { throw HttpRequestTimeoutException("http://test/api/sessions", 10_000) }.logIn(credentials)
        assertEquals(LogInOutcome.Unreachable, outcome)
    }

    // ── Secretos fuera de los logs ──

    @Test
    fun `the DTOs never show the password or the token in toString`() {
        assertFalse(LogInRequestDto("ana@ulpgc.es", "Gradus2026!").toString().contains("Gradus2026!"))
        val user = UserResponseDto("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z")
        assertFalse(SessionResponseDto("s3cr3t-t0k3n", "2026-12-18T10:00:00Z", user).toString().contains("s3cr3t-t0k3n"))
    }

    private companion object {
        // Como lo devuelve SessionResponse.java.
        const val SESSION_JSON = """{"token": "s3cr3t-t0k3n", "expiresAt": "2026-12-18T10:00:00Z", "user": $USER_JSON}"""
    }
}
