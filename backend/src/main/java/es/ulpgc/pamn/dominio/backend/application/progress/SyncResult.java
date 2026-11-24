package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.SkippedEntry;

import java.util.List;

/**
 * Resultado de sincronizar: todo el progreso del usuario tras fusionar, y lo enviado que no se guardó
 * (primero lo omitido al validar, después los cursos inexistentes, cada grupo en orden de petición).
 */
public record SyncResult(List<Enrollment> enrollments, List<SkippedEntry> skipped) {

    public SyncResult {
        enrollments = List.copyOf(enrollments);
        skipped = List.copyOf(skipped);
    }
}
