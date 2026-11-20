package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SearchCoursesTest {

    static final UUID GRAPHS = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID TREES = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    static final UUID ENGLISH = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    static final Instant MONDAY = Instant.parse("2026-11-16T09:00:00Z");
    static final Instant TUESDAY = Instant.parse("2026-11-17T09:00:00Z");
    static final Instant WEDNESDAY = Instant.parse("2026-11-18T09:00:00Z");

    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private final SearchCourses searchCourses = new SearchCourses(repository);

    void publish(UUID id, CourseDocument course, Instant at) {
        repository.create(id, AUTHOR, CourseVersionDraft.of(course, new CourseContent(id + " v1"), at));
    }

    void givenThreeCourses() {
        publish(GRAPHS, course().build(), MONDAY);
        publish(TREES, course().title("Árboles binarios").description("Recorridos en profundidad.")
                .tags("árboles", "algoritmos").build(), WEDNESDAY);
        publish(ENGLISH, course().title("Graphs").description("Breadth-first search.").language("en")
                .tags("graphs").build(), TUESDAY);
    }

    static SearchCoursesQuery query(String text, List<String> tags, String language) {
        return new SearchCoursesQuery(text, tags, language, null, null);
    }

    static SearchCoursesQuery page(String page, String size) {
        return new SearchCoursesQuery(null, List.of(), null, page, size);
    }

    Page<CourseSummary> found(SearchCoursesQuery query) {
        return assertInstanceOf(SearchResult.Found.class, searchCourses.search(query)).page();
    }

    List<UUID> ids(SearchCoursesQuery query) {
        return found(query).items().stream().map(CourseSummary::id).toList();
    }

    @Test
    void withoutFiltersTheNewestComeFirstOnTheFirstPageOf20() {
        givenThreeCourses();

        Page<CourseSummary> page = found(query(null, null, null));

        assertEquals(List.of(TREES, ENGLISH, GRAPHS), page.items().stream().map(CourseSummary::id).toList());
        assertEquals(0, page.request().page());
        assertEquals(20, page.request().size());
        assertEquals(3, page.total());
    }

    @Test
    void filtersByTextTagsAndLanguage() {
        givenThreeCourses();

        assertEquals(List.of(GRAPHS), ids(query("GRAFOS", null, null)));
        assertEquals(List.of(TREES, GRAPHS), ids(query(null, List.of("Algoritmos"), null)));
        assertEquals(List.of(TREES), ids(query(null, List.of("algoritmos", "árboles"), null)));
        assertEquals(List.of(ENGLISH), ids(query(null, null, "en")));
    }

    @Test
    void onlyTheLatestVersionOfEachCourseCounts() {
        publish(GRAPHS, course().build(), MONDAY);
        repository.appendVersion(GRAPHS, AUTHOR.id(), CourseVersionDraft.of(
                course().title("Árboles y grafos").build(), new CourseContent("v2"), TUESDAY));

        List<CourseSummary> items = found(query(null, null, null)).items();

        assertEquals(1, items.size());
        assertEquals(2, items.getFirst().version());
        assertEquals(List.of(), ids(query("introducción", null, null)));
    }

    @Test
    void coursesPublishedAtTheSameInstantAreOrderedById() {
        // §4.5: sin un orden total, dos cursos con la misma fecha podrían repetirse o saltarse entre páginas.
        publish(TREES, course().build(), MONDAY);
        publish(GRAPHS, course().build(), MONDAY);

        assertEquals(List.of(GRAPHS, TREES), ids(query(null, null, null)));
    }

    @Test
    void pagesSplitTheResultsAndKeepTheTotal() {
        givenThreeCourses();

        assertEquals(List.of(TREES, ENGLISH), ids(page("0", "2")));
        assertEquals(List.of(GRAPHS), ids(page("1", "2")));
        assertEquals(List.of(), ids(page("5", "2")));
        assertEquals(3, found(page("5", "2")).total());
    }

    @Test
    void blankPageAndSizeMeanTheDefaults() {
        assertEquals(20, found(page(" ", "")).request().size());
    }

    @Test
    void aPageOrSizeOutOfRangeIsAnInvalidParameter() {
        for (SearchCoursesQuery invalid : List.of(page("-1", null), page("abc", null), page(null, "0"),
                page(null, "101"), page(null, "2.5"), page("99999999999", null))) {
            SearchResult.Rejected rejected = assertInstanceOf(SearchResult.Rejected.class,
                    searchCourses.search(invalid), invalid.toString());

            assertEquals(ErrorCode.INVALID_PARAMETER, rejected.errors().getFirst().code());
        }
    }

    @Test
    void everyInvalidParameterIsReportedByName() {
        SearchResult.Rejected rejected = assertInstanceOf(SearchResult.Rejected.class,
                searchCourses.search(page("x", "1000")));

        assertEquals(List.of("page", "size"), rejected.errors().stream().map(ValidationError::field).toList());
    }
}
