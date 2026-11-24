package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchCoursesTest {

    static final UUID GRAPHS = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID TREES = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    static final UUID ENGLISH = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    static final UUID BY_LUIS = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    static final CourseAuthor LUIS =
            new CourseAuthor(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Luis Pérez");
    static final Instant MONDAY = Instant.parse("2026-11-16T09:00:00Z");
    static final Instant TUESDAY = Instant.parse("2026-11-17T09:00:00Z");
    static final Instant WEDNESDAY = Instant.parse("2026-11-18T09:00:00Z");
    /** La hora del reloj de los tests, y dónde empiezan «último mes» y «último año» (decisión C). */
    static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");
    static final Instant LAST_MONTH_START = Instant.parse("2026-10-22T10:00:00Z");
    static final Instant LAST_YEAR_START = Instant.parse("2025-11-22T10:00:00Z");
    static final String INVALID_PUBLISHED = "Debe ser «month» o «year».";

    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private final AdjustableClock clock = new AdjustableClock(NOW);
    private final SearchCourses searchCourses = new SearchCourses(repository, clock);

    void publish(UUID id, CourseDocument course, Instant at) {
        publish(id, AUTHOR, course, at);
    }

    void publish(UUID id, CourseAuthor author, CourseDocument course, Instant at) {
        repository.create(id, author, CourseVersionDraft.of(course, new CourseContent(id + " v1"), at));
    }

    /** Tres cursos de Ana. */
    void givenThreeCourses() {
        publish(GRAPHS, course().build(), MONDAY);
        publish(TREES, course().title("Árboles binarios").description("Recorridos en profundidad.")
                .tags("árboles", "algoritmos").build(), WEDNESDAY);
        publish(ENGLISH, course().title("Graphs").description("Breadth-first search.").language("en")
                .tags("graphs").build(), TUESDAY);
    }

    static SearchCoursesQuery query(String text, List<String> tags, String language) {
        return new SearchCoursesQuery(text, tags, language, null, null, null, null);
    }

    static SearchCoursesQuery byAuthor(String author, List<String> tags) {
        return new SearchCoursesQuery(null, tags, null, author, null, null, null);
    }

    static SearchCoursesQuery publishedWithin(String published) {
        return new SearchCoursesQuery(null, List.of(), null, null, published, null, null);
    }

    static SearchCoursesQuery page(String page, String size) {
        return new SearchCoursesQuery(null, List.of(), null, null, null, page, size);
    }

    Page<CourseSummary> found(SearchCoursesQuery query) {
        return assertInstanceOf(SearchResult.Found.class, searchCourses.search(query)).page();
    }

    List<UUID> ids(SearchCoursesQuery query) {
        return found(query).items().stream().map(CourseSummary::id).toList();
    }

    SearchResult.Rejected rejected(SearchCoursesQuery query) {
        return assertInstanceOf(SearchResult.Rejected.class, searchCourses.search(query), query.toString());
    }

    @Test
    void requiresARepositoryAndAClock() {
        assertThrows(NullPointerException.class, () -> new SearchCourses(null, clock));
        assertThrows(NullPointerException.class, () -> new SearchCourses(repository, null));
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
    void filtersByAuthor() {
        givenThreeCourses();
        publish(BY_LUIS, LUIS, course().tags("árboles").build(), TUESDAY);

        assertEquals(List.of(TREES, ENGLISH, GRAPHS), ids(byAuthor("ANA", null)));
        assertEquals(List.of(BY_LUIS), ids(byAuthor("luis", null)));
    }

    @Test
    void theAuthorCombinesWithATag() {
        givenThreeCourses();
        publish(BY_LUIS, LUIS, course().tags("árboles").build(), TUESDAY);

        assertEquals(List.of(TREES), ids(byAuthor("ana", List.of("árboles"))));
        assertEquals(List.of(BY_LUIS), ids(byAuthor("luis", List.of("árboles"))));
        // ENGLISH tiene la etiqueta, pero es de Ana: hay que cumplir los dos filtros.
        assertEquals(List.of(), ids(byAuthor("luis", List.of("graphs"))));
    }

    @Test
    void theAuthorIsNeverAnInvalidParameterAndBlankMeansAnyone() {
        // Decisión B: como q, author nunca es inválido; si no encuentra nada, la página está vacía.
        givenThreeCourses();

        assertEquals(3, found(byAuthor("   ", null)).total());
        assertEquals(List.of(), ids(byAuthor("%_\\ 123 ¿?", null)));
    }

    @Test
    void aCourseFrom40DaysAgoIsInTheLastYearButNotInTheLastMonth() {
        publish(GRAPHS, course().build(), NOW.minus(Duration.ofDays(40)));

        assertEquals(List.of(), ids(publishedWithin("month")));
        assertEquals(List.of(GRAPHS), ids(publishedWithin("year")));
    }

    @Test
    void aCourseFromTwoYearsAgoIsInNeitherWindow() {
        publish(GRAPHS, course().build(), Instant.parse("2024-11-22T10:00:00Z"));

        assertEquals(List.of(), ids(publishedWithin("month")));
        assertEquals(List.of(), ids(publishedWithin("year")));
    }

    @Test
    void theLastMonthIncludesItsStartButNotAMicrosecondBefore() {
        publish(GRAPHS, course().build(), LAST_MONTH_START);
        publish(TREES, course().build(), LAST_MONTH_START.minus(1, ChronoUnit.MICROS));

        assertEquals(List.of(GRAPHS), ids(publishedWithin("month")));
    }

    @Test
    void theLastYearIncludesItsStartButNotAMicrosecondBefore() {
        publish(GRAPHS, course().build(), LAST_YEAR_START);
        publish(TREES, course().build(), LAST_YEAR_START.minus(1, ChronoUnit.MICROS));

        assertEquals(List.of(GRAPHS), ids(publishedWithin("year")));
    }

    @Test
    void publishedAllowsSpacesAround() {
        publish(GRAPHS, course().build(), NOW.minus(Duration.ofDays(40)));

        assertEquals(List.of(), ids(publishedWithin(" month ")));
        assertEquals(List.of(GRAPHS), ids(publishedWithin("\tyear\n")));
    }

    @Test
    void aMissingOrBlankPublishedMeansAnyDate() {
        // «Cualquiera» se pide no enviando published (decisión A).
        publish(GRAPHS, course().build(), Instant.parse("2020-01-01T00:00:00Z"));

        for (String any : Arrays.asList(null, "", "   ")) {
            assertEquals(List.of(GRAPHS), ids(publishedWithin(any)), String.valueOf(any));
        }
    }

    @Test
    void anUnknownPublishedIsAnInvalidParameter() {
        // Decisión B: solo month y year, en minúsculas; no hay «any».
        for (String invalid : List.of("week", "Month", "YEAR", "1", "any")) {
            assertEquals(List.of(new ValidationError("published", ErrorCode.INVALID_PARAMETER, INVALID_PUBLISHED)),
                    rejected(publishedWithin(invalid)).errors(), invalid);
        }
    }

    @Test
    void theNewFiltersCombineWithTheOthers() {
        publish(TREES, course().tags("árboles").build(), NOW.minus(Duration.ofDays(3)));
        publish(GRAPHS, course().tags("árboles").build(), NOW.minus(Duration.ofDays(400)));
        publish(BY_LUIS, LUIS, course().tags("árboles").build(), NOW.minus(Duration.ofDays(3)));
        publish(ENGLISH, course().build(), NOW.minus(Duration.ofDays(3)));

        SearchCoursesQuery query = new SearchCoursesQuery(null, List.of("árboles"), null, "ana", "year", null, null);

        assertEquals(List.of(TREES), ids(query));
    }

    @Test
    void theWindowIsMeasuredAtEachSearchNotWhenTheUseCaseIsCreated() {
        publish(GRAPHS, course().build(), NOW.minus(Duration.ofDays(20)));
        assertEquals(List.of(GRAPHS), ids(publishedWithin("month")));

        clock.advance(Duration.ofDays(15));

        assertEquals(List.of(), ids(publishedWithin("month")));
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
            assertEquals(ErrorCode.INVALID_PARAMETER, rejected(invalid).errors().getFirst().code());
        }
    }

    @Test
    void everyInvalidParameterIsReportedByName() {
        SearchResult.Rejected rejected = rejected(page("x", "1000"));

        assertEquals(List.of("page", "size"), rejected.errors().stream().map(ValidationError::field).toList());
    }

    @Test
    void invalidPageSizeAndPublishedAreAllReportedInThatOrder() {
        SearchResult.Rejected rejected = rejected(
                new SearchCoursesQuery(null, List.of(), null, null, "week", "x", "1000"));

        assertEquals(List.of("page", "size", "published"),
                rejected.errors().stream().map(ValidationError::field).toList());
    }
}
