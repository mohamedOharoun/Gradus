package es.ulpgc.pamn.dominio.backend.domain.course;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Lo que se entrega al repositorio para publicar una versión (§4.2): los metadatos, el texto exacto y el
 * instante. El id del curso y el número de versión los pone el repositorio, que es quien sabe cuál toca.
 */
public record CourseVersionDraft(CourseMetadata metadata, CourseContent content, Instant publishedAt) {

    public CourseVersionDraft {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(content, "content");
        // PostgreSQL guarda microsegundos: truncando aquí, lo devuelto coincide con lo guardado.
        publishedAt = Objects.requireNonNull(publishedAt, "publishedAt").truncatedTo(ChronoUnit.MICROS);
        if (!metadata.sha256().equals(content.sha256())) {
            throw new IllegalArgumentException("Los metadatos no corresponden a este contenido");
        }
    }

    /** Factory method: deriva los metadatos del curso ya leído y validado. */
    public static CourseVersionDraft of(CourseDocument document, CourseContent content, Instant publishedAt) {
        return new CourseVersionDraft(CourseMetadata.of(document, content), content, publishedAt);
    }
}
