package es.ulpgc.pamn.dominio.course.publication.http

import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.MAX_COURSE_BYTES
import es.ulpgc.pamn.dominio.course.publication.PublishOutcome
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

// Publicación de curso nuevo (POST /api/courses) y de nueva versión (POST /api/courses/{id}/versions).
class HttpPublishCourseTest {

    private val publishedNoWarningsJson = """{"course": $SUMMARY_JSON, "warnings": []}"""
    private val publishedWithWarningsJson = """
        {
          "course": $SUMMARY_JSON,
          "warnings": [
            {"code": "REMOVED_IDS", "path": "", "message": "Se eliminaron módulos de la versión anterior"}
          ]
        }
    """

    // ── Peticiones ──

    @Test
    fun `publishNew posts raw json bytes with bearer token and sixty second timeout`() = runTest {
        var sent: HttpRequestData? = null
        publicationGatewayAnswering { request ->
            sent = request
            json(HttpStatusCode.Created, publishedNoWarningsJson)
        }.publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES)

        assertEquals(HttpMethod.Post, sent?.method)
        assertEquals("/api/courses", sent?.url?.encodedPath)
        assertEquals("Bearer $TEST_TOKEN", sent?.headers?.get(HttpHeaders.Authorization))
        assertEquals(ContentType.Application.Json, sent?.body?.contentType)
        assertContentEquals(SAMPLE_COURSE_BYTES, sent!!.body.toByteArray())
        assertEquals(60_000L, sent.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis)
    }

    @Test
    fun `publishVersion posts raw json bytes with bearer token and sixty second timeout`() = runTest {
        var sent: HttpRequestData? = null
        publicationGatewayAnswering { request ->
            sent = request
            json(HttpStatusCode.Created, publishedWithWarningsJson)
        }.publishVersion(TEST_SESSION, "0b8f-1", SAMPLE_COURSE_BYTES)

        assertEquals(HttpMethod.Post, sent?.method)
        assertEquals("/api/courses/0b8f-1/versions", sent?.url?.encodedPath)
        assertEquals("Bearer $TEST_TOKEN", sent?.headers?.get(HttpHeaders.Authorization))
        assertEquals(ContentType.Application.Json, sent?.body?.contentType)
        assertContentEquals(SAMPLE_COURSE_BYTES, sent!!.body.toByteArray())
        assertEquals(60_000L, sent.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis)
    }

    @Test
    fun `publishVersion encodes slashes in courseId as a single path segment`() = runTest {
        var sent: HttpRequestData? = null
        publicationGatewayAnswering { request ->
            sent = request
            json(HttpStatusCode.NotFound, apiErrorJson("id", "COURSE_NOT_FOUND"))
        }.publishVersion(TEST_SESSION, "../users", SAMPLE_COURSE_BYTES)

        assertEquals("/api/courses/..%2Fusers/versions", sent?.url?.encodedPath)
    }

    // ── Límite local de 2 MiB ──

    @Test
    fun `payload over two megabytes returns too large without opening a socket`() = runTest {
        var requests = 0
        val tooLarge = ByteArray(MAX_COURSE_BYTES + 1)
        val gateway = publicationGatewayAnswering {
            requests++
            json(HttpStatusCode.Created, publishedNoWarningsJson)
        }

        assertEquals(PublishOutcome.TooLarge, gateway.publishNew(TEST_SESSION, tooLarge))
        assertEquals(PublishOutcome.TooLarge, gateway.publishVersion(TEST_SESSION, "0b8f", tooLarge))
        assertEquals(0, requests)
    }

    @Test
    fun `payload of exact two megabytes is sent to the server`() = runTest {
        var requests = 0
        val exactLimit = ByteArray(MAX_COURSE_BYTES)
        val gateway = publicationGatewayAnswering {
            requests++
            json(HttpStatusCode.Created, publishedNoWarningsJson)
        }

        assertIs<PublishOutcome.Published>(gateway.publishNew(TEST_SESSION, exactLimit))
        assertIs<PublishOutcome.Published>(gateway.publishVersion(TEST_SESSION, "0b8f", exactLimit))
        assertEquals(2, requests)
    }

    // ── Respuestas ──

    @Test
    fun `201 returns published with course summary and warnings`() = runTest {
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.Created, publishedWithWarningsJson) }
            .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES)

        assertEquals(
            PublishOutcome.Published(
                course = CourseSummary(
                    id = "0b8f",
                    version = 2,
                    author = Author("5c1d", "Ana"),
                    publishedAt = "2026-11-18T08:35:00Z",
                    title = "Introducción a los grafos",
                    description = "Recorridos básicos",
                    language = "es",
                    tags = listOf("grafos", "algoritmos"),
                    moduleCount = 2,
                ),
                warnings = listOf(
                    CourseIssue("REMOVED_IDS", "", "Se eliminaron módulos de la versión anterior"),
                ),
            ),
            outcome,
        )
    }

    @Test
    fun `201 ignores unknown extra fields in course and warnings`() = runTest {
        val body = """
            {
              "course": $SUMMARY_WITH_EXTRA_METRICS_JSON,
              "warnings": [{"code": "REMOVED_IDS", "path": "", "message": "Ids borrados", "extra": true}],
              "serverNote": "ignored"
            }
        """.trimIndent()
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.Created, body) }
            .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES)

        val published = assertIs<PublishOutcome.Published>(outcome)
        assertEquals("0b8f", published.course.id)
        assertEquals(1, published.warnings.size)
    }

    @Test
    fun `400 and 422 with error report return invalid publish outcome`() = runTest {
        val expected = PublishOutcome.Invalid(
            CourseReport(
                errors = listOf(
                    CourseIssue("UNKNOWN_PREREQUISITE", "/modules/1/requires/0", "Falta el prerrequisito"),
                ),
                warnings = listOf(
                    CourseIssue("DUPLICATE_TAG", "/tags/1", "Etiqueta duplicada"),
                ),
            ),
        )

        assertEquals(
            expected,
            publicationGatewayAnswering { json(HttpStatusCode.BadRequest, INVALID_REPORT_JSON) }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            expected,
            publicationGatewayAnswering { json(HttpStatusCode.UnprocessableEntity, INVALID_REPORT_JSON) }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `400 and 422 with valid report or malformed json return server failure`() = runTest {
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.BadRequest, VALID_REPORT_JSON) }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.UnprocessableEntity, VALID_REPORT_WITH_WARNING_JSON) }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.BadRequest, "<html>error</html>") }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.UnprocessableEntity, "{}") }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `http error statuses map to specific publish outcomes`() = runTest {
        assertEquals(
            PublishOutcome.SessionExpired,
            publicationGatewayAnswering { json(HttpStatusCode.Unauthorized, apiErrorJson("authorization", "UNAUTHENTICATED")) }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.NotAuthor,
            publicationGatewayAnswering { json(HttpStatusCode.Forbidden, apiErrorJson("id", "NOT_COURSE_AUTHOR")) }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.NotFound,
            publicationGatewayAnswering { json(HttpStatusCode.NotFound, apiErrorJson("id", "COURSE_NOT_FOUND")) }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.IdenticalToLatest,
            publicationGatewayAnswering { json(HttpStatusCode.Conflict, apiErrorJson("body", "IDENTICAL_TO_LATEST")) }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.TooLarge,
            publicationGatewayAnswering { json(HttpStatusCode.PayloadTooLarge, INVALID_REPORT_JSON) }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `unexpected status or malformed 201 body is server failure`() = runTest {
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.OK, publishedNoWarningsJson) }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.Created, "<html>error</html>") }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.Created, """{"course": $SUMMARY_JSON}""") }
                .publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `network error and timeout are unreachable`() = runTest {
        assertEquals(
            PublishOutcome.Unreachable,
            publicationGatewayAnswering { throw IOException("sin red") }.publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            PublishOutcome.Unreachable,
            publicationGatewayAnswering { throw HttpRequestTimeoutException("url", 60_000) }
                .publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `session token never appears in publish outcomes`() = runTest {
        val outcomes = listOf(
            publicationGatewayAnswering { json(HttpStatusCode.Created, publishedNoWarningsJson) }.publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
            publicationGatewayAnswering { json(HttpStatusCode.BadRequest, INVALID_REPORT_JSON) }.publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
            publicationGatewayAnswering { json(HttpStatusCode.Unauthorized, "{}") }.publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
            publicationGatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.publishVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
            publicationGatewayAnswering { throw IOException("sin red") }.publishNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )

        outcomes.forEach { outcome ->
            assertFalse(TEST_TOKEN in outcome.toString())
        }
    }
}
