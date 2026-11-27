package es.ulpgc.pamn.dominio.course.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PublishOutcomeTest {

    private val firstError = CourseReport(
        errors = listOf(CourseIssue("INVALID_JSON", "", "JSON mal formado")),
    )
    private val secondError = CourseReport(
        errors = listOf(CourseIssue("UNKNOWN_PREREQUISITE", "/modules/0/requires/0", "No existe")),
    )
    private val onlyWarnings = CourseReport(
        warnings = listOf(CourseIssue("REMOVED_IDS", "", "Se eliminaron módulos")),
    )

    @Test
    fun `invalid publish outcome accepts a report with errors`() {
        val outcome = PublishOutcome.Invalid(firstError)
        assertEquals(firstError, outcome.report)
    }

    @Test
    fun `invalid publish outcome rejects an empty report`() {
        assertFailsWith<IllegalArgumentException> {
            PublishOutcome.Invalid(CourseReport.EMPTY)
        }
    }

    @Test
    fun `invalid publish outcome rejects a report with only warnings`() {
        assertFailsWith<IllegalArgumentException> {
            PublishOutcome.Invalid(onlyWarnings)
        }
    }

    @Test
    fun `invalid publish outcome enforces invariant when using copy`() {
        val invalid = PublishOutcome.Invalid(firstError)

        assertEquals(PublishOutcome.Invalid(secondError), invalid.copy(report = secondError))
        assertFailsWith<IllegalArgumentException> {
            invalid.copy(report = CourseReport.EMPTY)
        }
        assertFailsWith<IllegalArgumentException> {
            invalid.copy(report = onlyWarnings)
        }
    }
}
