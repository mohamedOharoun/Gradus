package es.ulpgc.pamn.dominio.backend.domain.progress;

import java.time.Instant;
import java.util.Objects;

/** Un módulo dominado tal como llega en la petición: el {@code moduleId} aún no está validado. */
public record MasteredModuleDraft(String moduleId, Instant masteredAt) {

    public MasteredModuleDraft {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(masteredAt, "masteredAt");
    }
}
