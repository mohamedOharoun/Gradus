package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: los cursos publicados por el usuario autenticado (docs/tareas/subida-cursos.md, paso 1a).
 * Devuelve únicamente la última versión de cada curso, sin cargar el JSON completo de hasta 2 MiB.
 */
public final class ListMyCourses {

    private final CourseRepository courses;

    public ListMyCourses(CourseRepository courses) {
        this.courses = Objects.requireNonNull(courses, "courses");
    }

    /** {@code authorId} sale de la sesión: cada usuario solo lista sus propios cursos. */
    public List<CourseSummary> list(UUID authorId) {
        Objects.requireNonNull(authorId, "authorId");
        return courses.findByAuthor(authorId);
    }
}
