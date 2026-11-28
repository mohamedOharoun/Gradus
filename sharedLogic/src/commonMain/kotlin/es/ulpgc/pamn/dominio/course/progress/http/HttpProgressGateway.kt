package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.http.callServer
import es.ulpgc.pamn.dominio.course.progress.ProgressGateway
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import es.ulpgc.pamn.dominio.course.progress.UnenrollOutcome
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent

// Adaptador HTTP + JSON del puerto ProgressGateway: habla con POST /api/progress/sync y
// DELETE /api/progress/enrollments/{courseId} (backend/docs/api-progreso.md).
// El JSON lo escribe y lee ProgressDtoMappers; aquí solo está el HTTP.
internal class HttpProgressGateway(
    private val client: HttpClient,
    private val baseUrl: String,
) : ProgressGateway {

    override suspend fun sync(session: Session, pending: ProgressSnapshot): SyncOutcome =
        callServer(unreachable = SyncOutcome.Unreachable, failure = SyncOutcome.ServerFailure) {
            val response = client.post(baseUrl) {
                url { appendPathSegments("api", "progress", "sync") }
                bearerAuth(session.token)
                // TextContent ya es un cuerpo listo: ContentNegotiation no lo vuelve a serializar.
                // Sin charset en el tipo, Ktor lo codifica en UTF-8 (y sin BOM).
                setBody(TextContent(encodeSyncRequest(pending), ContentType.Application.Json))
            }
            // Primero el estado y después el cuerpo: el 413 llega en text/plain, no en JSON.
            when (response.status) {
                HttpStatusCode.OK -> decodeSyncResponse(response.bodyAsText())
                    ?.let(SyncOutcome::Synced) ?: SyncOutcome.ServerFailure
                HttpStatusCode.Unauthorized -> SyncOutcome.SessionExpired
                else -> SyncOutcome.ServerFailure
            }
        }

    override suspend fun unenroll(session: Session, courseId: String): UnenrollOutcome =
        callServer(unreachable = UnenrollOutcome.Unreachable, failure = UnenrollOutcome.ServerFailure) {
            val response = client.delete(baseUrl) {
                // encodeSlash: un «/» dentro del id se manda como %2F para no cambiar la ruta pedida.
                url { appendPathSegments(listOf("api", "progress", "enrollments", courseId), encodeSlash = true) }
                bearerAuth(session.token)
            }
            // No se lee el cuerpo en ningún código: 204 llega vacío y en los demás solo importa el estado.
            when (response.status) {
                HttpStatusCode.NoContent -> UnenrollOutcome.Unenrolled
                HttpStatusCode.Unauthorized -> UnenrollOutcome.SessionExpired
                else -> UnenrollOutcome.ServerFailure
            }
        }
}
