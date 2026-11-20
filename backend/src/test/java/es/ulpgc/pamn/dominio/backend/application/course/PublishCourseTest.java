package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.codes;
import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.reportOf;
import static es.ulpgc.pamn.dominio.backend.application.course.FakeCourseReader.utf8;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.COURSE_ID;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PublishCourseTest {

    /** Con nanosegundos, para comprobar que se truncan a microsegundos como en PostgreSQL. */
    static final Instant NOW = Instant.parse("2026-11-19T10:15:30.123456789Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private final FakeCourseReader reader = new FakeCourseReader()
            .parsing("grafos", course().build())
            .parsing("con avisos", course().tags("grafos", "Grafos").build())
            .parsing("con errores", course().module(1, m -> m.requires("bfs")).build())
            .answering("enorme", new CourseReadResult.TooLarge(reportOf(CourseIssueCode.PAYLOAD_TOO_LARGE)));
    private final PublishCourse publishCourse = new PublishCourse(
            new CourseInspector(reader, CourseValidator.standard()), repository, CLOCK, () -> COURSE_ID);

    @Test
    void aValidCourseIsPublishedAsVersion1() {
        Publication.Published published = assertInstanceOf(Publication.Published.class,
                publishCourse.publish(AUTHOR, utf8("grafos")));

        CourseSummary course = published.course();
        assertEquals(COURSE_ID, course.id());
        assertEquals(1, course.version());
        assertEquals(AUTHOR, course.author());
        assertEquals(Instant.parse("2026-11-19T10:15:30.123456Z"), course.publishedAt());
        assertEquals("Introducción a los grafos", course.metadata().title());
        assertEquals(List.of(), published.report().issues());
    }

    @Test
    void thePublishedVersionIsStoredWithItsExactContent() {
        publishCourse.publish(AUTHOR, utf8("grafos"));

        assertEquals("grafos", repository.findLatest(COURSE_ID).orElseThrow().content().text());
    }

    @Test
    void warningsComeBackWithThePublishedCourse() {
        Publication.Published published = assertInstanceOf(Publication.Published.class,
                publishCourse.publish(AUTHOR, utf8("con avisos")));

        assertEquals(List.of(CourseIssueCode.DUPLICATE_TAG), codes(published.report()));
    }

    @Test
    void aCourseWithErrorsIsRejectedAndNothingIsStored() {
        Publication.Rejected rejected = assertInstanceOf(Publication.Rejected.class,
                publishCourse.publish(AUTHOR, utf8("con errores")));

        assertEquals(List.of(CourseIssueCode.SELF_PREREQUISITE), codes(rejected.report()));
        assertEquals(0, repository.versionCount());
    }

    @Test
    void anUnreadableBodyIsReportedAndNothingIsStored() {
        assertEquals(new Publication.Unreadable(FakeCourseReader.UNREADABLE),
                publishCourse.publish(AUTHOR, utf8("{")));
        assertEquals(0, repository.versionCount());
    }

    @Test
    void aBodyOverTheLimitIsReportedAndNothingIsStored() {
        CourseReport report = assertInstanceOf(Publication.TooLarge.class,
                publishCourse.publish(AUTHOR, utf8("enorme"))).report();

        assertEquals(List.of(CourseIssueCode.PAYLOAD_TOO_LARGE), codes(report));
        assertEquals(0, repository.versionCount());
    }

    @Test
    void everyPublicationGetsANewId() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID second = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
        var ids = List.of(first, second).iterator();
        PublishCourse publishing = new PublishCourse(
                new CourseInspector(reader, CourseValidator.standard()), repository, CLOCK, ids::next);

        publishing.publish(AUTHOR, utf8("grafos"));
        publishing.publish(AUTHOR, utf8("grafos"));

        assertEquals(1, repository.findLatest(first).orElseThrow().summary().version());
        assertEquals(1, repository.findLatest(second).orElseThrow().summary().version());
    }
}
