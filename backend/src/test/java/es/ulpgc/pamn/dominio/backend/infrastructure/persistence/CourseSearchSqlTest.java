package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseSearchSqlTest {

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
