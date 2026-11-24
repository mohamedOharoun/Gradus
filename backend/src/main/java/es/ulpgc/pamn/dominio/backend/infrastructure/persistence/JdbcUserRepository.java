package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.application.UserRepository;
import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.TermsAcceptance;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.fromColumn;
import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.toColumn;

/** {@link UserRepository} sobre PostgreSQL con JDBC. */
public final class JdbcUserRepository implements UserRepository {

    // ON CONFLICT (email) DO NOTHING: si el correo ya existe no inserta nada (0 filas) y no lanza error.
    // Comprobar e insertar ocurre en una sola sentencia atómica, sin carreras entre peticiones.
    private static final String INSERT_IF_EMAIL_IS_FREE = """
            INSERT INTO users (id, name, email, password_hash, created_at, terms_accepted_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (email) DO NOTHING
            """;

    // El correo, la fecha de alta y la de los términos no cambian nunca: solo se actualiza lo demás.
    private static final String UPDATE = """
            UPDATE users SET name = ?, password_hash = ?
            WHERE id = ?
            """;

    // Una sola sentencia: no hay hueco entre «¿es autor?» y «bórralo» en el que se cuele una publicación.
    // Sus sesiones se borran solas (sessions.user_id tiene ON DELETE CASCADE).
    private static final String DELETE_UNLESS_AUTHOR = """
            DELETE FROM users
            WHERE id = ? AND NOT EXISTS (SELECT 1 FROM courses WHERE courses.author_id = users.id)
            """;

    private static final String SELECT_USER = """
            SELECT id, name, email, password_hash, created_at, terms_accepted_at
            FROM users
            """;
    private static final String SELECT_BY_EMAIL = SELECT_USER + "WHERE email = ?";
    private static final String SELECT_BY_ID = SELECT_USER + "WHERE id = ?";

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
            insert.setObject(5, toColumn(user.createdAt()));
            insert.setObject(6, toColumn(user.termsAcceptance().acceptedAt()));
            return insert.executeUpdate() == 1;
        } catch (SQLException e) {
            // Fallo inesperado (base de datos caída, etc.): no es un caso de negocio, así que se propaga.
            throw new IllegalStateException("No se pudo guardar el usuario " + user.id(), e);
        }
    }

    @Override
    public void update(User user) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(UPDATE)) {
            update.setString(1, user.name().value());
            update.setString(2, user.passwordHash().value());
            update.setObject(3, user.id());
            update.executeUpdate(); // 0 filas si ya no existe: no es un error.
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el usuario " + user.id(), e);
        }
    }

    @Override
    public boolean removeUnlessAuthor(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement delete = connection.prepareStatement(DELETE_UNLESS_AUTHOR)) {
            delete.setObject(1, id);
            return delete.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo borrar el usuario " + id, e);
        }
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return findOne(SELECT_BY_EMAIL, email.value());
    }

    @Override
    public Optional<User> findById(UUID id) {
        return findOne(SELECT_BY_ID, id);
    }

    /** Ejecuta una consulta que devuelve como mucho un usuario (correo e id son únicos). */
    private Optional<User> findOne(String sql, Object key) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(sql)) {
            select.setObject(1, key);
            try (ResultSet row = select.executeQuery()) {
                return row.next() ? Optional.of(toUser(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo buscar el usuario", e);
        }
    }

    /** Convierte la fila actual en un {@link User}; los constructores vuelven a comprobar los datos. */
    private static User toUser(ResultSet row) throws SQLException {
        return new User(
                row.getObject("id", UUID.class),
                new UserName(row.getString("name")),
                new Email(row.getString("email")),
                new PasswordHash(row.getString("password_hash")),
                fromColumn(row, "created_at"),
                new TermsAcceptance(fromColumn(row, "terms_accepted_at")));
    }
}
