package es.ulpgc.pamn.dominio.course.publication.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData

// Ayudas y respuestas de ejemplo para los tests de HttpCoursePublicationGateway con MockEngine.

internal const val TEST_TOKEN = "secret-bearer-token-98765"

internal val TEST_SESSION = Session(
    token = TEST_TOKEN,
    expiresAt = "2026-12-25T10:00:00Z",
    account = Account("5c1d", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"),
)

internal val SAMPLE_COURSE_BYTES = """{"formatVersion": 1, "title": "Grafos"}""".encodeToByteArray()

internal fun publicationGatewayAnswering(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) = HttpCoursePublicationGateway(gradusHttpClient(MockEngine(handler)), baseUrl = "http://test")

internal const val SUMMARY_JSON = """
    {
      "id": "0b8f",
      "version": 2,
      "author": {"id": "5c1d", "name": "Ana"},
      "publishedAt": "2026-11-18T08:35:00Z",
      "formatVersion": 1,
      "title": "Introducción a los grafos",
      "description": "Recorridos básicos",
      "language": "es",
      "tags": ["grafos", "algoritmos"],
      "moduleCount": 2,
      "sha256": "9c1e"
    }
"""

internal const val SUMMARY_WITH_EXTRA_METRICS_JSON = """
    {
      "id": "0b8f",
      "version": 2,
      "author": {"id": "5c1d", "name": "Ana"},
      "publishedAt": "2026-11-18T08:35:00Z",
      "formatVersion": 1,
      "title": "Introducción a los grafos",
      "description": "Recorridos básicos",
      "language": "es",
      "tags": ["grafos", "algoritmos"],
      "moduleCount": 2,
      "sha256": "9c1e",
      "studentCount": 48,
      "averageRating": 4.6
    }
"""

internal const val VALID_REPORT_JSON = """{"valid": true, "errors": [], "warnings": []}"""

internal const val VALID_REPORT_WITH_WARNING_JSON = """
    {
      "valid": true,
      "errors": [],
      "warnings": [
        {"code": "REMOVED_IDS", "path": "", "message": "Se eliminaron módulos de la versión anterior"}
      ]
    }
"""

internal const val INVALID_REPORT_JSON = """
    {
      "valid": false,
      "errors": [
        {"code": "UNKNOWN_PREREQUISITE", "path": "/modules/1/requires/0", "message": "Falta el prerrequisito"}
      ],
      "warnings": [
        {"code": "DUPLICATE_TAG", "path": "/tags/1", "message": "Etiqueta duplicada"}
      ]
    }
"""

internal fun apiErrorJson(field: String, code: String) =
    """{"errors": [{"field": "$field", "code": "$code", "message": "Detalle"}]}"""
