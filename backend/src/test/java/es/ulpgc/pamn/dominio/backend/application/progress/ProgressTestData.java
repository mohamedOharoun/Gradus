package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.EnrollmentDraft;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModuleDraft;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Datos de ejemplo compartidos por los tests de {@link SyncProgress} (Object Mother). */
final class ProgressTestData {

    static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    static final Instant EARLY = Instant.parse("2026-11-01T10:00:00Z");
    static final Instant LATE = Instant.parse("2026-11-10T10:00:00Z");

    static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-000000000002");

    static final UUID GRAPHS = UUID.fromString("0b8f6d2e-0000-4000-8000-000000000001");
    static final UUID SORTING = UUID.fromString("0b8f6d2e-0000-4000-8000-000000000002");
    static final UUID MISSING = UUID.fromString("0b8f6d2e-0000-4000-8000-0000000000ff");
    static final UUID MISSING_TOO = UUID.fromString("0b8f6d2e-0000-4000-8000-0000000000fe");

    private ProgressTestData() {
    }

    static EnrollmentDraft draft(String courseId, Instant enrolledAt, MasteredModuleDraft... modules) {
        return new EnrollmentDraft(courseId, enrolledAt, Arrays.asList(modules));
    }

    static EnrollmentDraft draft(UUID courseId, Instant enrolledAt, MasteredModuleDraft... modules) {
        return draft(courseId.toString(), enrolledAt, modules);
    }

    static MasteredModuleDraft module(String moduleId, Instant masteredAt) {
        return new MasteredModuleDraft(moduleId, masteredAt);
    }

    static Enrollment enrollment(UUID courseId, Instant enrolledAt, MasteredModule... modules) {
        return new Enrollment(courseId, enrolledAt, List.of(modules));
    }

    static MasteredModule mastered(String moduleId, Instant masteredAt) {
        return new MasteredModule(moduleId, masteredAt);
    }
}
