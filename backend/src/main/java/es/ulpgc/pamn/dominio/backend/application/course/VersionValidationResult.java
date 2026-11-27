package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;

import java.util.Objects;
import java.util.UUID;

/**
 * Los cinco desenlaces de validar una versión nueva sin guardarla: 200 con el informe (válido —con
 * {@code REMOVED_IDS} si toca— o con errores), 413 si supera 2 MiB, 404 si el curso no existe, 403 si no
 * es el autor y 409 si el contenido es idéntico al de la última versión.
 */
public sealed interface VersionValidationResult {

    /** 200: informe de validación (válido o con errores de sintaxis, esquema o reglas). */
    record Checked(CourseReport report) implements VersionValidationResult {
        public Checked {
            Objects.requireNonNull(report, "report");
        }
    }

    /** 413: el cuerpo supera el límite de tamaño. */
    record TooLarge(CourseReport report) implements VersionValidationResult {
        public TooLarge {
            Objects.requireNonNull(report, "report");
        }
    }

    /** 404: el curso no existe. */
    record CourseNotFound(UUID courseId) implements VersionValidationResult {
        public CourseNotFound {
            Objects.requireNonNull(courseId, "courseId");
        }
    }

    /** 403: quien valida no es el autor del curso. */
    record NotAuthor(UUID courseId) implements VersionValidationResult {
        public NotAuthor {
            Objects.requireNonNull(courseId, "courseId");
        }
    }

    /** 409: el contenido es idéntico al de la última versión publicada. */
    record IdenticalToLatest(UUID courseId) implements VersionValidationResult {
        public IdenticalToLatest {
            Objects.requireNonNull(courseId, "courseId");
        }
    }
}
