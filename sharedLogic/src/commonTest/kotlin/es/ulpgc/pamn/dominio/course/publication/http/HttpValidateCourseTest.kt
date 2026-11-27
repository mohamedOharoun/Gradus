package es.ulpgc.pamn.dominio.course.publication.http

import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.MAX_COURSE_BYTES
import es.ulpgc.pamn.dominio.course.publication.ValidationOutcome
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

// Validación de curso nuevo (POST /api/courses/validate) y de nueva versión
// (POST /api/courses/{id}/versions/validate).
class HttpValidateCourseTest {

    // ── Peticiones ──

    @Test
    fun `validateNew posts raw json bytes with bearer token and sixty second timeout`() = runTest {
        val brokenJson = """{  "title" : "Sin cerrar" """.encodeToByteArray()
        var sent: HttpRequestData? = null
        publicationGatewayAnswering { request ->
            sent = request
            json(HttpStatusCode.OK, INVALID_REPORT_JSON)
        }.validateNew(TEST_SESSION, brokenJson)

        assertEquals(HttpMethod.Post, sent?.method)
        assertEquals("/api/courses/validate", sent?.url?.encodedPath)
        assertEquals("Bearer $TEST_TOKEN", sent?.headers?.get(HttpHeaders.Authorization))
        assertEquals(ContentType.Application.Json, sent?.body?.contentType)
        assertContentEquals(brokenJson, sent!!.body.toByteArray())
        assertEquals(60_000L, sent.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis)
    }

    @Test
    fun `validateVersion posts raw json bytes with bearer token and sixty second timeout`() = runTest {
        var sent: HttpRequestData? = null
        publicationGatewayAnswering { request ->
            sent = request
            json(HttpStatusCode.OK, VALID_REPORT_WITH_WARNING_JSON)
        }.validateVersion(TEST_SESSION, "0b8f-1", SAMPLE_COURSE_BYTES)

        assertEquals(HttpMethod.Post, sent?.method)
        assertEquals("/api/courses/0b8f-1/versions/validate", sent?.url?.encodedPath)
        assertEquals("Bearer $TEST_TOKEN", sent?.headers?.get(HttpHeaders.Authorization))
        assertEquals(ContentType.Application.Json, sent?.body?.contentType)
        assertContentEquals(SAMPLE_COURSE_BYTES, sent!!.body.toByteArray())
        assertEquals(60_000L, sent.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis)
    }

    @Test
    fun `validateVersion encodes slashes in courseId as a single path segment`() = runTest {
        var sent: HttpRequestData? = null
        publicationGatewayAnswering { request ->
            sent = request
            json(HttpStatusCode.NotFound, apiErrorJson("id", "COURSE_NOT_FOUND"))
        }.validateVersion(TEST_SESSION, "../users", SAMPLE_COURSE_BYTES)

        assertEquals("/api/courses/..%2Fusers/versions/validate", sent?.url?.encodedPath)
    }

    // ── Límite local de 2 MiB ──

    @Test
    fun `payload over two megabytes returns too large without opening a socket`() = runTest {
        var requests = 0
        val tooLarge = ByteArray(MAX_COURSE_BYTES + 1)
        val gateway = publicationGatewayAnswering {
            requests++
            json(HttpStatusCode.OK, VALID_REPORT_JSON)
        }

        assertEquals(ValidationOutcome.TooLarge, gateway.validateNew(TEST_SESSION, tooLarge))
        assertEquals(ValidationOutcome.TooLarge, gateway.validateVersion(TEST_SESSION, "0b8f", tooLarge))
        assertEquals(0, requests)
    }

    @Test
    fun `payload of exact two megabytes is sent to the server`() = runTest {
        var requests = 0
        val exactLimit = ByteArray(MAX_COURSE_BYTES)
        val gateway = publicationGatewayAnswering {
            requests++
            json(HttpStatusCode.OK, VALID_REPORT_JSON)
        }

        assertIs<ValidationOutcome.Validated>(gateway.validateNew(TEST_SESSION, exactLimit))
        assertIs<ValidationOutcome.Validated>(gateway.validateVersion(TEST_SESSION, "0b8f", exactLimit))
        assertEquals(2, requests)
    }

    // ── Respuestas ──

