package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.application.UserRepository;
import es.ulpgc.pamn.dominio.backend.domain.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Objects;

/** {@link UserRepository} sobre PostgreSQL con JDBC. */
public final class JdbcUserRepository implements UserRepository {

    // ON CONFLICT (email) DO NOTHING: si el correo ya existe no inserta nada (0 filas) y no lanza error.
    // Comprobar e insertar ocurre en una sola sentencia atómica, sin carreras entre peticiones.
    private static final String INSERT_IF_EMAIL_IS_FREE = """
            INSERT INTO users (id, name, email, password_hash, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (email) DO NOTHING
            """;

    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public boolean add(User user) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(INSERT_IF_EMAIL_IS_FREE)) {
            insert.setObject(1, user.id());
            insert.setString(2, user.name().value());
            insert.setString(3, user.email().value());
            insert.setString(4, user.passwordHash().value());
            insert.setObject(5, user.createdAt().atOffset(ZoneOffset.UTC));
            return insert.executeUpdate() == 1;
        } catch (SQLException e) {
            // Fallo inesperado (base de datos caída, etc.): no es un caso de negocio, así que se propaga.
            throw new IllegalStateException("No se pudo guardar el usuario " + user.id(), e);
        }
    }
}
