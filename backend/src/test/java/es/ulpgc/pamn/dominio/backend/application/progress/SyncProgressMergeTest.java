package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.application.course.AdjustableClock;
import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.EnrollmentDraft;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link SyncProgress}: qué se guarda, qué se devuelve y cuándo se escribe. */
class SyncProgressMergeTest {

    private final InMemoryEnrollmentRepository repository = new InMemoryEnrollmentRepository();
    private final FakeCourseExistence courses = new FakeCourseExistence(GRAPHS, SORTING);
    private final AdjustableClock clock = new AdjustableClock(NOW);
    private final SyncProgress syncProgress = new SyncProgress(repository, courses, clock);

    @Test
    void theResponseHoldsEverythingStoredNotOnlyWhatWasSent() {
        syncProgress.sync(ANA, List.of(draft(GRAPHS, EARLY, module("bfs", LATE))));

        SyncResult result = syncProgress.sync(ANA, List.of(draft(SORTING, LATE)));

        assertEquals(List.of(
                enrollment(GRAPHS, EARLY, mastered("bfs", LATE)),
                enrollment(SORTING, LATE)), result.enrollments());
    }

    @Test
    void sendingTheSameTwiceLeavesTheSameState() {
        List<EnrollmentDraft> request = List.of(draft(GRAPHS, EARLY, module("bfs", LATE)));

        SyncResult first = syncProgress.sync(ANA, request);
        SyncResult second = syncProgress.sync(ANA, request);

        assertEquals(first, second);
    }

    @Test
    void anOlderDateAndNewModulesFromALaterRequestAreMergedByTheRepository() {
        syncProgress.sync(ANA, List.of(draft(GRAPHS, LATE, module("bfs", LATE))));

        SyncResult result = syncProgress.sync(ANA, List.of(
                draft(GRAPHS, EARLY, module("bfs", EARLY), module("dfs", LATE))));

        assertEquals(List.of(enrollment(GRAPHS, EARLY, mastered("bfs", EARLY), mastered("dfs", LATE))),
                result.enrollments());
    }

    @Test
    void futureDatesAreClippedToTheInjectedClock() {
        Instant tomorrow = NOW.plus(Duration.ofDays(1));

        SyncResult result = syncProgress.sync(ANA, List.of(
                draft(GRAPHS, tomorrow, module("bfs", tomorrow))));

        assertEquals(List.of(enrollment(GRAPHS, NOW, mastered("bfs", NOW))), result.enrollments());
    }

    @Test
    void pastDatesAreNotChanged() {
        SyncResult result = syncProgress.sync(ANA, List.of(draft(GRAPHS, EARLY, module("bfs", LATE))));

        assertEquals(List.of(enrollment(GRAPHS, EARLY, mastered("bfs", LATE))), result.enrollments());
    }

    @Test
    void theClockIsReadOnEveryCallNotOnlyWhenTheUseCaseIsCreated() {
        Instant tomorrow = NOW.plus(Duration.ofDays(1));
        syncProgress.sync(ANA, List.of(draft(GRAPHS, tomorrow)));
        clock.advance(Duration.ofHours(1));

        SyncResult result = syncProgress.sync(ANA, List.of(draft(SORTING, tomorrow)));

        assertEquals(List.of(
                enrollment(GRAPHS, NOW),
                enrollment(SORTING, NOW.plus(Duration.ofHours(1)))), result.enrollments());
    }

    @Test
    void duplicatesInTheSameRequestAreMergedBeforeSaving() {
        syncProgress.sync(ANA, List.of(
                draft(GRAPHS, LATE, module("bfs", LATE)),
                draft(GRAPHS, EARLY, module("bfs", EARLY), module("dfs", LATE))));

        assertEquals(1, repository.mergeCalls().size());
        assertEquals(List.of(enrollment(GRAPHS, EARLY, mastered("bfs", EARLY), mastered("dfs", LATE))),
                repository.mergeCalls().getFirst().enrollments());
    }

    @Test
    void twoUsersDoNotSeeEachOther() {
        syncProgress.sync(ANA, List.of(draft(GRAPHS, EARLY)));
        syncProgress.sync(BEN, List.of(draft(SORTING, LATE)));

        assertEquals(List.of(enrollment(GRAPHS, EARLY)), syncProgress.sync(ANA, List.of()).enrollments());
        assertEquals(List.of(enrollment(SORTING, LATE)), syncProgress.sync(BEN, List.of()).enrollments());
    }

    @Test
    void theRepositoryReceivesTheUserIdOfTheCall() {
        syncProgress.sync(BEN, List.of(draft(GRAPHS, EARLY)));

        assertEquals(List.of(BEN), repository.mergeCalls().stream()
                .map(InMemoryEnrollmentRepository.MergeCall::userId).toList());
    }

    @Test
    void anEmptyRequestOnlyReads() {
        syncProgress.sync(ANA, List.of(draft(GRAPHS, EARLY)));
        int mergesBefore = repository.mergeCalls().size();
        int existenceBefore = courses.calls().size();

        SyncResult result = syncProgress.sync(ANA, List.of());

        assertEquals(List.of(enrollment(GRAPHS, EARLY)), result.enrollments());
        assertEquals(List.of(), result.skipped());
        assertEquals(mergesBefore, repository.mergeCalls().size());
        assertEquals(existenceBefore, courses.calls().size());
    }

    @Test
    void nothingIsWrittenWhenNothingValidIsLeft() {
        syncProgress.sync(ANA, List.of(draft("not-a-uuid", EARLY)));

        assertTrue(repository.mergeCalls().isEmpty());
    }

    @Test
    void nothingIsWrittenWhenEveryCourseIsMissing() {
        syncProgress.sync(ANA, List.of(draft(MISSING, EARLY)));

        assertTrue(repository.mergeCalls().isEmpty());
    }

    @Test
    void theResponseIsAlwaysReadAfterWriting() {
        syncProgress.sync(ANA, List.of());
        syncProgress.sync(ANA, List.of(draft(MISSING, EARLY)));

        assertEquals(2, repository.findAllCalls());
    }

    @Test
    void aBlankModuleOnlyDropsThatModuleAndTheRestIsSaved() {
        SyncResult result = syncProgress.sync(ANA, List.of(
                draft(GRAPHS, EARLY, module(" ", LATE), module("bfs", LATE))));

        assertEquals(List.of(enrollment(GRAPHS, EARLY, mastered("bfs", LATE))), result.enrollments());
    }

    @Test
    void aRepositoryFailurePropagatesInsteadOfBeingSwallowed() {
        IllegalStateException failure = new IllegalStateException("database down");
        EnrollmentRepository broken = new EnrollmentRepository() {
            @Override
            public void mergeAll(UUID userId, List<Enrollment> enrollments) {
                throw failure;
            }

            @Override
            public List<Enrollment> findAll(UUID userId) {
                return List.of();
            }
        };

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new SyncProgress(broken, courses, clock).sync(ANA, List.of(draft(GRAPHS, EARLY))));

        assertSame(failure, thrown);
    }
}
