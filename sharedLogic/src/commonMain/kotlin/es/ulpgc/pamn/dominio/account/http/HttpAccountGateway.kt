package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.AccountGateway
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

// Adaptador HTTP + JSON del puerto AccountGateway: habla con backend/.../web/UserController.
// Es la frontera con la red, así que aquí se convierte cualquier fallo en un RegistrationOutcome.
internal class HttpAccountGateway(
    private val client: HttpClient,
    private val baseUrl: String,
) : AccountGateway {

    override suspend fun register(account: NewAccount): RegistrationOutcome = try {
        val response = client.post("$baseUrl/api/users") {
            contentType(ContentType.Application.Json)
            setBody(account.toDto())
        }
        outcomeOf(response)
    } catch (e: CancellationException) {
        // Si se cancela (p. ej. se cierra la pantalla), se deja cancelar: no es un fallo de red.
        throw e
    } catch (e: IOException) {
        // Sin red, servidor apagado o tiempo agotado (HttpRequestTimeoutException también es IOException).
        RegistrationOutcome.Unreachable
    } catch (e: Exception) {
        // Un cuerpo que no se entiende u otra respuesta rara del servidor.
        RegistrationOutcome.ServerFailure
    }

    private suspend fun outcomeOf(response: HttpResponse): RegistrationOutcome = when (response.status) {
        HttpStatusCode.Created -> RegistrationOutcome.Registered(response.body<UserResponseDto>().toAccount())
        HttpStatusCode.BadRequest -> RegistrationOutcome.Rejected(response.body<ErrorResponseDto>().toServerFieldErrors())
        HttpStatusCode.Conflict -> RegistrationOutcome.EmailTaken
        else -> RegistrationOutcome.ServerFailure
    }
}
