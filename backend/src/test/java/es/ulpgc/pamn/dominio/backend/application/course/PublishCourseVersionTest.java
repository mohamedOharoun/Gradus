package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.course.PageRequest;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishCourseVersionTest {

    static final CourseAuthor SOMEONE_ELSE =
            new CourseAuthor(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Joel");
    static final UUID UNKNOWN_COURSE = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private final FakeCourseReader reader = new FakeCourseReader()
            .parsing("v1", course().build())
            .parsing("v2", course().title("Grafos: representación y BFS").build())
            .parsing("v2 con un módulo más", course().modules(module("representacion").build(),
                    module("bfs").requires("representacion").build(),
                    module("dijkstra").requires("bfs").build()).build())
            .parsing("v2 sin bfs", course().modules(module("representacion").build()).build())
            .parsing("v2 con avisos", course().tags("grafos", "Grafos").build())
            .parsing("v2 con errores", course().module(1, m -> m.requires("bfs")).build())
            .answering("enorme", new CourseReadResult.TooLarge(reportOf(CourseIssueCode.PAYLOAD_TOO_LARGE)));

    PublishCourseVersion publishingWith(CourseRepository repository) {
        return new PublishCourseVersion(new CourseInspector(reader, CourseValidator.standard()), reader, repository,
                CLOCK);
    }

    /** El curso ya publicado (versión 1, texto «v1») por {@code AUTHOR}. */
    void givenPublishedVersion1() {
        new PublishCourse(new CourseInspector(reader, CourseValidator.standard()), repository, CLOCK, () -> COURSE_ID)
                .publish(AUTHOR, utf8("v1"));
    }

    PublishVersionResult publish(UUID requester, UUID courseId, String body) {
        return publishingWith(repository).publish(requester, courseId, utf8(body));
    }

    @Test
    void theAuthorPublishesVersion2() {
        givenPublishedVersion1();

        Publication.Published published = assertInstanceOf(Publication.Published.class,
                publish(AUTHOR.id(), COURSE_ID, "v2"));

        assertEquals(2, published.course().version());
        assertEquals("Grafos: representación y BFS", published.course().metadata().title());
        assertEquals("v2", repository.findLatest(COURSE_ID).orElseThrow().content().text());
        assertEquals("v1", repository.findVersion(COURSE_ID, 1).orElseThrow().content().text());
    }

    @Test
    void anUnknownCourseIsNotFoundBeforeLookingAtTheBody() {
        // Opción A: aunque el cuerpo ni siquiera sea JSON, primero se dice que el curso no existe.
        assertEquals(new Publication.CourseNotFound(UNKNOWN_COURSE), publish(AUTHOR.id(), UNKNOWN_COURSE, "{"));
    }

    @Test
    void someoneElseCannotPublishAVersionEvenWithABrokenBody() {
        givenPublishedVersion1();

        assertEquals(new Publication.NotAuthor(COURSE_ID), publish(SOMEONE_ELSE.id(), COURSE_ID, "{"));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void theSameContentAsTheLatestVersionCreatesNoVersion() {
        givenPublishedVersion1();

        assertEquals(new Publication.IdenticalToLatest(COURSE_ID), publish(AUTHOR.id(), COURSE_ID, "v1"));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void anInvalidBodyFromTheAuthorIsReportedAndNothingIsStored() {
        givenPublishedVersion1();

        assertInstanceOf(Publication.Unreadable.class, publish(AUTHOR.id(), COURSE_ID, "{"));
        assertInstanceOf(Publication.Rejected.class, publish(AUTHOR.id(), COURSE_ID, "v2 con errores"));
        assertInstanceOf(Publication.TooLarge.class, publish(AUTHOR.id(), COURSE_ID, "enorme"));
        assertEquals(1, repository.versionCount());
    }

    @Test
    void removingIdsAddsAWarningWithThem() {
        givenPublishedVersion1();

        Publication.Published published = assertInstanceOf(Publication.Published.class,
                publish(AUTHOR.id(), COURSE_ID, "v2 sin bfs"));

        assertEquals(List.of(CourseIssueCode.REMOVED_IDS), codes(published.report()));
        assertTrue(published.report().warnings().getFirst().message().contains("bfs, bfs-l1"));
    }

    @Test
    void onlyAddingIdsGivesNoWarning() {
        givenPublishedVersion1();

        Publication.Published published = assertInstanceOf(Publication.Published.class,
                publish(AUTHOR.id(), COURSE_ID, "v2 con un módulo más"));

        assertEquals(List.of(), published.report().issues());
    }

    @Test
    void theWarningsOfTheNewVersionAreKept() {
        givenPublishedVersion1();

        Publication.Published published = assertInstanceOf(Publication.Published.class,
                publish(AUTHOR.id(), COURSE_ID, "v2 con avisos"));

        assertEquals(List.of(CourseIssueCode.DUPLICATE_TAG), codes(published.report()));
    }

    @Test
    void aPreviousVersionThatCanNoLongerBeReadIsNotCompared() {
        // Si el formato cambiara y el lector ya no entendiera la versión guardada, se publica sin REMOVED_IDS.
        CourseVersionDraft unreadable = CourseVersionDraft.of(course().build(),
                new CourseContent("formato antiguo"), CLOCK.instant());
        repository.create(COURSE_ID, AUTHOR, unreadable);

        Publication.Published published = assertInstanceOf(Publication.Published.class,
                publish(AUTHOR.id(), COURSE_ID, "v2 sin bfs"));

        assertEquals(List.of(), published.report().issues());
    }

    @Test
    void theRepositoryHasTheLastWord() {
        // Entre la comprobación y la transacción puede colarse otra publicación: manda lo que diga appendVersion.
        givenPublishedVersion1();
        for (AppendVersionResult raced : List.of(new AppendVersionResult.CourseNotFound(),
                new AppendVersionResult.NotAuthor(), new AppendVersionResult.IdenticalToLatest())) {
            PublishVersionResult result = publishingWith(racingRepository(raced)).publish(AUTHOR.id(), COURSE_ID,
                    utf8("v2"));

            assertEquals(switch (raced) {
                case AppendVersionResult.CourseNotFound notFound -> new Publication.CourseNotFound(COURSE_ID);
                case AppendVersionResult.NotAuthor notAuthor -> new Publication.NotAuthor(COURSE_ID);
                case AppendVersionResult.IdenticalToLatest identical -> new Publication.IdenticalToLatest(COURSE_ID);
                case AppendVersionResult.Appended appended -> throw new AssertionError();
            }, result);
        }
    }

    /** Un repositorio que lee como el de memoria, pero cuyo {@code appendVersion} responde {@code raced}. */
    CourseRepository racingRepository(AppendVersionResult raced) {
        return new CourseRepository() {
            @Override
            public CourseSummary create(UUID courseId, CourseAuthor author, CourseVersionDraft draft) {
                return repository.create(courseId, author, draft);
            }

            @Override
            public AppendVersionResult appendVersion(UUID courseId, UUID requesterId, CourseVersionDraft draft) {
                return raced;
            }

            @Override
            public Optional<CourseVersion> findLatest(UUID courseId) {
                return repository.findLatest(courseId);
            }

            @Override
            public Optional<CourseVersion> findVersion(UUID courseId, int version) {
                return repository.findVersion(courseId, version);
            }

            @Override
            public Page<CourseSummary> search(CourseSearch search, PageRequest page) {
                return repository.search(search, page);
            }

            @Override
            public List<CourseSummary> findByAuthor(UUID authorId) {
                return repository.findByAuthor(authorId);
            }
        };
    }
}
