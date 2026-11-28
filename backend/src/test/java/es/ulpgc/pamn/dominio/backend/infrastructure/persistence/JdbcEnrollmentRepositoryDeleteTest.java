package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitarios de {@link JdbcEnrollmentRepository#delete(UUID, UUID)} sin base de datos, usando
 * {@link Proxy} sobre {@link DataSource}, {@link Connection} y {@link PreparedStatement} (como en
 * {@link TransactionsTest}).
 */
class JdbcEnrollmentRepositoryDeleteTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COURSE_ID = UUID.fromString("0b8f6d2e-0000-4000-8000-000000000001");

    @Test
    void deleteExecutesParameterizedDeleteInsideATransaction() {
        RecordingJdbc jdbc = new RecordingJdbc(null);
        JdbcEnrollmentRepository repository = new JdbcEnrollmentRepository(jdbc.dataSource());

        repository.delete(USER_ID, COURSE_ID);

        assertEquals("DELETE FROM enrollments WHERE user_id = ? AND course_id = ?", jdbc.normalizedSql());
        assertEquals(List.of(
                "setAutoCommit(false)",
                "prepareStatement",
                "setObject(1, " + USER_ID + ")",
                "setObject(2, " + COURSE_ID + ")",
                "executeUpdate",
                "statement.close",
                "commit",
                "close"), jdbc.calls());
    }

    @Test
    void sqlErrorDuringDeleteRollsBackClosesConnectionAndPropagatesIllegalStateException() {
        SQLException cause = new SQLException("se perdió la conexión");
        RecordingJdbc jdbc = new RecordingJdbc(cause);
        JdbcEnrollmentRepository repository = new JdbcEnrollmentRepository(jdbc.dataSource());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> repository.delete(USER_ID, COURSE_ID));

        assertSame(cause, thrown.getCause());
        assertEquals(List.of(
                "setAutoCommit(false)",
                "prepareStatement",
                "setObject(1, " + USER_ID + ")",
                "setObject(2, " + COURSE_ID + ")",
                "executeUpdate",
                "statement.close",
                "rollback",
                "close"), jdbc.calls());
    }

    @Test
    void rejectsNullUserIdWithoutTouchingTheDatabase() {
        JdbcEnrollmentRepository repository = new JdbcEnrollmentRepository(NoConnectionDataSource.create());

        assertThrows(NullPointerException.class, () -> repository.delete(null, COURSE_ID));
    }

    @Test
    void rejectsNullCourseIdWithoutTouchingTheDatabase() {
        JdbcEnrollmentRepository repository = new JdbcEnrollmentRepository(NoConnectionDataSource.create());

        assertThrows(NullPointerException.class, () -> repository.delete(USER_ID, null));
    }

    /** Doble JDBC mínimo en memoria que registra la secuencia exacta de llamadas y el SQL preparado. */
    private static final class RecordingJdbc {

        private final List<String> calls = new ArrayList<>();
        private final SQLException updateFailure;
        private String preparedSql;

        private RecordingJdbc(SQLException updateFailure) {
            this.updateFailure = updateFailure;
        }

        List<String> calls() {
            return List.copyOf(calls);
        }

        String normalizedSql() {
            return preparedSql == null ? "" : preparedSql.trim().replaceAll("\\s+", " ");
        }

        DataSource dataSource() {
            PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setObject" -> {
                            calls.add("setObject(" + args[0] + ", " + args[1] + ")");
                            yield null;
                        }
                        case "executeUpdate" -> {
                            calls.add("executeUpdate");
                            if (updateFailure != null) {
                                throw updateFailure;
                            }
                            yield 1;
                        }
                        case "close" -> {
                            calls.add("statement.close");
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });

            Connection connection = (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> {
                            preparedSql = (String) args[0];
                            calls.add("prepareStatement");
                            yield statement;
                        }
                        default -> {
                            calls.add(args == null ? method.getName() : method.getName() + "(" + args[0] + ")");
                            yield null;
                        }
                    });

            return (DataSource) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{DataSource.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getConnection")) {
                            return connection;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
