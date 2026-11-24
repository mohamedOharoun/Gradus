package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.ChangePasswordOutcome
import es.ulpgc.pamn.dominio.account.DeleteAccountOutcome
import es.ulpgc.pamn.dominio.account.PasswordChange
import es.ulpgc.pamn.dominio.account.RenameOutcome
import es.ulpgc.pamn.dominio.account.Credentials
import es.ulpgc.pamn.dominio.account.LogInOutcome
import es.ulpgc.pamn.dominio.account.LogOutOutcome
import es.ulpgc.pamn.dominio.account.NewAccount
import es.ulpgc.pamn.dominio.account.RegistrationOutcome
import es.ulpgc.pamn.dominio.account.Session
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// Adaptador HTTP + JSON del puerto AccountGateway: habla con backend/.../web/UserController
// (registro y Ajustes: nombre, contraseña, eliminar la cuenta) y SessionController (iniciar y cerrar sesión). Es la frontera con la red, así que aquí se
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

    override suspend fun logOut(session: Session): LogOutOutcome =
        callServer(unreachable = LogOutOutcome.Unreachable, failure = LogOutOutcome.ServerFailure) {
            val response = client.delete("$baseUrl/api/sessions/current") { bearerAuth(session.token) }
            when (response.status) {
                // 401: el token ya no valía; para el usuario, la sesión ya está cerrada.
                HttpStatusCode.NoContent, HttpStatusCode.Unauthorized -> LogOutOutcome.LoggedOut
                else -> LogOutOutcome.ServerFailure
            }
        }

    // ── Ajustes (fase 2): las tres llevan el token y un cuerpo JSON ──

    override suspend fun rename(session: Session, name: String): RenameOutcome =
        callServer(unreachable = RenameOutcome.Unreachable, failure = RenameOutcome.ServerFailure) {
            val response = client.patch("$baseUrl/api/users/me") { withSessionAndJson(session, RenameRequestDto(name)) }
            when (response.status) {
                HttpStatusCode.OK -> RenameOutcome.Renamed(response.body<UserResponseDto>().toAccount())
                HttpStatusCode.BadRequest -> RenameOutcome.Rejected(response.body<ErrorResponseDto>().toServerFieldErrors())
                HttpStatusCode.Unauthorized -> RenameOutcome.SessionExpired
                else -> RenameOutcome.ServerFailure
            }
        }

    override suspend fun changePassword(session: Session, change: PasswordChange): ChangePasswordOutcome =
        callServer(unreachable = ChangePasswordOutcome.Unreachable, failure = ChangePasswordOutcome.ServerFailure) {
            val response = client.put("$baseUrl/api/users/me/password") { withSessionAndJson(session, change.toDto()) }
            when (response.status) {
                HttpStatusCode.NoContent -> ChangePasswordOutcome.Changed
                HttpStatusCode.BadRequest -> ChangePasswordOutcome.Rejected(response.body<ErrorResponseDto>().toServerFieldErrors())
                HttpStatusCode.Forbidden -> ChangePasswordOutcome.WrongPassword
                HttpStatusCode.Unauthorized -> ChangePasswordOutcome.SessionExpired
                else -> ChangePasswordOutcome.ServerFailure
            }
        }

    // DELETE con cuerpo (decisión J de docs/tareas/ajustes.md): la contraseña confirma el borrado.
    override suspend fun deleteAccount(session: Session, password: String): DeleteAccountOutcome =
        callServer(unreachable = DeleteAccountOutcome.Unreachable, failure = DeleteAccountOutcome.ServerFailure) {
            val response = client.delete("$baseUrl/api/users/me") { withSessionAndJson(session, DeleteAccountRequestDto(password)) }
            when (response.status) {
                HttpStatusCode.NoContent -> DeleteAccountOutcome.Deleted
                HttpStatusCode.BadRequest -> DeleteAccountOutcome.Rejected(response.body<ErrorResponseDto>().toServerFieldErrors())
                HttpStatusCode.Forbidden -> DeleteAccountOutcome.WrongPassword
                HttpStatusCode.Conflict -> DeleteAccountOutcome.AuthorOfCourses
                HttpStatusCode.Unauthorized -> DeleteAccountOutcome.SessionExpired
                else -> DeleteAccountOutcome.ServerFailure
            }
        }

    // Cabeceras comunes de las peticiones de Ajustes: el token de la sesión y un cuerpo JSON.
    private fun HttpRequestBuilder.withSessionAndJson(session: Session, body: Any) {
        bearerAuth(session.token)
        contentType(ContentType.Application.Json)
        setBody(body)
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
// Función de orden superior: recibe la llamada como lambda y la envuelve (la usan todas las peticiones,
// también las de cursos: course/http/HttpCourseGateway).
internal suspend fun <T> callServer(unreachable: T, failure: T, call: suspend () -> T): T = try {
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
