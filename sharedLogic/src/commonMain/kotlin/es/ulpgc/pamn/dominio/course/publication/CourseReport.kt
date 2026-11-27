package es.ulpgc.pamn.dominio.course.publication

// Un problema o aviso devuelto por el validador del servidor sobre un fichero de curso.
// `path` es un JSON Pointer dentro del curso ("" indica la raíz).
data class CourseIssue(
    val code: String,
    val path: String,
    val message: String,
)

// Etiquetas parametrizables para exportar un informe a texto plano en cualquier idioma.
data class ReportTextLabels(
    val errorsHeading: String = "Errores",
    val warningsHeading: String = "Avisos",
    val rootPathLabel: String = "raíz",
    val emptyReportMessage: String = "Sin errores ni avisos.",
)

// Informe de validación de un curso: es válido si y solo si no tiene errores.
data class CourseReport(
    val errors: List<CourseIssue> = emptyList(),
    val warnings: List<CourseIssue> = emptyList(),
) {
    val isValid: Boolean get() = errors.isEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()

    // Genera un resumen legible pensado para «Copiar informe» (p. ej. pegarlo en un LLM).
    fun formatAsPlainText(labels: ReportTextLabels = ReportTextLabels()): String {
        val sections = listOfNotNull(
            formatSection(labels.errorsHeading, errors, labels.rootPathLabel),
            formatSection(labels.warningsHeading, warnings, labels.rootPathLabel),
        )
        return if (sections.isEmpty()) labels.emptyReportMessage else sections.joinToString("\n\n")
    }

    private fun formatSection(
        heading: String,
        issues: List<CourseIssue>,
        rootPathLabel: String,
    ): String? {
        if (issues.isEmpty()) return null
        val lines = issues.map { (code, path, message) ->
            val location = path.ifBlank { rootPathLabel }
            "- [$code] $location: $message"
        }
        return buildString {
            append("$heading (${issues.size}):\n")
            append(lines.joinToString("\n"))
        }
    }

    companion object {
        val EMPTY = CourseReport()
    }
}
