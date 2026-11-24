package es.ulpgc.pamn.dominio.backend.domain.progress;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Una inscripción tal como llega en la petición: el {@code courseId} es texto y aún no está validado. */
public record EnrollmentDraft(String courseId, Instant enrolledAt, List<MasteredModuleDraft> mastered) {

    public EnrollmentDraft {
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(enrolledAt, "enrolledAt");
        mastered = List.copyOf(mastered);
    }
}
