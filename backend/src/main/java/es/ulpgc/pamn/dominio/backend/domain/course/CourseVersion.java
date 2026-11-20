package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.Objects;
import java.util.UUID;

/**
 * Una versión publicada con su contenido (§4.4, «GET con contenido»). Comprueba que el contenido es el
 * que describe su hash: si no lo fuera, los bytes servidos no serían los publicados (§4.1).
 */
public record CourseVersion(CourseSummary summary, CourseContent content) {

    public CourseVersion {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(content, "content");
        if (!summary.metadata().sha256().equals(content.sha256())) {
            throw new IllegalArgumentException("El contenido de la versión " + summary.version() + " del curso "
                    + summary.id() + " no coincide con su SHA-256");
        }
    }

    /** Factory method: el borrador convertido en la versión {@code version} del curso {@code courseId}. */
    public static CourseVersion of(UUID courseId, int version, CourseAuthor author, CourseVersionDraft draft) {
        return new CourseVersion(
                new CourseSummary(courseId, version, author, draft.publishedAt(), draft.metadata()), draft.content());
    }
}
