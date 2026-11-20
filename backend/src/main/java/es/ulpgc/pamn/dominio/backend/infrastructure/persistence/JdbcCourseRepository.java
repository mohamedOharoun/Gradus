package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.application.course.AppendVersionResult;
import es.ulpgc.pamn.dominio.backend.application.course.CourseRepository;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseMetadata;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.course.PageRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.toColumn;

/**
 * {@link CourseRepository} sobre PostgreSQL con JDBC (tablas {@code courses} y {@code course_versions}, ver
 * {@code 004_create_courses.sql}). Publicar usa una sola conexión y una transacción ({@link Transactions}).
 */
public final class JdbcCourseRepository implements CourseRepository {

    private static final String INSERT_COURSE = """
            INSERT INTO courses (id, author_id, created_at, latest_version)
            VALUES (?, ?, ?, 1)
            """;
    private static final String INSERT_VERSION = """
            INSERT INTO course_versions (course_id, version, published_at, format_version, title, description,
                                         language, tags, search_tags, search_text, module_count, sha256,
                                         size_bytes, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    // FOR UPDATE OF c: bloquea solo la fila del curso. Una segunda publicación del mismo curso espera aquí
    // hasta el commit de la primera y entonces ve el latest_version ya actualizado (§4.2).
    private static final String LOCK_COURSE = """
            SELECT c.author_id, u.name AS author_name, c.latest_version
            FROM courses c
            JOIN users u ON u.id = c.author_id
            WHERE c.id = ?
            FOR UPDATE OF c
            """;
    private static final String SELECT_SHA256 = "SELECT sha256 FROM course_versions WHERE course_id = ? AND version = ?";
    private static final String UPDATE_LATEST_VERSION = "UPDATE courses SET latest_version = ? WHERE id = ?";

    private static final String SELECT_LATEST = "SELECT " + CourseRows.SUMMARY_COLUMNS + ", v.content "
            + CourseRows.FROM_LATEST + " WHERE c.id = ?";
    private static final String SELECT_VERSION = "SELECT " + CourseRows.SUMMARY_COLUMNS + ", v.content "
            + CourseRows.FROM_ANY_VERSION + " WHERE c.id = ? AND v.version = ?";
    // §4.5: el segundo criterio desempata. Sin un orden total, dos cursos con la misma fecha podrían
    // repetirse o saltarse entre páginas.
    private static final String ORDER_AND_PAGE = " ORDER BY v.published_at DESC, c.id LIMIT ? OFFSET ?";

    private final Transactions transactions;

    public JdbcCourseRepository(DataSource dataSource) {
        this.transactions = new Transactions(dataSource);
    }

    @Override
    public CourseSummary create(UUID courseId, CourseAuthor author, CourseVersionDraft draft) {
        return transactions.inTransaction(connection -> {
            try (PreparedStatement insert = connection.prepareStatement(INSERT_COURSE)) {
                insert.setObject(1, courseId);
                insert.setObject(2, author.id());
                insert.setObject(3, toColumn(draft.publishedAt()));
                insert.executeUpdate();
            }
            insertVersion(connection, courseId, CourseSummary.FIRST_VERSION, draft);
            return new CourseSummary(courseId, CourseSummary.FIRST_VERSION, author, draft.publishedAt(),
                    draft.metadata());
        });
    }

    @Override
    public AppendVersionResult appendVersion(UUID courseId, UUID requesterId, CourseVersionDraft draft) {
        return transactions.inTransaction(connection -> {
            try (PreparedStatement lock = connection.prepareStatement(LOCK_COURSE)) {
                lock.setObject(1, courseId);
                try (ResultSet course = lock.executeQuery()) {
                    if (!course.next()) {
                        return new AppendVersionResult.CourseNotFound();
                    }
                    CourseAuthor author = new CourseAuthor(course.getObject("author_id", UUID.class),
                            course.getString("author_name"));
                    if (!author.id().equals(requesterId)) {
                        return new AppendVersionResult.NotAuthor();
                    }
                    int latest = course.getInt("latest_version");
                    if (latestSha256(connection, courseId, latest).equals(draft.metadata().sha256())) {
                        return new AppendVersionResult.IdenticalToLatest();
                    }
                    int next = latest + 1;
                    insertVersion(connection, courseId, next, draft);
                    updateLatestVersion(connection, courseId, next);
                    return new AppendVersionResult.Appended(
                            new CourseSummary(courseId, next, author, draft.publishedAt(), draft.metadata()));
                }
            }
        });
    }

    @Override
    public Optional<CourseVersion> findLatest(UUID courseId) {
        return findOne(SELECT_LATEST, List.of(courseId));
    }

    @Override
    public Optional<CourseVersion> findVersion(UUID courseId, int version) {
        return findOne(SELECT_VERSION, List.of(courseId, version));
    }

    /** El total y la página se leen de la misma foto de la base de datos ({@link Transactions#inSnapshot}). */
    @Override
    public Page<CourseSummary> search(CourseSearch search, PageRequest page) {
        SqlWhere where = CourseSearchSql.where(search);
        return transactions.inSnapshot(connection -> {
            long total;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT count(*) " + CourseRows.FROM_LATEST + " " + where.clause())) {
                bind(connection, count, where.parameters());
                try (ResultSet row = count.executeQuery()) {
                    row.next();
                    total = row.getLong(1);
                }
            }
            List<CourseSummary> items = new ArrayList<>();
            try (PreparedStatement select = connection.prepareStatement("SELECT " + CourseRows.SUMMARY_COLUMNS + " "
                    + CourseRows.FROM_LATEST + " " + where.clause() + ORDER_AND_PAGE)) {
                int next = bind(connection, select, where.parameters());
                select.setInt(next, page.size());
                select.setLong(next + 1, page.offset());
                try (ResultSet rows = select.executeQuery()) {
                    while (rows.next()) {
                        items.add(CourseRows.summary(rows));
                    }
                }
            }
            return new Page<>(items, page, total);
        });
    }

    /** Una consulta que devuelve como mucho una versión (la clave primaria es curso y versión). */
    private Optional<CourseVersion> findOne(String sql, List<Object> keys) {
        return transactions.inSnapshot(connection -> {
            try (PreparedStatement select = connection.prepareStatement(sql)) {
                for (int i = 0; i < keys.size(); i++) {
                    select.setObject(i + 1, keys.get(i));
                }
                try (ResultSet row = select.executeQuery()) {
                    return row.next() ? Optional.of(CourseRows.version(row)) : Optional.empty();
                }
            }
        });
    }

    /** Vincula los parámetros del {@code WHERE} desde el primer {@code ?} y devuelve la posición del siguiente. */
    private static int bind(Connection connection, PreparedStatement statement, List<SqlParameter> parameters)
            throws SQLException {
        int position = 1;
        for (SqlParameter parameter : parameters) {
            switch (parameter) {
                case SqlParameter.Text text -> statement.setString(position, text.value());
                case SqlParameter.TextArray array ->
                        statement.setArray(position, connection.createArrayOf("text", array.values().toArray()));
            }
            position++;
        }
        return position;
    }

    private static void insertVersion(Connection connection, UUID courseId, int version, CourseVersionDraft draft)
            throws SQLException {
        CourseMetadata metadata = draft.metadata();
        try (PreparedStatement insert = connection.prepareStatement(INSERT_VERSION)) {
            insert.setObject(1, courseId);
            insert.setInt(2, version);
            insert.setObject(3, toColumn(draft.publishedAt()));
            insert.setInt(4, metadata.formatVersion());
            insert.setString(5, metadata.title());
            insert.setString(6, metadata.description());
            insert.setString(7, metadata.language());
            insert.setArray(8, connection.createArrayOf("text", metadata.tags().toArray()));
            insert.setArray(9, connection.createArrayOf("text", metadata.searchTags().toArray()));
            insert.setString(10, metadata.searchText());
            insert.setInt(11, metadata.moduleCount());
            insert.setString(12, metadata.sha256());
            insert.setInt(13, metadata.sizeBytes());
            insert.setString(14, draft.content().text());
            insert.executeUpdate();
        }
    }

    private static String latestSha256(Connection connection, UUID courseId, int version) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(SELECT_SHA256)) {
            select.setObject(1, courseId);
            select.setInt(2, version);
            try (ResultSet row = select.executeQuery()) {
                if (!row.next()) {
                    throw new IllegalStateException("El curso " + courseId + " no tiene su versión " + version);
                }
                return row.getString("sha256");
            }
        }
    }

    private static void updateLatestVersion(Connection connection, UUID courseId, int version) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(UPDATE_LATEST_VERSION)) {
            update.setInt(1, version);
            update.setObject(2, courseId);
            update.executeUpdate();
        }
    }
}
