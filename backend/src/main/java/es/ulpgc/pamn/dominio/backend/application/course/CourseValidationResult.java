package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;

import java.util.Objects;

/** Los dos desenlaces de validar sin guardar (§4.3): 200 con el informe, sea válido o no, o 413. */
public sealed interface CourseValidationResult {

    record Checked(CourseReport report) implements CourseValidationResult {
        public Checked {
            Objects.requireNonNull(report, "report");
        }
    }

    record TooLarge(CourseReport report) implements CourseValidationResult {
        public TooLarge {
            Objects.requireNonNull(report, "report");
        }
    }
}
