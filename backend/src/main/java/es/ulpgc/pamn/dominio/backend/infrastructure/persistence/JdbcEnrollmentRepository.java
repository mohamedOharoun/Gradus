package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.application.progress.EnrollmentRepository;
import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.toColumn;

/**
 * {@link EnrollmentRepository} sobre PostgreSQL con JDBC (tablas {@code enrollments} y {@code
 * mastered_modules}, ver {@code 006_create_enrollments.sql}).
 */
public final class JdbcEnrollmentRepository implements EnrollmentRepository {

    // Las mismas sentencias que prueba backend/db/checks/006_enrollments_check.sql (con $n en lugar de ?):
    // unión, gana la fecha más antigua, nunca se borra nada.
    private static final String MERGE_ENROLLMENT = """
            INSERT INTO enrollments (user_id, course_id, enrolled_at)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, course_id)
            DO UPDATE SET enrolled_at = LEAST(enrollments.enrolled_at, EXCLUDED.enrolled_at)
            """;
    private static final String MERGE_MODULE = """
            INSERT INTO mastered_modules (user_id, course_id, module_id, mastered_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id, course_id, module_id)
            DO UPDATE SET mastered_at = LEAST(mastered_modules.mastered_at, EXCLUDED.mastered_at)
            """;
    private static final String SELECT_ENROLLMENTS = """
            SELECT course_id, enrolled_at FROM enrollments
            WHERE user_id = ?
            ORDER BY enrolled_at, course_id
            """;
    private static final String SELECT_MODULES = """
            SELECT course_id, module_id, mastered_at FROM mastered_modules
            WHERE user_id = ?
            ORDER BY module_id
            """;

    private final Transactions transactions;

    public JdbcEnrollmentRepository(DataSource dataSource) {
        this.transactions = new Transactions(dataSource);
    }

    /**
     * Una transacción, en el orden fijo de {@link EnrollmentWriteOrder}: primero todas las inscripciones y
     * después todos los módulos (cada inscripción, antes que los suyos: la clave ajena lo exige). La
     * fusión la hace PostgreSQL con {@code ON CONFLICT}; aquí no se lee nada.
     */
    @Override
    public void mergeAll(UUID userId, List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) {
            return; // Nada que fusionar (y el puerto nunca borra): ni conexión ni transacción.
        }
        List<Enrollment> ordered = EnrollmentWriteOrder.sorted(enrollments);
        transactions.inTransaction(connection -> {
            mergeEnrollments(connection, userId, ordered);
            mergeModules(connection, userId, ordered);
            return null;
        });
    }

    @Override
    public List<Enrollment> findAll(UUID userId) {
        return transactions.inSnapshot(connection -> EnrollmentRows.assemble(
                query(connection, SELECT_ENROLLMENTS, userId, EnrollmentRows::enrollment),
                query(connection, SELECT_MODULES, userId, EnrollmentRows::module)));
    }

    private static void mergeEnrollments(Connection connection, UUID userId, List<Enrollment> ordered)
            throws SQLException {
        try (PreparedStatement merge = connection.prepareStatement(MERGE_ENROLLMENT)) {
            for (Enrollment enrollment : ordered) {
                merge.setObject(1, userId);
                merge.setObject(2, enrollment.courseId());
                merge.setObject(3, toColumn(enrollment.enrolledAt()));
                merge.addBatch();
            }
            merge.executeBatch();
        }
    }

    private static void mergeModules(Connection connection, UUID userId, List<Enrollment> ordered)
            throws SQLException {
        try (PreparedStatement merge = connection.prepareStatement(MERGE_MODULE)) {
            for (Enrollment enrollment : ordered) {
                for (MasteredModule module : enrollment.mastered()) {
                    merge.setObject(1, userId);
                    merge.setObject(2, enrollment.courseId());
                    merge.setString(3, module.moduleId());
                    merge.setObject(4, toColumn(module.masteredAt()));
                    merge.addBatch();
                }
            }
            merge.executeBatch(); // Con el lote vacío no hace nada.
        }
    }

    /** Una fila de la consulta → un valor; {@code RowMapper} puede lanzar {@link SQLException}. */
    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet row) throws SQLException;
    }

    private static <T> List<T> query(Connection connection, String sql, UUID userId, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setObject(1, userId);
            try (ResultSet rows = select.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(mapper.map(rows));
                }
                return result;
            }
        }
    }
}
