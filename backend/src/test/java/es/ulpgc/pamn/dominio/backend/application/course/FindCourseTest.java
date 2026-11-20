package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.course.PublishCourseTest.CLOCK;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.COURSE_ID;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FindCourseTest {

    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private final FindCourse findCourse = new FindCourse(repository);

    static CourseVersionDraft draft(String text) {
        return CourseVersionDraft.of(course().build(), new CourseContent(text), CLOCK.instant());
    }

    @BeforeEach
    void twoVersionsArePublished() {
        repository.create(COURSE_ID, AUTHOR, draft("v1"));
        repository.appendVersion(COURSE_ID, AUTHOR.id(), draft("v2"));
    }

    @Test
    void theLatestVersionComesWithItsContent() {
        assertEquals("v2", findCourse.latest(COURSE_ID).orElseThrow().content().text());
        assertEquals(2, findCourse.latest(COURSE_ID).orElseThrow().summary().version());
    }

    @Test
    void anyPublishedVersionCanBeRead() {
        assertEquals("v1", findCourse.version(COURSE_ID, 1).orElseThrow().content().text());
    }

    @Test
    void anUnknownCourseOrVersionIsNotFound() {
        assertEquals(Optional.empty(), findCourse.latest(UUID.fromString("00000000-0000-0000-0000-0000000000ff")));
        assertEquals(Optional.empty(), findCourse.version(COURSE_ID, 3));
    }

    @Test
    void aVersionBelowOneDoesNotExist() {
        // §4.3: una versión que no es un entero positivo se responde como 404.
        assertEquals(Optional.empty(), findCourse.version(COURSE_ID, 0));
        assertEquals(Optional.empty(), findCourse.version(COURSE_ID, -1));
    }
}
