package es.ulpgc.pamn.dominio.course.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CourseReportJsonTest {

    @Test
    fun `round trip preserves errors and warnings`() {
        val report = CourseReport(
            errors = listOf(
                CourseIssue("UNKNOWN_PREREQUISITE", "/modules/1/requires/0", "Falta el prerrequisito"),
                CourseIssue("MISSING_TITLE", "", "El título es obligatorio"),
            ),
            warnings = listOf(
                CourseIssue("DUPLICATE_TAG", "/tags/1", "Etiqueta duplicada"),
            ),
        )

        val encoded = CourseReportJson.encode(report)
        assertEquals(report, CourseReportJson.decode(encoded))
    }

    @Test
    fun `round trip preserves report with only warnings`() {
        val report = CourseReport(
            warnings = listOf(
                CourseIssue("REMOVED_IDS", "", "Se eliminaron módulos de la versión anterior"),
            ),
        )

        val encoded = CourseReportJson.encode(report)
        assertEquals(report, CourseReportJson.decode(encoded))
    }

    @Test
    fun `round trip preserves empty report`() {
        val encoded = CourseReportJson.encode(CourseReport.EMPTY)
        assertEquals(CourseReport.EMPTY, CourseReportJson.decode(encoded))
    }

    @Test
    fun `decode ignores unknown extra fields`() {
        val json = """
            {
              "valid": false,
              "extraTopLevel": "ignored",
              "errors": [
                {"code": "BAD_SCHEMA", "path": "/title", "message": "Falta título", "severity": "high"}
              ],
              "warnings": [
                {"code": "REMOVED_IDS", "path": "", "message": "Ids borrados", "count": 2}
              ]
            }
        """.trimIndent()

        val expected = CourseReport(
            errors = listOf(CourseIssue("BAD_SCHEMA", "/title", "Falta título")),
            warnings = listOf(CourseIssue("REMOVED_IDS", "", "Ids borrados")),
        )
        assertEquals(expected, CourseReportJson.decode(json))
    }

    @Test
    fun `decode returns null for malformed json or missing required fields`() {
        assertNull(CourseReportJson.decode(""))
        assertNull(CourseReportJson.decode("not json"))
        assertNull(CourseReportJson.decode("{}"))
        assertNull(CourseReportJson.decode("""{"valid": true}"""))
        assertNull(CourseReportJson.decode("""{"errors": [], "warnings": []}"""))
        assertNull(CourseReportJson.decode("""{"valid": true, "errors": [], "warnings": [{"code": "X"}]}"""))
    }

    @Test
    fun `decode returns null when valid disagrees with errors list`() {
        val trueWithErrors = """
            {"valid": true, "errors": [{"code": "X", "path": "", "message": "Error"}], "warnings": []}
        """.trimIndent()
        val falseWithoutErrors = """{"valid": false, "errors": [], "warnings": []}"""

        assertNull(CourseReportJson.decode(trueWithErrors))
        assertNull(CourseReportJson.decode(falseWithoutErrors))
    }
}
