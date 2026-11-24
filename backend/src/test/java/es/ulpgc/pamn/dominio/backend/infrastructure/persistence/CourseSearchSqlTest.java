package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseSearchSqlTest {

    private static final Instant SINCE = Instant.parse("2026-10-22T10:00:00Z");

    @Test
    void withoutFiltersThereIsNoWhereClause() {
        SqlWhere where = CourseSearchSql.where(CourseSearch.ANYTHING);

        assertEquals("", where.clause());
        assertEquals(List.of(), where.parameters());
    }

    @Test
    void theTextIsSearchedWithAnEscapedLikePattern() {
        SqlWhere where = CourseSearchSql.where(CourseSearch.of("100%", null, null));

        assertEquals("WHERE v.search_text LIKE ? ESCAPE '\\'", where.clause());
        assertEquals(List.of(new SqlParameter.Text("%100\\%%")), where.parameters());
    }

    @Test
    void tagsAreOneArrayThatMustBeContained() {
        SqlWhere where = CourseSearchSql.where(CourseSearch.of(null, List.of("Grafos", "algoritmos"), null));

        assertEquals("WHERE v.search_tags @> ?", where.clause());
        assertEquals(List.of(new SqlParameter.TextArray(List.of("grafos", "algoritmos"))), where.parameters());
    }

    @Test
    void everyFilterIsJoinedWithAndKeepingItsParametersInOrder() {
        SqlWhere where = CourseSearchSql.where(CourseSearch.of("grafos", List.of("algoritmos"), "es"));

        assertEquals("WHERE v.search_text LIKE ? ESCAPE '\\' AND v.search_tags @> ? AND v.language = ?",
                where.clause());
        assertEquals(List.of(new SqlParameter.Text("%grafos%"),
                new SqlParameter.TextArray(List.of("algoritmos")),
                new SqlParameter.Text("es")), where.parameters());
    }

    @Test
    void theAuthorIsSearchedInTheLowercasedNameWithAnEscapedLikePattern() {
        SqlWhere where = CourseSearchSql.where(CourseSearch.ANYTHING.withAuthor("ana_r"));

        assertEquals("WHERE lower(u.name) LIKE ? ESCAPE '\\'", where.clause());
        assertEquals(List.of(new SqlParameter.Text("%ana\\_r%")), where.parameters());
    }

    @Test
    void theAuthorPatternIsAlreadyNormalizedSoOnlyTheColumnIsLowercased() {
        // lower() solo se aplica a u.name: el patrón llega en minúsculas y con los espacios agrupados.
        SqlWhere where = CourseSearchSql.where(CourseSearch.ANYTHING.withAuthor("  ANA   Ruiz "));

        assertEquals(List.of(new SqlParameter.Text("%ana ruiz%")), where.parameters());
    }

    @Test
    void thePublicationDateIsAnInclusiveTimestampCondition() {
        SqlWhere where = CourseSearchSql.where(CourseSearch.ANYTHING.publishedSince(SINCE));

        assertEquals("WHERE v.published_at >= ?", where.clause());
        assertEquals(List.of(new SqlParameter.Timestamp(SINCE)), where.parameters());
    }

    @Test
    void theTimestampParameterIsTheInstantTruncatedToMicroseconds() {
        // Lo trunca CourseSearch: el SQL recibe exactamente el instante con el que compara matches.
        SqlWhere where = CourseSearchSql.where(
                CourseSearch.ANYTHING.publishedSince(Instant.parse("2026-10-22T10:00:00.123456789Z")));

        assertEquals(List.of(new SqlParameter.Timestamp(Instant.parse("2026-10-22T10:00:00.123456Z"))),
                where.parameters());
    }

    @Test
    void theFiveFiltersAreJoinedInOrderWithTheirParametersInTheSameOrder() {
        SqlWhere where = CourseSearchSql.where(CourseSearch.of("grafos", List.of("algoritmos"), "es")
                .withAuthor("Ana")
                .publishedSince(SINCE));

        assertEquals("WHERE v.search_text LIKE ? ESCAPE '\\' AND v.search_tags @> ? AND v.language = ?"
                + " AND lower(u.name) LIKE ? ESCAPE '\\' AND v.published_at >= ?", where.clause());
        assertEquals(List.of(new SqlParameter.Text("%grafos%"),
                new SqlParameter.TextArray(List.of("algoritmos")),
                new SqlParameter.Text("es"),
                new SqlParameter.Text("%ana%"),
                new SqlParameter.Timestamp(SINCE)), where.parameters());
    }

    @Test
    void aTimestampParameterIsNeverNull() {
        assertThrows(NullPointerException.class, () -> new SqlParameter.Timestamp(null));
    }

    @Test
    void conditionsCombineLikeAMonoid() {
        // NONE es el elemento neutro y «and» es asociativo: el orden de agrupar no cambia el resultado.
        SqlWhere a = SqlWhere.condition("a = ?", new SqlParameter.Text("1"));
        SqlWhere b = SqlWhere.condition("b = ?", new SqlParameter.Text("2"));
        SqlWhere c = SqlWhere.condition("c = ?", new SqlParameter.Text("3"));

        assertEquals(a, SqlWhere.NONE.and(a));
        assertEquals(a, a.and(SqlWhere.NONE));
        assertEquals(a.and(b).and(c), a.and(b.and(c)));
    }
}
