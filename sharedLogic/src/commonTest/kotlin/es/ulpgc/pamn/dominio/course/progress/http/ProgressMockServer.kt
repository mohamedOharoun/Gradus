package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import kotlin.time.Instant

// Ayudas comunes a los tests de HttpProgressGateway: un servidor falso (MockEngine) sin red.

internal fun progressGatewayAnswering(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) = HttpProgressGateway(gradusHttpClient(MockEngine(handler)), baseUrl = "http://test")

internal const val TEST_TOKEN = "tok-secreto-123"

internal val testSession = Session(
    token = TEST_TOKEN,
    expiresAt = "2026-12-01T00:00:00Z",
    account = Account(id = "u1", name = "Ana", email = "ana@ulpgc.es", createdAt = "2026-11-17T10:00:00Z"),
)

internal val noPending = ProgressSnapshot(emptyList())

// Un 200 válido, como lo devuelve el servidor.
internal const val SYNC_OK_JSON = """
    {"enrollments": [{"courseId": "0b8f6d2e", "enrolledAt": "2026-11-24T10:00:00Z",
                      "mastered": [{"moduleId": "bfs", "masteredAt": "2026-11-24T10:20:00.5Z"}]}],
     "skipped": [{"courseId": "9a1c", "code": "COURSE_NOT_FOUND"},
                 {"courseId": "0b8f6d2e", "moduleId": " ", "code": "REQUIRED"}]}
"""

internal fun snapshotOf(courseId: String, vararg moduleIds: String) = ProgressSnapshot(
    listOf(
        EnrollmentSnapshot(
            Enrollment(courseId, Instant.parse("2026-11-24T10:00:00Z")),
            moduleIds.map { MasteredModule(it, Instant.parse("2026-11-24T10:20:00Z")) },
        ),
    ),
)
