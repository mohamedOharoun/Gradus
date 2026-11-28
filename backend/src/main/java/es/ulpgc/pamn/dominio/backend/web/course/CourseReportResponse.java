package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;

import java.util.List;

/** El informe de validación (§3.7): {@code valid} es verdadero si y solo si {@code errors} está vacío. */
public record CourseReportResponse(boolean valid, List<IssueResponse> errors, List<IssueResponse> warnings) {

    public static CourseReportResponse from(CourseReport report) {
        return new CourseReportResponse(
                report.isValid(),
                report.errors().stream().map(IssueResponse::from).toList(),
                report.warnings().stream().map(IssueResponse::from).toList());
    }
}
