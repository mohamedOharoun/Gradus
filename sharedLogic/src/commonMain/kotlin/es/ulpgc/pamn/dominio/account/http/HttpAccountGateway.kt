package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.Credentials
import es.ulpgc.pamn.dominio.account.LogInOutcome
import es.ulpgc.pamn.dominio.account.NewAccount
import es.ulpgc.pamn.dominio.account.RegistrationOutcome
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// Adaptador HTTP + JSON del puerto AccountGateway: habla con backend/.../web/UserController
// (registro) y SessionController (inicio de sesión). Es la frontera con la red, así que aquí se
// convierte cualquier fallo en un desenlace (RegistrationOutcome o LogInOutcome).
internal class HttpAccountGateway(
    private val client: HttpClient,
    private val baseUrl: String,
) : AccountGateway {

    override suspend fun register(account: NewAccount): RegistrationOutcome =
        callServer(unreachable = RegistrationOutcome.Unreachable, failure = RegistrationOutcome.ServerFailure) {
            registrationOutcomeOf(post("/api/users", account.toDto()))
        }

    override suspend fun logIn(credentials: Credentials): LogInOutcome =
        callServer(unreachable = LogInOutcome.Unreachable, failure = LogInOutcome.ServerFailure) {
            logInOutcomeOf(post("/api/sessions", credentials.toDto()))
        }

    private suspend fun post(path: String, body: Any): HttpResponse = client.post("$baseUrl$path") {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun registrationOutcomeOf(response: HttpResponse): RegistrationOutcome = when (response.status) {
        HttpStatusCode.Created -> RegistrationOutcome.Registered(response.body<UserResponseDto>().toAccount())
        HttpStatusCode.BadRequest -> RegistrationOutcome.Rejected(response.body<ErrorResponseDto>().toServerFieldErrors())
        HttpStatusCode.Conflict -> RegistrationOutcome.EmailTaken
        else -> RegistrationOutcome.ServerFailure
    }

    private suspend fun logInOutcomeOf(response: HttpResponse): LogInOutcome = when (response.status) {
        HttpStatusCode.Created -> LogInOutcome.LoggedIn(response.body<SessionResponseDto>().toSession())
        HttpStatusCode.BadRequest -> LogInOutcome.Rejected(response.body<ErrorResponseDto>().toServerFieldErrors())
        HttpStatusCode.Unauthorized -> LogInOutcome.InvalidCredentials
        else -> LogInOutcome.ServerFailure
    }
}

// Ejecuta una llamada al servidor y convierte las excepciones en el desenlace que toque.
// Función de orden superior: recibe la llamada como lambda y la envuelve (la usan todas las peticiones).
private suspend fun <T> callServer(unreachable: T, failure: T, call: suspend () -> T): T = try {
    call()
} catch (e: CancellationException) {
    // Si se cancela (p. ej. se cierra la pantalla), se deja cancelar: no es un fallo de red.
    throw e
} catch (e: IOException) {
    // Sin red, servidor apagado o tiempo agotado (HttpRequestTimeoutException también es IOException).
    unreachable
} catch (e: Exception) {
    // Un cuerpo que no se entiende u otra respuesta rara del servidor.
    failure
}
