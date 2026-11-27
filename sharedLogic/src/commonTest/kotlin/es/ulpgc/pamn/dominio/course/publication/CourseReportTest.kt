package es.ulpgc.pamn.dominio.course.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourseReportTest {

    @Test
    fun `empty report is valid and has no warnings`() {
        assertTrue(CourseReport.EMPTY.isValid)
        assertFalse(CourseReport.EMPTY.hasWarnings)
        assertEquals(emptyList(), CourseReport.EMPTY.errors)
        assertEquals(emptyList(), CourseReport.EMPTY.warnings)
    }

    @Test
    fun `report with only warnings is valid and marks hasWarnings`() {
        val report = CourseReport(
            warnings = listOf(
                CourseIssue(
                    code = "DUPLICATE_TAG",
                    path = "/tags/1",
                    message = "Etiqueta duplicada",
                ),
            ),
        )

        assertTrue(report.isValid)
        assertTrue(report.hasWarnings)
    }

    @Test
    fun `report with errors is not valid`() {
        val report = CourseReport(
            errors = listOf(
                CourseIssue(
                    code = "UNKNOWN_PREREQUISITE",
                    path = "/modules/1/requires/0",
                    message = "El módulo requiere otro que no existe",
                ),
            ),
        )

        assertFalse(report.isValid)
        assertFalse(report.hasWarnings)
    }

    @Test
    fun `plain text formatting includes both errors and warnings in order`() {
        val report = CourseReport(
            errors = listOf(
                CourseIssue(
                    code = "UNKNOWN_PREREQUISITE",
                    path = "/modules/1/requires/0",
                    message = "Falta el prerrequisito",
                ),
                CourseIssue(
                    code = "MISSING_TITLE",
                    path = "/title",
                    message = "El título es obligatorio",
                ),
            ),
            warnings = listOf(
                CourseIssue(
                    code = "DUPLICATE_TAG",
                    path = "/tags/1",
                    message = "Etiqueta duplicada",
                ),
            ),
        )

        val expected = """
            Errores (2):
            - [UNKNOWN_PREREQUISITE] /modules/1/requires/0: Falta el prerrequisito
            - [MISSING_TITLE] /title: El título es obligatorio

            Avisos (1):
            - [DUPLICATE_TAG] /tags/1: Etiqueta duplicada
        """.trimIndent()

        assertEquals(expected, report.formatAsPlainText())
    }

    @Test
    fun `plain text formatting with only errors omits warnings section`() {
        val report = CourseReport(
            errors = listOf(
                CourseIssue(
                    code = "SCHEMA_ERROR",
                    path = "/modules",
                    message = "Debe ser una lista",
                ),
            ),
        )

        val expected = """
            Errores (1):
            - [SCHEMA_ERROR] /modules: Debe ser una lista
        """.trimIndent()

        assertEquals(expected, report.formatAsPlainText())
    }

    @Test
    fun `plain text formatting with only warnings omits errors section`() {
        val report = CourseReport(
            warnings = listOf(
                CourseIssue(
                    code = "REMOVED_IDS",
                    path = "",
                    message = "Se eliminaron módulos de la versión anterior",
                ),
            ),
        )

        val expected = """
            Avisos (1):
            - [REMOVED_IDS] raíz: Se eliminaron módulos de la versión anterior
        """.trimIndent()

        assertEquals(expected, report.formatAsPlainText())
    }

    @Test
    fun `plain text formatting of empty report returns empty message`() {
        assertEquals("Sin errores ni avisos.", CourseReport.EMPTY.formatAsPlainText())
    }

    @Test
    fun `plain text formatting replaces empty or blank path with root label`() {
        val report = CourseReport(
            errors = listOf(
                CourseIssue(code = "INVALID_JSON", path = "", message = "JSON roto"),
                CourseIssue(code = "EMPTY_BODY", path = "   ", message = "Cuerpo vacío"),
            ),
        )

        val expected = """
            Errores (2):
            - [INVALID_JSON] raíz: JSON roto
            - [EMPTY_BODY] raíz: Cuerpo vacío
        """.trimIndent()

        assertEquals(expected, report.formatAsPlainText())
    }

    @Test
    fun `plain text formatting supports localized labels`() {
        val englishLabels = ReportTextLabels(
            errorsHeading = "Errors",
            warningsHeading = "Warnings",
            rootPathLabel = "root",
            emptyReportMessage = "No errors or warnings.",
        )

        assertEquals(
            "No errors or warnings.",
            CourseReport.EMPTY.formatAsPlainText(englishLabels),
        )

        val report = CourseReport(
            errors = listOf(
                CourseIssue(code = "INVALID_JSON", path = "", message = "Malformed JSON"),
            ),
            warnings = listOf(
                CourseIssue(code = "REMOVED_IDS", path = "/modules", message = "Removed ids"),
            ),
        )

        val expected = """
            Errors (1):
            - [INVALID_JSON] root: Malformed JSON

            Warnings (1):
            - [REMOVED_IDS] /modules: Removed ids
        """.trimIndent()

        assertEquals(expected, report.formatAsPlainText(englishLabels))
    }
}
