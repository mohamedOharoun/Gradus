package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;

import java.util.List;

/** Una página de la búsqueda (§4.5): {@code {"items": [ … ], "page": 0, "size": 20, "total": 37}}. */
public record CoursePageResponse(List<CourseResponse> items, int page, int size, long total) {

    public static CoursePageResponse from(Page<CourseSummary> page) {
        Page<CourseResponse> responses = page.map(CourseResponse::from);
        return new CoursePageResponse(responses.items(), page.request().page(), page.request().size(), page.total());
    }
}
