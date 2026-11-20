package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.course.PageRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: dónde se guardan los cursos y sus versiones. Las versiones son inmutables: nunca se
 * actualizan ni se borran (§4.1). La aplicación no sabe si es PostgreSQL o memoria.
 */
public interface CourseRepository {

    /** Guarda un curso nuevo con su versión 1 (en la misma transacción) y la devuelve. */
    CourseSummary create(UUID courseId, CourseAuthor author, CourseVersionDraft draft);

    /**
     * Añade la versión siguiente a la última, comprobando de forma atómica que el curso existe, que
     * {@code requesterId} es su autor y que el contenido no es idéntico al de la última versión (§4.2).
     */
    AppendVersionResult appendVersion(UUID courseId, UUID requesterId, CourseVersionDraft draft);

    Optional<CourseVersion> findLatest(UUID courseId);

    Optional<CourseVersion> findVersion(UUID courseId, int version);

    /**
     * Busca entre las últimas versiones (§4.5) las que cumplen {@link CourseSearch#matches}, ordenadas por
     * fecha de publicación descendente y después por id del curso, para que el orden sea total.
     */
    Page<CourseSummary> search(CourseSearch search, PageRequest page);
}
