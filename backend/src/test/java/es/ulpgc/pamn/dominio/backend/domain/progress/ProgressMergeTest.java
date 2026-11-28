package es.ulpgc.pamn.dominio.backend.domain.progress;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressMergeTest {

    private static final UUID COURSE_A = UUID.fromString("0b8f6d2e-0000-4000-8000-00000000000a");
    private static final UUID COURSE_B = UUID.fromString("0b8f6d2e-0000-4000-8000-00000000000b");
    private static final Instant T1 = Instant.parse("2026-11-24T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-24T11:00:00Z");

    private static Enrollment enrollment(UUID course, Instant at, MasteredModule... modules) {
        return new Enrollment(course, at, List.of(modules));
    }

    private static MasteredModule module(String id, Instant at) {
        return new MasteredModule(id, at);
    }

    @Test
    void unionOfDifferentCourses() {
        List<Enrollment> result = ProgressMerge.merge(
                List.of(enrollment(COURSE_A, T1)), List.of(enrollment(COURSE_B, T1)));

        assertEquals(List.of(enrollment(COURSE_A, T1), enrollment(COURSE_B, T1)), result);
    }

    @Test
    void unionOfModulesOfTheSameCourse() {
        List<Enrollment> result = ProgressMerge.merge(
                List.of(enrollment(COURSE_A, T1, module("bfs", T1))),
                List.of(enrollment(COURSE_A, T1, module("dfs", T1))));

        assertEquals(List.of(enrollment(COURSE_A, T1, module("bfs", T1), module("dfs", T1))), result);
    }

    @Test
    void theOldestEnrolledAtWinsInBothDirections() {
        List<Enrollment> older = List.of(enrollment(COURSE_A, T1));
        List<Enrollment> newer = List.of(enrollment(COURSE_A, T2));

        assertEquals(T1, ProgressMerge.merge(older, newer).get(0).enrolledAt());
        assertEquals(T1, ProgressMerge.merge(newer, older).get(0).enrolledAt());
    }

    @Test
    void theOldestMasteredAtWinsInBothDirections() {
        List<Enrollment> older = List.of(enrollment(COURSE_A, T1, module("bfs", T1)));
        List<Enrollment> newer = List.of(enrollment(COURSE_A, T1, module("bfs", T2)));

        assertEquals(T1, ProgressMerge.merge(older, newer).get(0).mastered().get(0).masteredAt());
        assertEquals(T1, ProgressMerge.merge(newer, older).get(0).mastered().get(0).masteredAt());
    }

    @Test
    void duplicatesInTheSameListAreMerged() {
        List<Enrollment> result = ProgressMerge.merge(List.of(), List.of(
                enrollment(COURSE_A, T2, module("bfs", T2), module("bfs", T1)),
                enrollment(COURSE_A, T1, module("dfs", T1))));

        assertEquals(List.of(enrollment(COURSE_A, T1, module("bfs", T1), module("dfs", T1))), result);
    }

    @Test
    void isIdempotent() {
        List<Enrollment> stored = List.of(enrollment(COURSE_A, T2, module("bfs", T2)));
        List<Enrollment> incoming = List.of(
                enrollment(COURSE_A, T1, module("bfs", T1), module("dfs", T2)), enrollment(COURSE_B, T1));

        List<Enrollment> once = ProgressMerge.merge(stored, incoming);

        assertEquals(once, ProgressMerge.merge(once, incoming));
    }

    @Test
    void neverDeletesAnythingStored() {
        List<Enrollment> stored = List.of(
                enrollment(COURSE_A, T1, module("bfs", T1), module("dfs", T1)), enrollment(COURSE_B, T1));

        List<Enrollment> result = ProgressMerge.merge(stored, List.of(enrollment(COURSE_A, T2)));

        assertEquals(stored, result);
    }

    @Test
    void anEmptyIncomingReturnsWhatWasStored() {
        List<Enrollment> stored = List.of(enrollment(COURSE_A, T1, module("bfs", T1)));

        assertEquals(stored, ProgressMerge.merge(stored, List.of()));
    }

    @Test
    void nothingStoredAndNothingIncomingGivesNothing() {
        assertEquals(List.of(), ProgressMerge.merge(List.of(), List.of()));
    }

    @Test
    void theOrderIsFirstAppearanceStoredFirst() {
        List<Enrollment> result = ProgressMerge.merge(
                List.of(enrollment(COURSE_B, T1, module("z", T1))),
                List.of(enrollment(COURSE_A, T1), enrollment(COURSE_B, T1, module("a", T1))));

        assertEquals(List.of(COURSE_B, COURSE_A), result.stream().map(Enrollment::courseId).toList());
        assertEquals(List.of("z", "a"),
                result.get(0).mastered().stream().map(MasteredModule::moduleId).toList());
    }

    @Test
    void doesNotModifyItsInputs() {
        List<Enrollment> stored = new ArrayList<>(List.of(enrollment(COURSE_A, T2, module("bfs", T2))));
        List<Enrollment> incoming = new ArrayList<>(List.of(enrollment(COURSE_A, T1, module("bfs", T1))));
        List<Enrollment> storedCopy = List.copyOf(stored);
        List<Enrollment> incomingCopy = List.copyOf(incoming);

        ProgressMerge.merge(stored, incoming);

        assertEquals(storedCopy, stored);
        assertEquals(incomingCopy, incoming);
    }

    @Test
    void theResultCannotBeModifiedFromOutside() {
        List<Enrollment> result = ProgressMerge.merge(List.of(), List.of(enrollment(COURSE_A, T1)));

        assertThrows(UnsupportedOperationException.class, () -> result.add(enrollment(COURSE_B, T1)));
    }
}
