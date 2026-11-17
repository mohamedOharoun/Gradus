package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.application.SessionRepository;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionTokenHash;

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

/** {@link SessionRepository} sobre PostgreSQL con JDBC (tabla {@code sessions}, ver {@code 003_create_sessions.sql}). */
public final class JdbcSessionRepository implements SessionRepository {

    private static final String INSERT = """
            INSERT INTO sessions (id, user_id, token_hash, created_at, last_used_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT_BY_TOKEN_HASH = """
            SELECT id, user_id, token_hash, created_at, last_used_at, expires_at
            FROM sessions
            WHERE token_hash = ?
            """;
    // Solo cambia lo que cambia al renovar; el resto de la sesión es inmutable.
    private static final String UPDATE_USAGE = "UPDATE sessions SET last_used_at = ?, expires_at = ? WHERE id = ?";
    private static final String DELETE_BY_TOKEN_HASH = "DELETE FROM sessions WHERE token_hash = ?";

    private final DataSource dataSource;

    public JdbcSessionRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void add(Session session) {
        execute(INSERT, session.id(), session.userId(), session.tokenHash().value(),
                toColumn(session.createdAt()), toColumn(session.lastUsedAt()), toColumn(session.expiresAt()));
    }

    @Override
    public Optional<Session> findByTokenHash(SessionTokenHash tokenHash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(SELECT_BY_TOKEN_HASH)) {
            select.setString(1, tokenHash.value());
            try (ResultSet row = select.executeQuery()) {
                return row.next() ? Optional.of(toSession(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo buscar la sesión", e);
        }
    }

    @Override
    public void update(Session session) {
        execute(UPDATE_USAGE, toColumn(session.lastUsedAt()), toColumn(session.expiresAt()), session.id());
    }

    @Override
    public void remove(SessionTokenHash tokenHash) {
        execute(DELETE_BY_TOKEN_HASH, tokenHash.value());
    }

    /** Ejecuta una sentencia que modifica datos; que afecte a 0 filas no es un error (ver el puerto). */
    private void execute(String sql, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            // Fallo inesperado (base de datos caída, etc.): no es un caso de negocio, así que se propaga.
            throw new IllegalStateException("No se pudo modificar la tabla de sesiones", e);
        }
    }

    private static Session toSession(ResultSet row) throws SQLException {
        return new Session(
                row.getObject("id", UUID.class),
                row.getObject("user_id", UUID.class),
                new SessionTokenHash(row.getString("token_hash")),
                fromColumn(row, "created_at"),
                fromColumn(row, "last_used_at"),
                fromColumn(row, "expires_at"));
    }
}
