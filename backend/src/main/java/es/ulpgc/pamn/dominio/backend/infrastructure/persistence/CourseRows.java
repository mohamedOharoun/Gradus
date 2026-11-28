package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseMetadata;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.fromColumn;

/**
 * Filas de {@code course_versions} (alias {@code v}) unidas a {@code courses} ({@code c}) y {@code users}
 * ({@code u}) → objetos del dominio. Los constructores vuelven a comprobar los datos al leerlos.
 */
final class CourseRows {

    /** Las columnas de un {@link CourseSummary}: todo menos el contenido, que puede ocupar 2 MiB. */
    static final String SUMMARY_COLUMNS = """
            v.course_id, v.version, v.published_at, v.format_version, v.title, v.description, v.language,
            v.tags, v.search_tags, v.search_text, v.module_count, v.sha256, v.size_bytes,
            c.author_id, u.name AS author_name""";

    /** Solo la última versión de cada curso. */
    static final String FROM_LATEST = """
            FROM courses c
            JOIN course_versions v ON v.course_id = c.id AND v.version = c.latest_version
            JOIN users u ON u.id = c.author_id""";

    /** Cualquier versión. */
    static final String FROM_ANY_VERSION = """
            FROM courses c
            JOIN course_versions v ON v.course_id = c.id
            JOIN users u ON u.id = c.author_id""";

    private CourseRows() {
    }

    static CourseSummary summary(ResultSet row) throws SQLException {
        return new CourseSummary(
                row.getObject("course_id", UUID.class),
                row.getInt("version"),
                new CourseAuthor(row.getObject("author_id", UUID.class), row.getString("author_name")),
                fromColumn(row, "published_at"),
                new CourseMetadata(
                        row.getInt("format_version"),
                        row.getString("title"),
                        row.getString("description"),
                        row.getString("language"),
                        texts(row, "tags"),
                        texts(row, "search_tags"),
                        row.getString("search_text"),
                        row.getInt("module_count"),
                        row.getString("sha256"),
                        row.getInt("size_bytes")));
    }

    /** Necesita también la columna {@code v.content}. {@link CourseVersion} comprueba que coincide con su SHA-256. */
    static CourseVersion version(ResultSet row) throws SQLException {
        return new CourseVersion(summary(row), new CourseContent(row.getString("content")));
    }

    private static List<String> texts(ResultSet row, String column) throws SQLException {
        Array array = row.getArray(column);
        try {
            return List.of((String[]) array.getArray());
        } finally {
            array.free();
        }
    }
}
