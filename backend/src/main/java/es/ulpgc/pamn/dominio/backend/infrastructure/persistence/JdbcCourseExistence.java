package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.application.progress.CourseExistence;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** {@link CourseExistence} sobre PostgreSQL con JDBC: una sola consulta por petición. */
public final class JdbcCourseExistence implements CourseExistence {

    private static final String SELECT_EXISTING = "SELECT id FROM courses WHERE id = ANY(?)";

    private final DataSource dataSource;

    public JdbcCourseExistence(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Set<UUID> findExisting(Set<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return Set.of(); // Respuesta ya conocida: no hace falta ir a la base de datos.
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(SELECT_EXISTING)) {
            select.setArray(1, connection.createArrayOf("uuid", courseIds.toArray()));
            try (ResultSet rows = select.executeQuery()) {
                Set<UUID> existing = new HashSet<>();
                while (rows.next()) {
                    existing.add(rows.getObject("id", UUID.class));
                }
                return Set.copyOf(existing);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo comprobar qué cursos existen", e);
        }
    }
}
