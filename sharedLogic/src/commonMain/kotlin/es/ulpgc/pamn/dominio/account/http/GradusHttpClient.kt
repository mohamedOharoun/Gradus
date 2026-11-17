package es.ulpgc.pamn.dominio.account.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Factory method del cliente HTTP con la configuración de Gradus.
// Sin motor, Ktor usa el de la plataforma (OkHttp en Android, Darwin en iOS); los tests le pasan un MockEngine.
internal fun gradusHttpClient(engine: HttpClientEngine? = null): HttpClient =
    if (engine == null) HttpClient { gradusDefaults() } else HttpClient(engine) { gradusDefaults() }

private fun HttpClientConfig<*>.gradusDefaults() {
    // Un 400 o un 409 son respuestas normales que hay que leer, no excepciones.
    expectSuccess = false
    install(ContentNegotiation) {
        // Si el servidor añade campos nuevos, la app no se rompe.
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}

private const val CONNECT_TIMEOUT_MILLIS = 5_000L
// El backend calcula PBKDF2 (≈ 0,4 s): hay margen de sobra.
private const val REQUEST_TIMEOUT_MILLIS = 10_000L
