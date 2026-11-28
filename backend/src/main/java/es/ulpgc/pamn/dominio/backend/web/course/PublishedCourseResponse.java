package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;

import java.util.List;

/** Respuesta 201 al publicar (§4.4): {@code {"course": {…metadatos…}, "warnings": [ … ]}}. */
public record PublishedCourseResponse(CourseResponse course, List<IssueResponse> warnings) {

    public static PublishedCourseResponse from(CourseSummary course, CourseReport report) {
        return new PublishedCourseResponse(CourseResponse.from(course),
                report.warnings().stream().map(IssueResponse::from).toList());
    }
}
