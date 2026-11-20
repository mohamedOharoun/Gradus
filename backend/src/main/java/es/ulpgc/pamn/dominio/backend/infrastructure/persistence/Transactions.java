package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Execute Around para transacciones (§4.2): abre una conexión, desactiva el autocommit, ejecuta el trabajo
 * y hace commit; si algo falla, rollback. Así ningún método del repositorio repite ese código ni puede
 * olvidarse de deshacer.
 */
final class Transactions {

    /** Un trabajo con la conexión de la transacción. Como {@code Function}, pero puede lanzar {@link SQLException}. */
    @FunctionalInterface
    interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private final DataSource dataSource;

    Transactions(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    /**
     * Un {@link SQLException} (base de datos caída, restricción incumplida…) no es un caso de negocio: se
     * propaga como {@link IllegalStateException}, como en el resto de repositorios.
     */
    <T> T inTransaction(SqlWork<T> work) {
        return run(connection -> {
        }, work);
    }

    /**
     * Una transacción de solo lectura en la que todas las consultas ven la misma foto de la base de datos
     * ({@code REPEATABLE READ}). Por ejemplo, el total y la página de una búsqueda: si se publicara un curso
     * entre las dos consultas, la página podría tener más resultados que el total.
     */
    <T> T inSnapshot(SqlWork<T> work) {
        return run(connection -> {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setReadOnly(true);
        }, work);
    }

    /** Prepara la conexión antes de la primera sentencia de la transacción. */
    @FunctionalInterface
    private interface Setup {
        void apply(Connection connection) throws SQLException;
    }

    private <T> T run(Setup setup, SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            setup.apply(connection);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo completar la transacción", e);
        }
    }

    /** Si también falla el rollback, ese fallo se adjunta al original en vez de taparlo. */
    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
