package es.ulpgc.pamn.dominio.course.http

import es.ulpgc.pamn.dominio.account.http.callServer
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.SearchOutcome
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments

// Adaptador HTTP + JSON del puerto CourseGateway: habla con GET /api/courses y GET /api/courses/{id}
// (backend/docs/api-cursos.md). Lee el cuerpo como texto y lo traduce con CourseDtoMappers.
internal class HttpCourseGateway(
    private val client: HttpClient,
    private val baseUrl: String,
) : CourseGateway {

    override suspend fun search(query: String, page: Int): SearchOutcome =
        callServer(unreachable = SearchOutcome.Unreachable, failure = SearchOutcome.ServerFailure) {
            val response = client.get(baseUrl) {
                url {
                    appendPathSegments("api", "courses")
                    if (query.isNotBlank()) parameters.append("q", query.trim())
                    parameters.append("page", page.toString())
                    parameters.append("size", PAGE_SIZE.toString())
                }
            }
            if (response.status != HttpStatusCode.OK) {
                SearchOutcome.ServerFailure
            } else {
                decodeCoursePage(response.bodyAsText())?.let(SearchOutcome::Found) ?: SearchOutcome.ServerFailure
            }
        }

    override suspend fun course(id: String): CourseOutcome =
        callServer(unreachable = CourseOutcome.Unreachable, failure = CourseOutcome.ServerFailure) {
            // encodeSlash: un «/» dentro del id se manda como %2F, así que no puede cambiar la ruta pedida.
            val response = client.get(baseUrl) { url { appendPathSegments(listOf("api", "courses", id), encodeSlash = true) } }
            when (response.status) {
                HttpStatusCode.OK -> when (val decoded = decodeCourseVersion(response.bodyAsText())) {
                    is CourseDecoding.Decoded -> CourseOutcome.Found(decoded.course)
                    is CourseDecoding.UnsupportedFormat -> CourseOutcome.UnsupportedFormat
                    is CourseDecoding.Malformed -> CourseOutcome.ServerFailure
                }
                HttpStatusCode.NotFound -> CourseOutcome.NotFound
                else -> CourseOutcome.ServerFailure
            }
        }

    private companion object {
        // Resultados por página (el backend admite de 1 a 100; 20 es su valor por defecto).
        const val PAGE_SIZE = 20
    }
}
