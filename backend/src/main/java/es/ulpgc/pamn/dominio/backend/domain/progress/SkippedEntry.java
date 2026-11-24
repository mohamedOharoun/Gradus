package es.ulpgc.pamn.dominio.backend.domain.progress;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;

import java.util.Objects;
import java.util.Optional;

/**
 * Algo que se envió y no se guardó, con el motivo. Sin {@code moduleId}: se omitió la inscripción
 * entera; con {@code moduleId}: solo ese módulo. Ambos textos van tal como llegaron.
 */
public record SkippedEntry(String courseId, Optional<String> moduleId, ErrorCode code) {

    public SkippedEntry {
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(code, "code");
    }

    /** Se omitió la inscripción entera con sus módulos. */
    public static SkippedEntry enrollment(String courseId, ErrorCode code) {
        return new SkippedEntry(courseId, Optional.empty(), code);
    }

    /** Se omitió solo este módulo; la inscripción y sus otros módulos sí se guardaron. */
    public static SkippedEntry module(String courseId, String moduleId, ErrorCode code) {
        return new SkippedEntry(courseId, Optional.of(moduleId), code);
    }
}
