package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentWriteOrderTest {

    private static final UUID C1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID C2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID C3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-02-01T00:00:00Z");

    private static Enrollment enrollment(UUID courseId, String... moduleIds) {
        return new Enrollment(courseId, T1, java.util.Arrays.stream(moduleIds)
                .map(id -> new MasteredModule(id, T2)).toList());
    }

    private static List<UUID> courseIds(List<Enrollment> enrollments) {
        return enrollments.stream().map(Enrollment::courseId).toList();
    }

    private static List<String> moduleIds(Enrollment enrollment) {
        return enrollment.mastered().stream().map(MasteredModule::moduleId).toList();
    }

    @Test
    void unorderedCoursesComeOutByCourseId() {
        List<Enrollment> sorted = EnrollmentWriteOrder.sorted(List.of(enrollment(C3), enrollment(C1), enrollment(C2)));

        assertEquals(List.of(C1, C2, C3), courseIds(sorted));
    }

    @Test
    void theModulesOfEachEnrollmentComeOutByModuleId() {
        List<Enrollment> sorted = EnrollmentWriteOrder.sorted(
                List.of(enrollment(C2, "m3", "m1", "m2"), enrollment(C1, "b", "a")));

        assertEquals(List.of("a", "b"), moduleIds(sorted.get(0)));
        assertEquals(List.of("m1", "m2", "m3"), moduleIds(sorted.get(1)));
    }

    @Test
    void theResultDoesNotDependOnTheOrderOfTheInput() {
        List<Enrollment> input = List.of(enrollment(C1, "a", "b"), enrollment(C2, "c", "d"), enrollment(C3, "e"));
        List<Enrollment> expected = EnrollmentWriteOrder.sorted(input);

        List<Enrollment> shuffled = new ArrayList<>(input);
        for (int seed = 0; seed < 6; seed++) {
            Collections.shuffle(shuffled, new java.util.Random(seed));
            assertEquals(expected, EnrollmentWriteOrder.sorted(shuffled));
        }
    }

    @Test
    void theFunctionDoesNotChangeTheListItReceives() {
        List<Enrollment> input = new ArrayList<>(List.of(enrollment(C2, "b", "a"), enrollment(C1)));
        List<Enrollment> copy = List.copyOf(input);

        EnrollmentWriteOrder.sorted(input);

        assertEquals(copy, input);
    }

    @Test
    void anEmptyListGivesAnEmptyList() {
        assertTrue(EnrollmentWriteOrder.sorted(List.of()).isEmpty());
    }

    @Test
    void itUsesTheNaturalOrderOfUuidNotTheOrderOfItsText() {
        // UUID.compareTo compara long con signo: 8000… (negativo) va antes que 7fff…, al revés que en texto.
        UUID high = UUID.fromString("80000000-0000-0000-0000-000000000000");
        UUID low = UUID.fromString("7fffffff-0000-0000-0000-000000000000");

        List<Enrollment> sorted = EnrollmentWriteOrder.sorted(List.of(enrollment(low), enrollment(high)));

        assertEquals(Stream.of(low, high).sorted().toList(), courseIds(sorted));
        assertEquals(List.of(high, low), courseIds(sorted));
    }

    @Test
    void modulesAreOrderedAsTextSoUppercaseComesFirst() {
        List<Enrollment> sorted = EnrollmentWriteOrder.sorted(List.of(enrollment(C1, "b", "B")));

        assertEquals(List.of("B", "b"), moduleIds(sorted.get(0)));
    }

    @Test
    void onlyTheOrderChangesDatesAndEmptyModuleListsAreKept() {
        List<Enrollment> sorted = EnrollmentWriteOrder.sorted(List.of(enrollment(C2), enrollment(C1, "a")));

        assertEquals(T1, sorted.get(0).enrolledAt());
        assertEquals(T2, sorted.get(0).mastered().get(0).masteredAt());
        assertTrue(sorted.get(1).mastered().isEmpty());
    }
}
