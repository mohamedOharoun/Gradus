package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.ANA;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.BEN;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.EARLY;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.GRAPHS;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.LATE;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.MISSING;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.NOW;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.SORTING;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.draft;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.enrollment;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.mastered;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.module;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnenrollCourseTest {

    private final InMemoryEnrollmentRepository repository = new InMemoryEnrollmentRepository();
    private final UnenrollCourse unenrollCourse = new UnenrollCourse(repository);

    @Test
    void rejectsANullRepository() {
        assertThrows(NullPointerException.class, () -> new UnenrollCourse(null));
    }

    @Test
    void rejectsANullUserIdWithoutCallingTheRepository() {
        assertThrows(NullPointerException.class, () -> unenrollCourse.unenroll(null, GRAPHS));

        assertTrue(repository.deleteCalls().isEmpty());
    }

    @Test
    void rejectsANullCourseIdWithoutCallingTheRepository() {
        assertThrows(NullPointerException.class, () -> unenrollCourse.unenroll(ANA, null));

        assertTrue(repository.deleteCalls().isEmpty());
    }

    @Test
    void unenrollingRemovesTheEnrollmentAndAllItsMasteredModules() {
        repository.mergeAll(ANA, List.of(
                enrollment(GRAPHS, EARLY, mastered("bfs", EARLY), mastered("dfs", LATE))));

        unenrollCourse.unenroll(ANA, GRAPHS);

        assertTrue(repository.findAll(ANA).isEmpty());
        assertEquals(List.of(new InMemoryEnrollmentRepository.DeleteCall(ANA, GRAPHS)), repository.deleteCalls());
    }

    @Test
    void unenrollingFromANonEnrolledCourseIsIdempotentAndLeavesOtherProgressIntact() {
        Enrollment graphs = enrollment(GRAPHS, EARLY, mastered("bfs", LATE));
        repository.mergeAll(ANA, List.of(graphs));

        assertDoesNotThrow(() -> unenrollCourse.unenroll(ANA, MISSING));

        assertEquals(List.of(graphs), repository.findAll(ANA));
        assertEquals(List.of(new InMemoryEnrollmentRepository.DeleteCall(ANA, MISSING)), repository.deleteCalls());
    }

    @Test
    void repeatingUnenrollTwiceIsIdempotentAndLeavesOtherProgressIntact() {
        Enrollment sorting = enrollment(SORTING, EARLY, mastered("mergesort", LATE));
        repository.mergeAll(ANA, List.of(
                enrollment(GRAPHS, EARLY, mastered("bfs", LATE)),
                sorting));

        assertDoesNotThrow(() -> {
            unenrollCourse.unenroll(ANA, GRAPHS);
            unenrollCourse.unenroll(ANA, GRAPHS);
        });

        assertEquals(List.of(sorting), repository.findAll(ANA));
    }

    @Test
    void unenrollingOneCourseDoesNotAffectOtherCoursesOfTheSameUser() {
        Enrollment sorting = enrollment(SORTING, EARLY, mastered("quicksort", LATE));
        repository.mergeAll(ANA, List.of(
                enrollment(GRAPHS, EARLY, mastered("bfs", LATE)),
                sorting));

        unenrollCourse.unenroll(ANA, GRAPHS);

        assertEquals(List.of(sorting), repository.findAll(ANA));
    }

    @Test
    void unenrollingOneUserDoesNotAffectAnotherUserInTheSameCourse() {
        Enrollment bensGraphs = enrollment(GRAPHS, LATE, mastered("dfs", LATE));
        repository.mergeAll(ANA, List.of(enrollment(GRAPHS, EARLY, mastered("bfs", EARLY))));
        repository.mergeAll(BEN, List.of(bensGraphs));

        unenrollCourse.unenroll(ANA, GRAPHS);

        assertTrue(repository.findAll(ANA).isEmpty());
        assertEquals(List.of(bensGraphs), repository.findAll(BEN));
    }

    @Test
    void syncingAgainAfterUnenrollingStartsCleanWithoutPreviousModulesOrTimestamps() {
        FakeCourseExistence courses = new FakeCourseExistence(GRAPHS);
        SyncProgress syncProgress = new SyncProgress(repository, courses, Clock.fixed(NOW, ZoneOffset.UTC));
        syncProgress.sync(ANA, List.of(
                draft(GRAPHS, EARLY, module("bfs", EARLY), module("dfs", EARLY))));

        unenrollCourse.unenroll(ANA, GRAPHS);
        SyncResult afterReEnroll = syncProgress.sync(ANA, List.of(
                draft(GRAPHS, LATE, module("dijkstra", LATE))));

        assertEquals(List.of(enrollment(GRAPHS, LATE, mastered("dijkstra", LATE))), afterReEnroll.enrollments());
        assertTrue(afterReEnroll.skipped().isEmpty());
    }

    @Test
    void aRepositoryFailurePropagatesInsteadOfBeingSwallowed() {
        IllegalStateException failure = new IllegalStateException("database down");
        EnrollmentRepository broken = new EnrollmentRepository() {
            @Override
            public void mergeAll(UUID userId, List<Enrollment> enrollments) {
            }

            @Override
            public List<Enrollment> findAll(UUID userId) {
                return List.of();
            }

            @Override
            public void delete(UUID userId, UUID courseId) {
                throw failure;
            }
        };

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new UnenrollCourse(broken).unenroll(ANA, GRAPHS));

        assertSame(failure, thrown);
    }
}
