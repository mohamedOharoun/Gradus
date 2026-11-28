package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.EnrollmentRows.EnrollmentRow;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.EnrollmentRows.ModuleRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentRowsTest {

    private static final UUID C1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID C2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant T3 = Instant.parse("2026-03-01T00:00:00Z");

    private static ModuleRow module(UUID courseId, String moduleId, Instant at) {
        return new ModuleRow(courseId, new MasteredModule(moduleId, at));
    }

    @Test
    void eachModuleGoesWithItsEnrollment() {
        List<Enrollment> result = EnrollmentRows.assemble(
                List.of(new EnrollmentRow(C1, T1), new EnrollmentRow(C2, T2)),
                List.of(module(C1, "a", T3), module(C2, "b", T3), module(C1, "c", T3)));

        assertEquals(List.of(
                new Enrollment(C1, T1, List.of(new MasteredModule("a", T3), new MasteredModule("c", T3))),
                new Enrollment(C2, T2, List.of(new MasteredModule("b", T3)))), result);
    }

    @Test
    void anEnrollmentWithoutModulesHasAnEmptyList() {
        List<Enrollment> result = EnrollmentRows.assemble(
                List.of(new EnrollmentRow(C1, T1), new EnrollmentRow(C2, T2)),
                List.of(module(C2, "b", T3)));

        assertTrue(result.get(0).mastered().isEmpty());
        assertEquals(1, result.get(1).mastered().size());
    }

    @Test
    void theOrderOfTheEnrollmentsIsKept() {
        List<Enrollment> result = EnrollmentRows.assemble(
                List.of(new EnrollmentRow(C2, T1), new EnrollmentRow(C1, T2)), List.of());

        assertEquals(List.of(C2, C1), result.stream().map(Enrollment::courseId).toList());
    }

    @Test
    void theOrderOfTheModulesOfEachEnrollmentIsKept() {
        List<Enrollment> result = EnrollmentRows.assemble(
                List.of(new EnrollmentRow(C1, T1)),
                List.of(module(C1, "z", T2), module(C1, "a", T2), module(C1, "m", T2)));

        assertEquals(List.of("z", "a", "m"),
                result.get(0).mastered().stream().map(MasteredModule::moduleId).toList());
    }

    @Test
    void withoutRowsTheResultIsAnEmptyList() {
        assertTrue(EnrollmentRows.assemble(List.of(), List.of()).isEmpty());
    }
}
