package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.EnrollmentDraft;
import es.ulpgc.pamn.dominio.backend.domain.progress.ProgressMerge;
import es.ulpgc.pamn.dominio.backend.domain.progress.ProgressValidation;
import es.ulpgc.pamn.dominio.backend.domain.progress.SkippedEntry;
import es.ulpgc.pamn.dominio.backend.domain.progress.ValidatedProgress;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Caso de uso: sincronizar el progreso de un usuario (contrato {@code api-progreso.md}).
 * Validar → quitar repetidos → comprobar existencia (una consulta) → {@code mergeAll} → {@code findAll}.
 * Es un <em>pipeline</em> de funciones puras del dominio entre dos llamadas a puertos. No lee, fusiona y
 * guarda: la fusión es del repositorio, que la hace de forma atómica.
 */
public final class SyncProgress {

    private final EnrollmentRepository enrollments;
    private final CourseExistence courses;
    private final Clock clock;

    public SyncProgress(EnrollmentRepository enrollments, CourseExistence courses, Clock clock) {
        this.enrollments = Objects.requireNonNull(enrollments, "enrollments");
        this.courses = Objects.requireNonNull(courses, "courses");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** El usuario es siempre el de la sesión; nunca se lee del cuerpo. */
    public SyncResult sync(UUID userId, List<EnrollmentDraft> drafts) {
        ValidatedProgress validated = ProgressValidation.validate(drafts, clock.instant());
        List<Enrollment> distinct = ProgressMerge.merge(List.of(), validated.valid());
        Set<UUID> existing = existingAmong(distinct);

        List<Enrollment> toSave = distinct.stream().filter(e -> existing.contains(e.courseId())).toList();
        if (!toSave.isEmpty()) {
            enrollments.mergeAll(userId, toSave);
        }

        List<SkippedEntry> unknownCourses = distinct.stream()
                .filter(e -> !existing.contains(e.courseId()))
                .map(e -> SkippedEntry.enrollment(e.courseId().toString(), ErrorCode.COURSE_NOT_FOUND))
                .toList();
        return new SyncResult(enrollments.findAll(userId),
                Stream.concat(validated.skipped().stream(), unknownCourses.stream()).toList());
    }

    /** Sin nada válido no se consulta. */
    private Set<UUID> existingAmong(List<Enrollment> distinct) {
        if (distinct.isEmpty()) {
            return Set.of();
        }
        return courses.findExisting(distinct.stream().map(Enrollment::courseId).collect(Collectors.toSet()));
    }
}