    @Test
    fun `200 returns validated report for valid without warnings`() = runTest {
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.OK, VALID_REPORT_JSON) }
            .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES)

        assertEquals(ValidationOutcome.Validated(CourseReport.EMPTY), outcome)
    }

    @Test
    fun `200 returns validated report with warnings on version validation`() = runTest {
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.OK, VALID_REPORT_WITH_WARNING_JSON) }
            .validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES)

        assertEquals(
            ValidationOutcome.Validated(
                CourseReport(
                    warnings = listOf(
                        CourseIssue("REMOVED_IDS", "", "Se eliminaron módulos de la versión anterior"),
                    ),
                ),
            ),
            outcome,
        )
    }

    @Test
    fun `200 returns validated report when course has validation errors`() = runTest {
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.OK, INVALID_REPORT_JSON) }
            .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES)

        assertEquals(
            ValidationOutcome.Validated(
                CourseReport(
                    errors = listOf(
                        CourseIssue("UNKNOWN_PREREQUISITE", "/modules/1/requires/0", "Falta el prerrequisito"),
                    ),
                    warnings = listOf(
                        CourseIssue("DUPLICATE_TAG", "/tags/1", "Etiqueta duplicada"),
                    ),
                ),
            ),
            outcome,
        )
    }

    @Test
    fun `200 ignores extra fields in report and issues`() = runTest {
        val body = """
            {
              "valid": true,
              "schemaVersion": 1,
              "errors": [],
              "warnings": [
                {"code": "REMOVED_IDS", "path": "", "message": "Ids borrados", "extra": 42}
              ]
            }
        """.trimIndent()
        val outcome = publicationGatewayAnswering { json(HttpStatusCode.OK, body) }
            .validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES)

        val validated = assertIs<ValidationOutcome.Validated>(outcome)
        assertEquals(1, validated.report.warnings.size)
    }

    @Test
    fun `http error statuses map to specific validation outcomes`() = runTest {
        assertEquals(
            ValidationOutcome.SessionExpired,
            publicationGatewayAnswering { json(HttpStatusCode.Unauthorized, apiErrorJson("authorization", "UNAUTHENTICATED")) }
                .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.NotAuthor,
            publicationGatewayAnswering { json(HttpStatusCode.Forbidden, apiErrorJson("id", "NOT_COURSE_AUTHOR")) }
                .validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.NotFound,
            publicationGatewayAnswering { json(HttpStatusCode.NotFound, apiErrorJson("id", "COURSE_NOT_FOUND")) }
                .validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.IdenticalToLatest,
            publicationGatewayAnswering { json(HttpStatusCode.Conflict, apiErrorJson("body", "IDENTICAL_TO_LATEST")) }
                .validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.TooLarge,
            publicationGatewayAnswering { json(HttpStatusCode.PayloadTooLarge, INVALID_REPORT_JSON) }
                .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `unexpected status or malformed 200 body is server failure`() = runTest {
        assertEquals(
            ValidationOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.BadRequest, INVALID_REPORT_JSON) }
                .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.UnprocessableEntity, INVALID_REPORT_JSON) }
                .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }
                .validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.OK, "<html>broken</html>") }
                .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.ServerFailure,
            publicationGatewayAnswering { json(HttpStatusCode.OK, """{"valid": false, "errors": [], "warnings": []}""") }
                .validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `network error and timeout are unreachable`() = runTest {
        assertEquals(
            ValidationOutcome.Unreachable,
            publicationGatewayAnswering { throw IOException("sin red") }.validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )
        assertEquals(
            ValidationOutcome.Unreachable,
            publicationGatewayAnswering { throw HttpRequestTimeoutException("url", 60_000) }
                .validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
        )
    }

    @Test
    fun `session token never appears in validation outcomes`() = runTest {
        val outcomes = listOf(
            publicationGatewayAnswering { json(HttpStatusCode.OK, INVALID_REPORT_JSON) }.validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
            publicationGatewayAnswering { json(HttpStatusCode.Unauthorized, "{}") }.validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
            publicationGatewayAnswering { json(HttpStatusCode.InternalServerError, "{}") }.validateVersion(TEST_SESSION, "0b8f", SAMPLE_COURSE_BYTES),
            publicationGatewayAnswering { throw IOException("sin red") }.validateNew(TEST_SESSION, SAMPLE_COURSE_BYTES),
        )

        outcomes.forEach { outcome ->
            assertFalse(TEST_TOKEN in outcome.toString())
        }
    }
}
