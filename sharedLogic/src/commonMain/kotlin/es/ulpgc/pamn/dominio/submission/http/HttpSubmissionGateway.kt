package es.ulpgc.pamn.dominio.submission.http

import es.ulpgc.pamn.dominio.account.ApiErrorCode
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.http.ErrorResponseDto
import es.ulpgc.pamn.dominio.account.http.callServer
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.PhotoSubmission
import es.ulpgc.pamn.dominio.submission.Submission
import es.ulpgc.pamn.dominio.submission.SubmissionGateway
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

// Adaptador HTTP del puerto SubmissionGateway (docs/tareas/ejercicio-foto.md, decisión U): habla con
// backend/.../web/submission/SubmissionController. Multipart con `submitFormWithBinaryData`, que ya trae
// ktor-client-core (sin dependencias nuevas). Es la frontera con la red: cualquier fallo es un SubmitOutcome.
internal class HttpSubmissionGateway(
    private val client: HttpClient,
    private val baseUrl: String,
) : SubmissionGateway {

    override suspend fun submit(session: Session, submission: PhotoSubmission): SubmitOutcome {
        // Con más de 2 MiB, el servidor responde 413 sin leerla y corta la conexión a mitad del envío;
        // OkHttp lo ve como un fallo de red y diría «sin conexión» (comprobado contra el servidor real).
        if (submission.isTooLarge) return SubmitOutcome.PhotoRejected(PhotoRejection.TOO_LARGE)
        return send(session, submission)
    }

    private suspend fun send(session: Session, submission: PhotoSubmission): SubmitOutcome =
        callServer(unreachable = SubmitOutcome.Unreachable, failure = SubmitOutcome.ServerFailure) {
            val url = "$baseUrl/api/courses/${submission.courseId}/exercises/${submission.exerciseId}/submissions"
            val response = client.submitFormWithBinaryData(url, formOf(submission)) {
                bearerAuth(session.token)
                // 2 MB con mala cobertura pueden tardar más que los 10 s de las peticiones JSON.
                timeout { requestTimeoutMillis = UPLOAD_TIMEOUT_MILLIS }
            }
            outcomeOf(response)
        }

    private suspend fun outcomeOf(response: HttpResponse): SubmitOutcome = when (response.status) {
        HttpStatusCode.Created -> SubmitOutcome.Submitted(response.body<SubmissionResponseDto>().toSubmission())
        HttpStatusCode.BadRequest -> badRequestOf(response.body<ErrorResponseDto>())
        HttpStatusCode.PayloadTooLarge -> SubmitOutcome.PhotoRejected(PhotoRejection.TOO_LARGE)
        HttpStatusCode.NotFound -> SubmitOutcome.NotFound
        HttpStatusCode.Unauthorized -> SubmitOutcome.SessionExpired
        else -> SubmitOutcome.ServerFailure
    }

    // Un 400 de la foto es culpa de la foto; cualquier otro (versión, multipart roto) es un fallo de la app.
    private fun badRequestOf(error: ErrorResponseDto): SubmitOutcome {
        val first = error.errors.first()
        return when {
            first.field == PHOTO_FIELD && ApiErrorCode.fromName(first.code) == ApiErrorCode.INVALID_PHOTO ->
                SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG)
            first.field == PHOTO_FIELD && ApiErrorCode.fromName(first.code) == ApiErrorCode.REQUIRED ->
                SubmitOutcome.PhotoRejected(PhotoRejection.MISSING)
            else -> SubmitOutcome.ServerFailure
        }
    }

    private companion object {
        const val PHOTO_FIELD = "photo"
        const val UPLOAD_TIMEOUT_MILLIS = 60_000L

        // Los dos campos del contrato. La foto va como fichero (con filename): si no, el servidor no la ve
        // como foto (paso 3e-2). El nombre da igual: el servidor mira los bytes, no el nombre ni el tipo.
        fun formOf(submission: PhotoSubmission) = formData {
            append("version", submission.courseVersion.toString())
            append(
                PHOTO_FIELD,
                submission.jpeg,
                Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"foto.jpg\"")
                },
            )
        }
    }
}

// Lo que la app usa de SubmissionResponse.java; los demás campos se ignoran (ignoreUnknownKeys).
@Serializable
internal data class SubmissionResponseDto(val id: String, val submittedAt: String) {
    fun toSubmission() = Submission(id = id, submittedAt = submittedAt)
}
