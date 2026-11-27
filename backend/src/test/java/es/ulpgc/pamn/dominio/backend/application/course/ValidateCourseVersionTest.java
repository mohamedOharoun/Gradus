package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.codes;
import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.reportOf;
import static es.ulpgc.pamn.dominio.backend.application.course.FakeCourseReader.utf8;
import static es.ulpgc.pamn.dominio.backend.application.course.PublishCourseTest.CLOCK;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.COURSE_ID;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.module;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateCourseVersionTest {

    private static final CourseAuthor SOMEONE_ELSE =
            new CourseAuthor(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Joel");
    private static final UUID UNKNOWN_COURSE = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private final FakeCourseReader reader = new FakeCourseReader()
            .parsing("v1", course().build())
            .parsing("v2", course().title("Grafos: representación y BFS").build())
            .parsing("v2 con un módulo más", course().modules(module("representacion").build(),
                    module("bfs").requires("representacion").build(),
                    module("dijkstra").requires("bfs").build()).build())
            .parsing("v2 sin bfs", course().modules(module("representacion").build()).build())
            .parsing("v2 con avisos y sin bfs", course().tags("grafos", "Grafos")
                    .modules(module("representacion").build()).build())
            .parsing("v2 con errores", course().module(1, m -> m.requires("bfs")).build())
            .answering("esquema invalido", new CourseReadResult.Invalid(reportOf(CourseIssueCode.INVALID_JSON)))
            .answering("enorme", new CourseReadResult.TooLarge(reportOf(CourseIssueCode.PAYLOAD_TOO_LARGE)));

    private CourseInspector inspector() {
        return new CourseInspector(reader, CourseValidator.standard());
    }

    private ValidateCourseVersion validator() {
        return new ValidateCourseVersion(inspector(), reader, repository);
    }

    private void givenPublishedVersion1() {
        new PublishCourse(inspector(), repository, CLOCK, () -> COURSE_ID)
                .publish(AUTHOR, utf8("v1"));
    }

    private VersionValidationResult validate(UUID requester, UUID courseId, String body) {
        return validator().validate(requester, courseId, utf8(body));
    }

    @Test
    void validNewVersionWithoutWarningsReturnsValidReportAndDoesNotStoreAnything() {
        givenPublishedVersion1();

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "v2"));

        assertTrue(checked.report().isValid());
        assertEquals(List.of(), checked.report().issues());
        assertEquals(1, repository.versionCount());
        assertEquals("v1", repository.findLatest(COURSE_ID).orElseThrow().content().text());
    }

    @Test
    void removingIdsAddsRemovedIdsWarningWithoutStoringAnything() {
        givenPublishedVersion1();

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "v2 sin bfs"));

        assertTrue(checked.report().isValid());
        assertEquals(List.of(CourseIssueCode.REMOVED_IDS), codes(checked.report()));
        assertTrue(checked.report().warnings().getFirst().message().contains("bfs, bfs-l1"));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void onlyAddingIdsGivesNoRemovedIdsWarning() {
        givenPublishedVersion1();

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "v2 con un módulo más"));

        assertTrue(checked.report().isValid());
        assertEquals(List.of(), checked.report().issues());
        assertEquals(1, repository.versionCount());
    }

    @Test
    void ownWarningsArePreservedAlongsideRemovedIds() {
        givenPublishedVersion1();

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "v2 con avisos y sin bfs"));

        assertTrue(checked.report().isValid());
        // CourseReport ordena los avisos por path: "" (REMOVED_IDS) va antes de "/tags/1" (DUPLICATE_TAG).
        assertEquals(List.of(CourseIssueCode.REMOVED_IDS, CourseIssueCode.DUPLICATE_TAG), codes(checked.report()));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void previousVersionThatCanNoLongerBeReadStillValidatesWithoutRemovedIds() {
        CourseVersionDraft unreadable = CourseVersionDraft.of(course().build(),
                new CourseContent("formato antiguo"), CLOCK.instant());
        repository.create(COURSE_ID, AUTHOR, unreadable);

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "v2 sin bfs"));

        assertTrue(checked.report().isValid());
        assertEquals(List.of(), checked.report().issues());
        assertEquals(1, repository.versionCount());
    }

    @Test
    void layerOneUnreadableBodyReturnsCheckedInvalidReport() {
        givenPublishedVersion1();

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "{"));

        assertFalse(checked.report().isValid());
        assertEquals(List.of(CourseIssueCode.INVALID_JSON), codes(checked.report()));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void schemaErrorsReturnCheckedInvalidReport() {
        givenPublishedVersion1();

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "esquema invalido"));

        assertFalse(checked.report().isValid());
        assertEquals(1, repository.versionCount());
    }

    @Test
    void semanticErrorsReturnCheckedInvalidReport() {
        givenPublishedVersion1();

        VersionValidationResult.Checked checked = assertInstanceOf(VersionValidationResult.Checked.class,
                validate(AUTHOR.id(), COURSE_ID, "v2 con errores"));

        assertFalse(checked.report().isValid());
        assertEquals(List.of(CourseIssueCode.SELF_PREREQUISITE), codes(checked.report()));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void bodyOverSizeLimitReturnsTooLarge() {
        givenPublishedVersion1();

        VersionValidationResult.TooLarge tooLarge = assertInstanceOf(VersionValidationResult.TooLarge.class,
                validate(AUTHOR.id(), COURSE_ID, "enorme"));

        assertEquals(List.of(CourseIssueCode.PAYLOAD_TOO_LARGE), codes(tooLarge.report()));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void unknownCourseReturnsCourseNotFoundBeforeInspectingBody() {
        assertEquals(new VersionValidationResult.CourseNotFound(UNKNOWN_COURSE),
                validate(AUTHOR.id(), UNKNOWN_COURSE, "{"));
    }

    @Test
    void someoneElseReturnsNotAuthorBeforeInspectingBody() {
        givenPublishedVersion1();

        assertEquals(new VersionValidationResult.NotAuthor(COURSE_ID),
                validate(SOMEONE_ELSE.id(), COURSE_ID, "{"));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void identicalContentToLatestReturnsIdenticalToLatestWithoutStoringAnything() {
        givenPublishedVersion1();

        assertEquals(new VersionValidationResult.IdenticalToLatest(COURSE_ID),
                validate(AUTHOR.id(), COURSE_ID, "v1"));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new ValidateCourseVersion(null, reader, repository));
        assertThrows(NullPointerException.class, () -> new ValidateCourseVersion(inspector(), null, repository));
        assertThrows(NullPointerException.class, () -> new ValidateCourseVersion(inspector(), reader, null));

        ValidateCourseVersion useCase = validator();
        assertThrows(NullPointerException.class, () -> useCase.validate(null, COURSE_ID, utf8("v2")));
        assertThrows(NullPointerException.class, () -> useCase.validate(AUTHOR.id(), null, utf8("v2")));
        assertThrows(NullPointerException.class, () -> useCase.validate(AUTHOR.id(), COURSE_ID, null));

        assertThrows(NullPointerException.class, () -> new VersionValidationResult.Checked(null));
        assertThrows(NullPointerException.class, () -> new VersionValidationResult.TooLarge(null));
        assertThrows(NullPointerException.class, () -> new VersionValidationResult.CourseNotFound(null));
        assertThrows(NullPointerException.class, () -> new VersionValidationResult.NotAuthor(null));
        assertThrows(NullPointerException.class, () -> new VersionValidationResult.IdenticalToLatest(null));
    }
}
