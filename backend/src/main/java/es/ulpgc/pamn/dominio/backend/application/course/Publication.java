package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;

import java.util.Objects;
import java.util.UUID;

/**
 * Los desenlaces de publicar, compartidos por {@link PublishResult} (curso nuevo) y
 * {@link PublishVersionResult} (versión nueva). Cada record implementa los resultados en los que puede
 * aparecer: así el compilador sabe que publicar un curso nuevo nunca da, por ejemplo, {@code NotAuthor}.
 */
public final class Publication {

    private Publication() {
    }

    /** 201: publicado. El informe solo tiene avisos (incluido {@code REMOVED_IDS}, si toca). */
    public record Published(CourseSummary course, CourseReport report) implements PublishResult, PublishVersionResult {
        public Published {
            Objects.requireNonNull(course, "course");
            if (!report.isValid()) {
                throw new IllegalArgumentException("Un curso publicado no puede tener errores");
            }
        }
    }

    /** 413. */
    public record TooLarge(CourseReport report) implements PublishResult, PublishVersionResult {
    }

    /** 400: capa 1. */
    public record Unreadable(CourseReport report) implements PublishResult, PublishVersionResult {
    }

    /** 422: capas 2–5. */
    public record Rejected(CourseReport report) implements PublishResult, PublishVersionResult {
    }

    /** 404. */
    public record CourseNotFound(UUID courseId) implements PublishVersionResult {
    }

    /** 403. */
    public record NotAuthor(UUID courseId) implements PublishVersionResult {
    }

    /** 409. */
    public record IdenticalToLatest(UUID courseId) implements PublishVersionResult {
    }
}
