package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Traduce la Specification {@link CourseSearch} a SQL, con el mismo significado que
 * {@link CourseSearch#matches}. Las columnas son las de {@link CourseRows#FROM_LATEST}: {@code course_versions}
 * con el alias {@code v} y {@code users} con el alias {@code u}.
 */
final class CourseSearchSql {

    private CourseSearchSql() {
    }

    static SqlWhere where(CourseSearch search) {
        return Stream.of(
                        search.text().map(text -> SqlWhere.condition("v.search_text LIKE ? ESCAPE '\\'",
                                new SqlParameter.Text(LikePattern.containing(text)))),
                        // @> es «contiene todas»: el Y lógico de las etiquetas (§4.5).
                        Optional.of(search.tags()).filter(tags -> !tags.isEmpty())
                                .map(tags -> SqlWhere.condition("v.search_tags @> ?", new SqlParameter.TextArray(tags))),
                        search.language().map(language -> SqlWhere.condition("v.language = ?",
                                new SqlParameter.Text(language))),
                        // El autor ya llega normalizado (en minúsculas): solo hace falta bajar la columna, como
                        // toLowerCase(Locale.ROOT) en matches. Comprobado con el locale de la base (en_US.utf8).
                        search.author().map(author -> SqlWhere.condition("lower(u.name) LIKE ? ESCAPE '\\'",
                                new SqlParameter.Text(LikePattern.containing(author)))),
                        // >=: el límite está incluido, como !isBefore en matches.
                        search.publishedSince().map(since -> SqlWhere.condition("v.published_at >= ?",
                                new SqlParameter.Timestamp(since))))
                .flatMap(Optional::stream)
                .reduce(SqlWhere.NONE, SqlWhere::and);
    }
}
