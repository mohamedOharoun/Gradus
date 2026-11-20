package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Caso de uso: leer un curso publicado con su contenido, en su última versión o en una concreta (§4.3). */
public final class FindCourse {

    private final CourseRepository repository;

    public FindCourse(CourseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<CourseVersion> latest(UUID courseId) {
        return repository.findLatest(courseId);
    }

    /** Una versión que no es un entero positivo no existe: ni se pregunta al repositorio. */
    public Optional<CourseVersion> version(UUID courseId, int version) {
        return version < CourseSummary.FIRST_VERSION ? Optional.empty() : repository.findVersion(courseId, version);
    }
}
