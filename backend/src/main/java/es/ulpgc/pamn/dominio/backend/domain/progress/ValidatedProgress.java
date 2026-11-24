package es.ulpgc.pamn.dominio.backend.domain.progress;

import java.util.List;

/** Resultado de validar una petición: lo que se puede guardar y lo que se omite, en orden de petición. */
public record ValidatedProgress(List<Enrollment> valid, List<SkippedEntry> skipped) {

    public ValidatedProgress {
        valid = List.copyOf(valid);
        skipped = List.copyOf(skipped);
    }
}
