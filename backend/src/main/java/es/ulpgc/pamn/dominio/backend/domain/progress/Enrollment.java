package es.ulpgc.pamn.dominio.backend.domain.progress;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** La inscripción de un alumno en un curso, con los módulos que lleva dominados. Ya validada. */
public record Enrollment(UUID courseId, Instant enrolledAt, List<MasteredModule> mastered) {

    public Enrollment {
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(enrolledAt, "enrolledAt");
        mastered = List.copyOf(mastered);
    }
}
