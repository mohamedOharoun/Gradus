package es.ulpgc.pamn.dominio.course.publication.http

import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.http.callServer
import es.ulpgc.pamn.dominio.course.publication.CoursePublicationGateway
import es.ulpgc.pamn.dominio.course.publication.MAX_COURSE_BYTES
import es.ulpgc.pamn.dominio.course.publication.MyCoursesOutcome
import es.ulpgc.pamn.dominio.course.publication.PublishOutcome
import es.ulpgc.pamn.dominio.course.publication.ValidationOutcome
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.content.ByteArrayContent

// Adaptador HTTP del puerto CoursePublicationGateway (backend/docs/api-cursos.md).
// Envía los bytes JSON tal cual con ByteArrayContent para que ContentNegotiation no re-serialice
// ni envuelva el fichero del autor, y rechaza localmente > 2 MiB sin abrir socket.
internal class HttpCoursePublicationGateway(
    private val client: HttpClient,
    private val baseUrl: String,
) : CoursePublicationGateway {

    override suspend fun myCourses(session: Session): MyCoursesOutcome =
        callServer(unreachable = MyCoursesOutcome.Unreachable, failure = MyCoursesOutcome.ServerFailure) {
            val response = client.get(baseUrl) {
                url { appendPathSegments("api", "users", "me", "courses") }
                bearerAuth(session.token)
            }
            when (response.status) {
                HttpStatusCode.OK -> decodeMyCourses(response.bodyAsText())
                    ?.let(MyCoursesOutcome::Found)
                    ?: MyCoursesOutcome.ServerFailure
                HttpStatusCode.Unauthorized -> MyCoursesOutcome.SessionExpired
                else -> MyCoursesOutcome.ServerFailure
            }
        }

    override suspend fun validateNew(session: Session, courseJson: ByteArray): ValidationOutcome =
        postValidation(session, courseJson, listOf("api", "courses", "validate"))

    override suspend fun validateVersion(
        session: Session,
        courseId: String,
        courseJson: ByteArray,
    ): ValidationOutcome =
        postValidation(session, courseJson, listOf("api", "courses", courseId, "versions", "validate"))

    override suspend fun publishNew(session: Session, courseJson: ByteArray): PublishOutcome =
        postPublish(session, courseJson, listOf("api", "courses"))

    override suspend fun publishVersion(
        session: Session,
        courseId: String,
        courseJson: ByteArray,
    ): PublishOutcome =
        postPublish(session, courseJson, listOf("api", "courses", courseId, "versions"))

    private suspend fun postValidation(
        session: Session,
        courseJson: ByteArray,
        pathSegments: List<String>,
    ): ValidationOutcome {
        if (courseJson.size > MAX_COURSE_BYTES) return ValidationOutcome.TooLarge
        return callServer(unreachable = ValidationOutcome.Unreachable, failure = ValidationOutcome.ServerFailure) {
            val response = postCourseJson(session, courseJson, pathSegments)
            validationOutcomeOf(response)
        }
    }

    private suspend fun postPublish(
        session: Session,
        courseJson: ByteArray,
        pathSegments: List<String>,
    ): PublishOutcome {
        if (courseJson.size > MAX_COURSE_BYTES) return PublishOutcome.TooLarge
        return callServer(unreachable = PublishOutcome.Unreachable, failure = PublishOutcome.ServerFailure) {
            val response = postCourseJson(session, courseJson, pathSegments)
            publishOutcomeOf(response)
        }
    }

    private suspend fun postCourseJson(
        session: Session,
        courseJson: ByteArray,
        pathSegments: List<String>,
    ): HttpResponse = client.post(baseUrl) {
        url { appendPathSegments(pathSegments, encodeSlash = true) }
        withCourseUpload(session, courseJson)
    }

    private fun HttpRequestBuilder.withCourseUpload(session: Session, courseJson: ByteArray) {
        bearerAuth(session.token)
        // ByteArrayContent evita que ContentNegotiation serialice el ByteArray como lista de números.
        setBody(ByteArrayContent(courseJson, ContentType.Application.Json))
        timeout { requestTimeoutMillis = UPLOAD_TIMEOUT_MILLIS }
    }

    private suspend fun validationOutcomeOf(response: HttpResponse): ValidationOutcome = when (response.status) {
        HttpStatusCode.OK -> decodeCourseReport(response.bodyAsText())
            ?.let(ValidationOutcome::Validated)
            ?: ValidationOutcome.ServerFailure
        HttpStatusCode.Unauthorized -> ValidationOutcome.SessionExpired
        HttpStatusCode.Forbidden -> ValidationOutcome.NotAuthor
        HttpStatusCode.NotFound -> ValidationOutcome.NotFound
        HttpStatusCode.Conflict -> ValidationOutcome.IdenticalToLatest
        HttpStatusCode.PayloadTooLarge -> ValidationOutcome.TooLarge
        else -> ValidationOutcome.ServerFailure
    }

    private suspend fun publishOutcomeOf(response: HttpResponse): PublishOutcome = when (response.status) {
        HttpStatusCode.Created -> decodePublishedCourse(response.bodyAsText())
            ?: PublishOutcome.ServerFailure
        HttpStatusCode.BadRequest, HttpStatusCode.UnprocessableEntity -> {
            val report = decodeCourseReport(response.bodyAsText())
            if (report != null && !report.isValid) PublishOutcome.Invalid(report) else PublishOutcome.ServerFailure
        }
        HttpStatusCode.Unauthorized -> PublishOutcome.SessionExpired
        HttpStatusCode.Forbidden -> PublishOutcome.NotAuthor
        HttpStatusCode.NotFound -> PublishOutcome.NotFound
        HttpStatusCode.Conflict -> PublishOutcome.IdenticalToLatest
        HttpStatusCode.PayloadTooLarge -> PublishOutcome.TooLarge
        else -> PublishOutcome.ServerFailure
    }

    private companion object {
        const val UPLOAD_TIMEOUT_MILLIS = 60_000L
    }
}
