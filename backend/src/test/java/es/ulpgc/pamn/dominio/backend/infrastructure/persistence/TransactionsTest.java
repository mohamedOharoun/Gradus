package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionsTest {

    /** Lo que se ha hecho con la conexión, en orden: «setAutoCommit(false)», «commit», «close»… */
    private final List<String> calls = new ArrayList<>();

    /**
     * Una conexión de mentira hecha con {@link Proxy}: Java crea en tiempo de ejecución un objeto que
     * implementa {@link Connection} y manda cada llamada a esta lambda, que solo la anota.
     */
    private final Connection connection = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                calls.add(args == null ? method.getName() : method.getName() + "(" + args[0] + ")");
                return null;
            });

    private final DataSource dataSource = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                if (method.getName().equals("getConnection")) {
                    return connection;
                }
                throw new UnsupportedOperationException(method.getName());
            });

    private final Transactions transactions = new Transactions(dataSource);

    @Test
    void theWorkRunsOnOneConnectionAndIsCommitted() {
        String result = transactions.inTransaction(used -> {
            assertSame(connection, used);
            calls.add("trabajo");
            return "hecho";
        });

        assertEquals("hecho", result);
        assertEquals(List.of("setAutoCommit(false)", "trabajo", "commit", "close"), calls);
    }

    @Test
    void aSnapshotReadsEverythingFromTheSamePictureOfTheDatabase() {
        // REPEATABLE READ = 4 (Connection.TRANSACTION_REPEATABLE_READ): todas las consultas ven los mismos datos.
        transactions.inSnapshot(used -> {
            calls.add("lectura");
            return null;
        });

        assertEquals(List.of("setAutoCommit(false)", "setTransactionIsolation(4)", "setReadOnly(true)", "lectura",
                "commit", "close"), calls);
    }

    @Test
    void anSqlErrorRollsBackAndBecomesAnIllegalStateException() {
        SQLException cause = new SQLException("se cayó la base de datos");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> transactions.inTransaction(used -> {
                    throw cause;
                }));

        assertSame(cause, e.getCause());
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), calls);
    }

    @Test
    void anyOtherErrorAlsoRollsBackAndIsRethrownAsItIs() {
        IllegalArgumentException cause = new IllegalArgumentException("fallo nuestro");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> transactions.inTransaction(used -> {
                    throw cause;
                }));

        assertSame(cause, e);
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), calls);
    }
}
