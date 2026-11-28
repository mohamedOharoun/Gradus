package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.CONTENT;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.COURSE_ID;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseSearchTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-18T08:35:00Z");

    /** «Introducción a los grafos», etiquetas grafos y algoritmos, en español, de «Ana Ruiz». */
    static final CourseSummary GRAPHS = summary("Ana Ruiz", PUBLISHED_AT);

    private static CourseSummary summary(String authorName, Instant publishedAt) {
        return summary(authorName, publishedAt, CourseMetadata.of(course().build(), CONTENT));
    }

    private static CourseSummary summary(String authorName, Instant publishedAt, CourseMetadata metadata) {
        return new CourseSummary(COURSE_ID, 1, new CourseAuthor(AUTHOR.id(), authorName), publishedAt,
                metadata);
    }

    // ---- Texto, etiquetas e idioma (los de siempre; solo cambia el ejemplo) ----

    @Test
    void textAndTagsAreNormalizedAndBlanksAreIgnored() {
        CourseSearch search = CourseSearch.of("  Los   GRAFOS ", List.of("Algoritmos", " ", "grafos", "GRAFOS"), "");

        assertEquals(Optional.of("los grafos"), search.text());
        assertEquals(List.of("algoritmos", "grafos"), search.tags());
        assertEquals(Optional.empty(), search.language());
    }

    @Test
    void missingParametersMeanNoFilter() {
        CourseSearch search = CourseSearch.of(null, null, null);

        assertEquals(CourseSearch.ANYTHING, search);
        assertTrue(search.matches(GRAPHS));
    }

    @Test
    void theTextIsASubstringOfTitleOrDescription() {
        assertTrue(CourseSearch.of("GRAFOS", List.of(), null).matches(GRAPHS));
        assertTrue(CourseSearch.of("en anchura", List.of(), null).matches(GRAPHS));
        assertFalse(CourseSearch.of("dijkstra", List.of(), null).matches(GRAPHS));
    }

    @Test
    void everyTagMustBePresent() {
        assertTrue(CourseSearch.of(null, List.of("Grafos", "algoritmos"), null).matches(GRAPHS));
        assertFalse(CourseSearch.of(null, List.of("grafos", "redes"), null).matches(GRAPHS));
    }

    @Test
    void aTagMustMatchWholeNotAsASubstring() {
        assertFalse(CourseSearch.of(null, List.of("graf"), null).matches(GRAPHS));
    }

    @Test
    void theLanguageMustMatchExactly() {
        assertTrue(CourseSearch.of(null, null, "es").matches(GRAPHS));
        assertFalse(CourseSearch.of(null, null, "es-ES").matches(GRAPHS));
    }

    @Test
    void wildcardsInTheTextAreLiteral() {
        // §4.5: buscar «100%» no puede convertirse en un comodín.
        CourseSummary percent = summary("Ana Ruiz", PUBLISHED_AT,
                CourseMetadata.of(course().title("Grafos al 100% de capacidad").build(), CONTENT));

        assertTrue(CourseSearch.of("100%", null, null).matches(percent));
        assertFalse(CourseSearch.of("100%", null, null).matches(GRAPHS));
        assertFalse(CourseSearch.of("gra_os", null, null).matches(GRAPHS));
    }

    @Test
    void theTagListIsImmutable() {
        CourseSearch search = CourseSearch.of(null, Arrays.asList("grafos"), null);

        assertEquals(List.of("grafos"), search.tags());
        assertThrows(UnsupportedOperationException.class, () -> search.tags().add("x"));
    }

    // ---- Autor (decisión D) ----

    @Test
    void ofLeavesAuthorAndDateEmpty() {
        CourseSearch search = CourseSearch.of("grafos", List.of("algoritmos"), "es");

        assertEquals(Optional.empty(), search.author());
        assertEquals(Optional.empty(), search.publishedSince());
    }

    @Test
    void theAuthorIsNormalized() {
        assertEquals(Optional.of("ana ruiz"), CourseSearch.ANYTHING.withAuthor("  ANA   Ruiz ").author());
    }

    @Test
    void aNullOrBlankAuthorMeansNoFilter() {
        assertEquals(CourseSearch.ANYTHING, CourseSearch.ANYTHING.withAuthor(null));
        assertEquals(CourseSearch.ANYTHING, CourseSearch.ANYTHING.withAuthor(""));
        assertEquals(CourseSearch.ANYTHING, CourseSearch.ANYTHING.withAuthor(" \t\n "));
    }

    @Test
    void aNullOrBlankAuthorRemovesAnEarlierOne() {
        CourseSearch withAuthor = CourseSearch.ANYTHING.withAuthor("ana");

        assertEquals(Optional.empty(), withAuthor.withAuthor(null).author());
        assertEquals(Optional.empty(), withAuthor.withAuthor("  ").author());
    }

    @Test
    void theAuthorIsACaseInsensitiveSubstring() {
        assertTrue(CourseSearch.ANYTHING.withAuthor("ANA").matches(GRAPHS));
        assertTrue(CourseSearch.ANYTHING.withAuthor("na ru").matches(GRAPHS));
        assertTrue(CourseSearch.ANYTHING.withAuthor("Ana Ruiz").matches(GRAPHS));
        assertFalse(CourseSearch.ANYTHING.withAuthor("Luis").matches(GRAPHS));
    }

    @Test
    void accentsAreNotStrippedInTheAuthor() {
        CourseSummary angel = summary("Ángel", PUBLISHED_AT);

        assertTrue(CourseSearch.ANYTHING.withAuthor("ángel").matches(angel));
        assertTrue(CourseSearch.ANYTHING.withAuthor("ÁNGEL").matches(angel));
        assertFalse(CourseSearch.ANYTHING.withAuthor("angel").matches(angel));
    }

    @Test
    void wildcardsInTheAuthorAreLiteral() {
        CourseSummary underscore = summary("Ana_Ruiz", PUBLISHED_AT);

        assertTrue(CourseSearch.ANYTHING.withAuthor("a_r").matches(underscore));
        assertFalse(CourseSearch.ANYTHING.withAuthor("a_r").matches(GRAPHS));
        assertFalse(CourseSearch.ANYTHING.withAuthor("%").matches(GRAPHS));
    }

    @Test
    void aPercentSignInTheAuthorMatchesOnlyAPercentSign() {
        assertTrue(CourseSearch.ANYTHING.withAuthor("100%").matches(summary("Ana 100% Ruiz", PUBLISHED_AT)));
        assertFalse(CourseSearch.ANYTHING.withAuthor("100%").matches(GRAPHS));
    }

    @Test
    void doubleSpacesInsideTheStoredNameAreNotCollapsed() {
        // Límite aceptado (decisión D): UserName solo recorta los extremos, y SQL se comporta igual.
        CourseSummary doubleSpace = summary("Ana  Ruiz", PUBLISHED_AT);

        assertFalse(CourseSearch.ANYTHING.withAuthor("ana ruiz").matches(doubleSpace));
        // Tampoco ayuda escribir los dos espacios: el texto buscado también se normaliza.
        assertFalse(CourseSearch.ANYTHING.withAuthor("ana  ruiz").matches(doubleSpace));
        assertTrue(CourseSearch.ANYTHING.withAuthor("ana").matches(doubleSpace));
    }

    @Test
    void theAuthorIsLowercasedTheSameWayWhateverTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            // En turco, «I» en minúscula es «ı» (sin punto); con Locale.ROOT siempre es «i».
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertTrue(CourseSearch.ANYTHING.withAuthor("ISABEL").matches(summary("Isabel Díaz", PUBLISHED_AT)));
        } finally {
            Locale.setDefault(original);
        }
    }

    // ---- Fecha de publicación (decisiones C y E) ----

    @Test
    void theLimitIsTruncatedToMicroseconds() {
        Instant since = Instant.parse("2026-10-22T10:00:00.123456789Z");

        assertEquals(Optional.of(Instant.parse("2026-10-22T10:00:00.123456Z")),
                CourseSearch.ANYTHING.publishedSince(since).publishedSince());
    }

    @Test
    void theLimitCannotBeNull() {
        assertThrows(NullPointerException.class, () -> CourseSearch.ANYTHING.publishedSince(null));
    }

    @Test
    void aCoursePublishedExactlyAtTheLimitIsIncluded() {
        Instant limit = Instant.parse("2026-10-22T10:00:00Z");

        assertTrue(CourseSearch.ANYTHING.publishedSince(limit).matches(summary("Ana", limit)));
    }

    @Test
    void aCoursePublishedOneMicrosecondBeforeTheLimitIsExcluded() {
        Instant limit = Instant.parse("2026-10-22T10:00:00Z");

        assertFalse(CourseSearch.ANYTHING.publishedSince(limit)
                .matches(summary("Ana", limit.minus(1, ChronoUnit.MICROS))));
        assertTrue(CourseSearch.ANYTHING.publishedSince(limit)
                .matches(summary("Ana", limit.plus(1, ChronoUnit.MICROS))));
    }

    @Test
    void theLimitIsTruncatedBeforeComparing() {
        // Con nanosegundos sin truncar, «10:00:00.000000» quedaría fuera; PostgreSQL no los guarda.
        Instant limit = Instant.parse("2026-10-22T10:00:00.000000999Z");

        assertTrue(CourseSearch.ANYTHING.publishedSince(limit)
                .matches(summary("Ana", Instant.parse("2026-10-22T10:00:00Z"))));
    }

    @Test
    void withoutADateFilterEveryCourseEnters() {
        assertTrue(CourseSearch.ANYTHING.matches(summary("Ana", Instant.EPOCH)));
        assertTrue(CourseSearch.ANYTHING.matches(summary("Ana", Instant.parse("2070-01-01T00:00:00Z"))));
    }

    // ---- Combinación ----

    @Test
    void allFiltersAreCombinedWithAnd() {
        CourseSearch everything = CourseSearch.of("grafos", List.of("algoritmos"), "es")
                .withAuthor("ana")
                .publishedSince(PUBLISHED_AT);

        assertTrue(everything.matches(GRAPHS));
        // Fallar uno solo basta para quedar fuera.
        assertFalse(CourseSearch.of("dijkstra", List.of("algoritmos"), "es")
                .withAuthor("ana").publishedSince(PUBLISHED_AT).matches(GRAPHS));
        assertFalse(CourseSearch.of("grafos", List.of("redes"), "es")
                .withAuthor("ana").publishedSince(PUBLISHED_AT).matches(GRAPHS));
        assertFalse(CourseSearch.of("grafos", List.of("algoritmos"), "en")
                .withAuthor("ana").publishedSince(PUBLISHED_AT).matches(GRAPHS));
        assertFalse(CourseSearch.of("grafos", List.of("algoritmos"), "es")
                .withAuthor("luis").publishedSince(PUBLISHED_AT).matches(GRAPHS));
        assertFalse(CourseSearch.of("grafos", List.of("algoritmos"), "es")
                .withAuthor("ana").publishedSince(PUBLISHED_AT.plus(1, ChronoUnit.MICROS)).matches(GRAPHS));
    }

    @Test
    void theWithersKeepTheOtherCriteriaInAnyOrder() {
        Instant since = Instant.parse("2026-10-22T10:00:00Z");
        CourseSearch authorFirst = CourseSearch.of("grafos", List.of("algoritmos"), "es")
                .withAuthor("Ana").publishedSince(since);
        CourseSearch dateFirst = CourseSearch.of("grafos", List.of("algoritmos"), "es")
                .publishedSince(since).withAuthor("Ana");

        assertEquals(authorFirst, dateFirst);
        assertEquals(new CourseSearch(Optional.of("grafos"), List.of("algoritmos"), Optional.of("es"),
                Optional.of("ana"), Optional.of(since)), authorFirst);
    }

    @Test
    void theWithersDoNotModifyTheOriginal() {
        CourseSearch original = CourseSearch.of("grafos", List.of(), null);

        CourseSearch withAuthor = original.withAuthor("ana");
        CourseSearch withDate = original.publishedSince(PUBLISHED_AT);

        assertNotSame(original, withAuthor);
        assertNotSame(original, withDate);
        assertEquals(Optional.empty(), original.author());
        assertEquals(Optional.empty(), original.publishedSince());
        assertEquals(CourseSearch.of("grafos", List.of(), null), original);
    }
}
