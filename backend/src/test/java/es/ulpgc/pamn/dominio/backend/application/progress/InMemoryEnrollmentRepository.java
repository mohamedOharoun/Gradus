package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.ProgressMerge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Repositorio falso: un progreso por usuario, fusionado con {@link ProgressMerge}, y registro de llamadas. */
final class InMemoryEnrollmentRepository implements EnrollmentRepository {

    record MergeCall(UUID userId, List<Enrollment> enrollments) {
    }

    private final Map<UUID, List<Enrollment>> byUser = new HashMap<>();
    private final List<MergeCall> mergeCalls = new ArrayList<>();
    private int findAllCalls;

    List<MergeCall> mergeCalls() {
        return List.copyOf(mergeCalls);
    }

    int findAllCalls() {
        return findAllCalls;
    }

    @Override
    public void mergeAll(UUID userId, List<Enrollment> enrollments) {
        mergeCalls.add(new MergeCall(userId, List.copyOf(enrollments)));
        byUser.merge(userId, List.copyOf(enrollments), ProgressMerge::merge);
    }

    @Override
    public List<Enrollment> findAll(UUID userId) {
        findAllCalls++;
        return byUser.getOrDefault(userId, List.of());
    }
}
