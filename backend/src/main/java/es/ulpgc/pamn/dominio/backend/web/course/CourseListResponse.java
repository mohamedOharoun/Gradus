package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;

import java.util.List;

/**
 * {@code {"courses": [...]}}: los cursos publicados por el usuario autenticado, en el orden que devuelve
 * {@code ListMyCourses} (última publicación primero). Se envuelve en un objeto para poder añadir campos
 * más adelante sin romper la app.
 */
public record CourseListResponse(List<CourseResponse> courses) {

    public CourseListResponse {
        courses = List.copyOf(courses);
    }

    public static CourseListResponse from(List<CourseSummary> courses) {
        return new CourseListResponse(courses.stream().map(CourseResponse::from).toList());
    }
}
