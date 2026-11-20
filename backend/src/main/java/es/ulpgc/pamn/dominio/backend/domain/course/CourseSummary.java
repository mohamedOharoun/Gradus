package es.ulpgc.pamn.dominio.backend.domain.course;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Los metadatos de una versión publicada (§4.4), sin su contenido: es lo que se devuelve al publicar y en
 * cada resultado de la búsqueda, para no cargar hasta 2 MiB por resultado.
 */
public record CourseSummary(UUID id, int version, CourseAuthor author, Instant publishedAt, CourseMetadata metadata) {

    public static final int FIRST_VERSION = 1;

    public CourseSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(metadata, "metadata");
        if (version < FIRST_VERSION) {
            throw new IllegalArgumentException("Las versiones empiezan en " + FIRST_VERSION + ": " + version);
        }
    }
}
