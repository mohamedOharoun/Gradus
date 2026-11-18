package es.ulpgc.pamn.dominio.account.http

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

// Ayudas comunes a los tests del adaptador HTTP: un servidor falso (MockEngine) sin red.

// Crea el adaptador con un servidor falso que responde lo que diga `handler`.
internal fun gatewayAnswering(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) = HttpAccountGateway(gradusHttpClient(MockEngine(handler)), baseUrl = "http://test")

// Respuesta JSON con su cabecera Content-Type, como las de Javalin.
internal fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
    respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

// Como lo devuelve UserResponse.java.
internal const val USER_JSON =
    """{"id": "7f3c", "name": "Ana", "email": "ana@ulpgc.es", "createdAt": "2026-11-17T10:00:00.123456Z"}"""
