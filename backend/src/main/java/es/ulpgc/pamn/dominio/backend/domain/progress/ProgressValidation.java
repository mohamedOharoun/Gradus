package es.ulpgc.pamn.dominio.backend.domain.progress;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Valida la entrada en bruto de una petición (reglas 4, 7 y 8 de {@code api-progreso.md}).
 * Función pura: sin estado, y la hora llega por parámetro.
 */
public final class ProgressValidation {

    private ProgressValidation() {
    }

    /** Lo que sale de validar una sola inscripción: ella misma (si vale) y lo que se omitió. */
    private record Outcome(Optional<Enrollment> enrollment, List<SkippedEntry> skipped) {
    }

    public static ValidatedProgress validate(List<EnrollmentDraft> drafts, Instant now) {
        List<Outcome> outcomes = drafts.stream().map(draft -> validateOne(draft, now)).toList();
        return new ValidatedProgress(
                outcomes.stream().flatMap(o -> o.enrollment().stream()).toList(),
                outcomes.stream().flatMap(o -> o.skipped().stream()).toList());
    }

    private static Outcome validateOne(EnrollmentDraft draft, Instant now) {
        Optional<UUID> courseId = parseUuid(draft.courseId());
        if (courseId.isEmpty()) {
            return new Outcome(Optional.empty(),
                    List.of(SkippedEntry.enrollment(draft.courseId(), ErrorCode.COURSE_NOT_FOUND)));
        }
        List<MasteredModule> modules = draft.mastered().stream()
                .filter(m -> !m.moduleId().isBlank())
                .map(m -> new MasteredModule(m.moduleId(), clip(m.masteredAt(), now)))
                .toList();
        List<SkippedEntry> skipped = draft.mastered().stream()
                .filter(m -> m.moduleId().isBlank())
                .map(m -> SkippedEntry.module(draft.courseId(), m.moduleId(), ErrorCode.REQUIRED))
                .toList();
        return new Outcome(
                Optional.of(new Enrollment(courseId.get(), clip(draft.enrolledAt(), now), modules)), skipped);
    }

    /** Una fecha futura (reloj del móvil adelantado) se recorta a {@code now}. */
    private static Instant clip(Instant date, Instant now) {
        return date.isAfter(now) ? now : date;
    }

    /** Solo vale el formato canónico de 36 caracteres; {@code UUID.fromString} admite también atajos raros. */
    private static Optional<UUID> parseUuid(String text) {
        try {
            UUID parsed = UUID.fromString(text);
            return parsed.toString().equalsIgnoreCase(text) ? Optional.of(parsed) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
