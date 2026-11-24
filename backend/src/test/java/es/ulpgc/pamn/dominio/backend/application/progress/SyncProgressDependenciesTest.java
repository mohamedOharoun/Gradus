package es.ulpgc.pamn.dominio.backend.application.progress;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.NOW;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncProgressDependenciesTest {

    private final EnrollmentRepository repository = new InMemoryEnrollmentRepository();
    private final CourseExistence courses = new FakeCourseExistence();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void rejectsANullRepository() {
        assertThrows(NullPointerException.class, () -> new SyncProgress(null, courses, clock));
    }

    @Test
    void rejectsANullCourseExistence() {
        assertThrows(NullPointerException.class, () -> new SyncProgress(repository, null, clock));
    }

    @Test
    void rejectsANullClock() {
        assertThrows(NullPointerException.class, () -> new SyncProgress(repository, courses, null));
    }
}
