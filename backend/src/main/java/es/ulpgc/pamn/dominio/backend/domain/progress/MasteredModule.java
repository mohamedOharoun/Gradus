package es.ulpgc.pamn.dominio.backend.domain.progress;

import java.time.Instant;
import java.util.Objects;

/** Un módulo que el alumno ha dominado, y cuándo lo hizo por primera vez. */
public record MasteredModule(String moduleId, Instant masteredAt) {

    public MasteredModule {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(masteredAt, "masteredAt");
        if (moduleId.isBlank()) {
            throw new IllegalArgumentException("moduleId must not be blank");
        }
    }
}
