package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.NewAccount
import es.ulpgc.pamn.dominio.account.RegistrationOutcome
import es.ulpgc.pamn.dominio.account.ServerFieldError
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

// Registro (POST /api/users). El inicio de sesión está en HttpAccountGatewayLogInTest.
class HttpAccountGatewayTest {

    private val newAccount = NewAccount(name = "Ana", email = "ana@ulpgc.es", password = "Gradus2026!", acceptedTerms = true)

    // ── La petición ──

    @Test
    fun `it posts the new account as the JSON that the backend expects`() = runTest {
        var sent: HttpRequestData? = null
        var sentBody = ""
        val gateway = gatewayAnswering { request ->
            sent = request
            sentBody = request.body.toByteArray().decodeToString()
            json(HttpStatusCode.Created, USER_JSON)
        }

        gateway.register(newAccount)

        assertEquals(HttpMethod.Post, sent?.method)
        assertEquals("http://test/api/users", sent?.url.toString())
        // Mismos campos que RegisterUserRequest.java, ni uno más (el backend rechaza los desconocidos).
        assertEquals(
            Json.parseToJsonElement("""{"name":"Ana","email":"ana@ulpgc.es","password":"Gradus2026!","acceptedTerms":true}"""),
            Json.parseToJsonElement(sentBody),
        )
    }

    // ── Las respuestas ──

    @Test
    fun `201 is registered with the account returned by the server`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.Created, USER_JSON) }.register(newAccount)
        assertEquals(
            RegistrationOutcome.Registered(Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00.123456Z")),
            outcome,
        )
    }

    @Test
    fun `400 is rejected with every error of the server`() = runTest {
        val body = """
            {"errors": [
              {"field": "name", "code": "TOO_SHORT", "message": "Debe tener al menos 2 caracteres."},
              {"field": "email", "code": "INVALID_EMAIL", "message": "No tiene un formato de correo válido."}
            ]}
        """
        val outcome = gatewayAnswering { json(HttpStatusCode.BadRequest, body) }.register(newAccount)
        assertEquals(
            RegistrationOutcome.Rejected(listOf(ServerFieldError("name", "TOO_SHORT"), ServerFieldError("email", "INVALID_EMAIL"))),
            outcome,
        )
    }

    @Test
    fun `409 is email taken`() = runTest {
        val body = """{"errors": [{"field": "email", "code": "EMAIL_TAKEN", "message": "Ya existe una cuenta con este correo."}]}"""
        assertEquals(RegistrationOutcome.EmailTaken, gatewayAnswering { json(HttpStatusCode.Conflict, body) }.register(newAccount))
    }

    @Test
    fun `a field the app does not know yet in the response does not break anything`() = runTest {
        val body = """{"id": "7f3c", "name": "Ana", "email": "ana@ulpgc.es", "createdAt": "2026-11-17T10:00:00Z", "avatar": null}"""
        val outcome = gatewayAnswering { json(HttpStatusCode.Created, body) }.register(newAccount)
        assertEquals(RegistrationOutcome.Registered(Account("7f3c", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z")), outcome)
    }

    // ── Fallos ──

    @Test
    fun `a 5xx is a server failure`() = runTest {
        val outcome = gatewayAnswering { respond("Server Error", HttpStatusCode.InternalServerError) }.register(newAccount)
        assertEquals(RegistrationOutcome.ServerFailure, outcome)
    }

    @Test
    fun `an unexpected status is a server failure`() = runTest {
        val outcome = gatewayAnswering { respond("Not Found", HttpStatusCode.NotFound) }.register(newAccount)
        assertEquals(RegistrationOutcome.ServerFailure, outcome)
    }

    @Test
    fun `a body that cannot be read is a server failure`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.Created, "esto no es JSON") }.register(newAccount)
        assertEquals(RegistrationOutcome.ServerFailure, outcome)
    }

    @Test
    fun `a 400 whose body cannot be read is a server failure`() = runTest {
        val outcome = gatewayAnswering { json(HttpStatusCode.BadRequest, """{"unexpected": true}""") }.register(newAccount)
        assertEquals(RegistrationOutcome.ServerFailure, outcome)
    }

    @Test
    fun `without connection the server is unreachable`() = runTest {
        val outcome = gatewayAnswering { throw IOException("Connection refused") }.register(newAccount)
        assertEquals(RegistrationOutcome.Unreachable, outcome)
    }

    @Test
    fun `a timeout means the server is unreachable`() = runTest {
        val outcome = gatewayAnswering { throw HttpRequestTimeoutException("http://test/api/users", 10_000) }.register(newAccount)
        assertEquals(RegistrationOutcome.Unreachable, outcome)
    }
}
