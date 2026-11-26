package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.application.submission.SubmissionRepository;
import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;
import es.ulpgc.pamn.dominio.backend.domain.submission.SubmissionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.fromColumn;
import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.toColumn;

/**
 * {@link SubmissionRepository} sobre PostgreSQL con JDBC (docs/tareas/ejercicio-foto.md, paso 3d). Tabla
 * {@code submissions} de {@code db/init/005_create_submissions.sql}; la foto va en la columna {@code photo}.
 *
 * <p>Listar y buscar <b>no leen</b> la columna {@code photo} (solo {@code photo_bytes}): una foto ocupa hasta
 * 2 MB. Solo {@link #photo} la lee. Las consultas no ordenan: el orden lo decide {@code ListMySubmissions}.
 */
public final class JdbcSubmissionRepository implements SubmissionRepository {

    // Una sola sentencia: la entrega y su foto se guardan juntas o ninguna.
    private static final String INSERT = """
            INSERT INTO submissions (id, student_id, course_id, course_version, exercise_id, submitted_at,
                                     status, photo, photo_bytes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    // Todas las columnas menos la foto.
    private static final String SELECT_SUBMISSION = """
            SELECT id, student_id, course_id, course_version, exercise_id, submitted_at, status, photo_bytes
            FROM submissions
            """;
    private static final String SELECT_MINE = SELECT_SUBMISSION + "WHERE student_id = ? AND course_id = ?";
    private static final String SELECT_BY_ID = SELECT_SUBMISSION + "WHERE id = ?";
    private static final String SELECT_PHOTO = "SELECT photo FROM submissions WHERE id = ?";

    private final DataSource dataSource;

    public JdbcSubmissionRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void add(Submission submission, Jpeg photo) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insert.setObject(1, submission.id());
            insert.setObject(2, submission.studentId());
            insert.setObject(3, submission.courseId());
            insert.setInt(4, submission.courseVersion());
            insert.setString(5, submission.exerciseId());
            insert.setObject(6, toColumn(submission.submittedAt()));
            insert.setString(7, submission.status().name());
            insert.setBytes(8, photo.bytes());
            insert.setInt(9, submission.photoBytes());
            insert.executeUpdate();
        } catch (SQLException e) {
            // Fallo inesperado (base de datos caída, etc.): no es un caso de negocio, así que se propaga.
            throw new IllegalStateException("No se pudo guardar la entrega " + submission.id(), e);
        }
    }

    @Override
    public List<Submission> mine(UUID studentId, UUID courseId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(SELECT_MINE)) {
            select.setObject(1, studentId);
            select.setObject(2, courseId);
            try (ResultSet rows = select.executeQuery()) {
                List<Submission> found = new ArrayList<>();
                while (rows.next()) {
                    found.add(toSubmission(rows));
                }
                return found;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron listar las entregas", e);
        }
    }

    @Override
    public Optional<Submission> find(UUID submissionId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(SELECT_BY_ID)) {
            select.setObject(1, submissionId);
            try (ResultSet row = select.executeQuery()) {
                return row.next() ? Optional.of(toSubmission(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo buscar la entrega " + submissionId, e);
        }
    }

    @Override
    public Optional<Jpeg> photo(UUID submissionId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(SELECT_PHOTO)) {
            select.setObject(1, submissionId);
            try (ResultSet row = select.executeQuery()) {
                return row.next() ? Optional.of(toJpeg(submissionId, row.getBytes("photo"))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la foto de la entrega " + submissionId, e);
        }
    }

    /** La fila actual como {@link Submission}; el constructor vuelve a comprobar los datos. */
    private static Submission toSubmission(ResultSet row) throws SQLException {
        return new Submission(
                row.getObject("id", UUID.class),
                row.getObject("student_id", UUID.class),
                row.getObject("course_id", UUID.class),
                row.getInt("course_version"),
                row.getString("exercise_id"),
                fromColumn(row, "submitted_at"),
                SubmissionStatus.valueOf(row.getString("status")),
                row.getInt("photo_bytes"));
    }

    /** Lo guardado ya se comprobó al entregar; si ahora no es un JPEG válido, es fallo del servidor. */
    private static Jpeg toJpeg(UUID submissionId, byte[] bytes) {
        return switch (Jpeg.check(bytes)) {
            case JpegCheck.Valid valid -> valid.jpeg();
            case JpegCheck rejected -> throw new IllegalStateException(
                    "La foto guardada de la entrega " + submissionId + " no es válida: " + rejected);
        };
    }
}
