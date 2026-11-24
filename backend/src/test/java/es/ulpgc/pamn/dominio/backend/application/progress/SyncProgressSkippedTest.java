package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.application.course.AdjustableClock;
import es.ulpgc.pamn.dominio.backend.domain.progress.SkippedEntry;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.ANA;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.EARLY;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.GRAPHS;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.LATE;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.MISSING;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.MISSING_TOO;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.NOW;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.SORTING;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.draft;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.enrollment;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.mastered;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.module;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link SyncProgress}: qué va a {@code skipped}, en qué orden y cómo se consulta la existencia. */
class SyncProgressSkippedTest {

    private final InMemoryEnrollmentRepository repository = new InMemoryEnrollmentRepository();
    private final FakeCourseExistence courses = new FakeCourseExistence(GRAPHS, SORTING);
    private final SyncProgress syncProgress =
            new SyncProgress(repository, courses, new AdjustableClock(NOW));

    @Test
    void aMissingCourseIsSkippedAndTheRestIsSaved() {
        SyncResult result = syncProgress.sync(ANA, List.of(
                draft(MISSING, EARLY, module("bfs", LATE)),
                draft(GRAPHS, EARLY)));

        assertEquals(List.of(SkippedEntry.enrollment(MISSING.toString(), ErrorCode.COURSE_NOT_FOUND)),
                result.skipped());
        assertEquals(List.of(enrollment(GRAPHS, EARLY)), result.enrollments());
    }

    @Test
    void whatWasSkippedWhileValidatingAndMissingCoursesAreJoined() {
        SyncResult result = syncProgress.sync(ANA, List.of(
                draft("not-a-uuid", EARLY),
                draft(GRAPHS, EARLY, module(" ", LATE)),
                draft(MISSING, EARLY)));

        assertEquals(List.of(
                SkippedEntry.enrollment("not-a-uuid", ErrorCode.COURSE_NOT_FOUND),
                SkippedEntry.module(GRAPHS.toString(), " ", ErrorCode.REQUIRED),
                SkippedEntry.enrollment(MISSING.toString(), ErrorCode.COURSE_NOT_FOUND)), result.skipped());
    }

    @Test
    void skippedListsFirstWhatValidationOmittedThenMissingCoursesEachInRequestOrder() {
        SyncResult result = syncProgress.sync(ANA, List.of(
                draft(MISSING_TOO, EARLY),
                draft("bad-2", EARLY),
                draft(MISSING, EARLY),
                draft("bad-1", EARLY)));

        assertEquals(List.of(
                SkippedEntry.enrollment("bad-2", ErrorCode.COURSE_NOT_FOUND),
                SkippedEntry.enrollment("bad-1", ErrorCode.COURSE_NOT_FOUND),
                SkippedEntry.enrollment(MISSING_TOO.toString(), ErrorCode.COURSE_NOT_FOUND),
                SkippedEntry.enrollment(MISSING.toString(), ErrorCode.COURSE_NOT_FOUND)), result.skipped());
    }

    @Test
    void aMissingCourseSentTwiceIsSkippedOnlyOnce() {
        SyncResult result = syncProgress.sync(ANA, List.of(
                draft(MISSING, EARLY), draft(MISSING, LATE)));

        assertEquals(List.of(SkippedEntry.enrollment(MISSING.toString(), ErrorCode.COURSE_NOT_FOUND)),
                result.skipped());
    }

    @Test
    void anUppercaseCourseIdIsSavedAndSkippedUnderTheCanonicalLowercaseUuid() {
        SyncResult result = syncProgress.sync(ANA, List.of(
                draft(GRAPHS.toString().toUpperCase(), EARLY, module("bfs", LATE)),
                draft(MISSING.toString().toUpperCase(), EARLY)));

        assertEquals(List.of(enrollment(GRAPHS, EARLY, mastered("bfs", LATE))), result.enrollments());
        assertEquals(List.of(SkippedEntry.enrollment(MISSING.toString(), ErrorCode.COURSE_NOT_FOUND)),
                result.skipped());
    }

    @Test
    void existenceIsAskedOnceWithTheDistinctIds() {
        syncProgress.sync(ANA, List.of(
                draft(GRAPHS, EARLY),
                draft("not-a-uuid", EARLY),
                draft(GRAPHS, LATE),
                draft(MISSING, EARLY),
                draft(SORTING, EARLY)));

        assertEquals(List.of(Set.of(GRAPHS, MISSING, SORTING)), courses.calls());
    }

    @Test
    void existenceIsNotAskedWhenNothingValidIsLeft() {
        syncProgress.sync(ANA, List.of(draft("not-a-uuid", EARLY)));
        syncProgress.sync(ANA, List.of());

        assertTrue(courses.calls().isEmpty());
    }

    @Test
    void nothingIsSkippedWhenEverythingIsSaved() {
        SyncResult result = syncProgress.sync(ANA, List.of(draft(GRAPHS, EARLY)));

        assertEquals(List.of(), result.skipped());
    }
}
