package es.ulpgc.pamn.dominio.backend.config;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;

/** Datos de conexión a PostgreSQL, leídos de las mismas variables GRADUS_DB_* que usa docker-compose. */
public record DatabaseConfig(String jdbcUrl, String user, String password) {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5432;

    public static DatabaseConfig fromEnvironment(Map<String, String> env) {
        String host = Environment.optional(env, "GRADUS_DB_HOST", DEFAULT_HOST);
        int port = Environment.port(env, "GRADUS_DB_PORT", DEFAULT_PORT);
        String name = Environment.required(env, "GRADUS_DB_NAME");
        return new DatabaseConfig(
                "jdbc:postgresql://" + host + ":" + port + "/" + name,
                Environment.required(env, "GRADUS_DB_USER"),
                Environment.required(env, "GRADUS_DB_PASSWORD"));
    }

    public DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    /** Evita que la contraseña de la base de datos acabe en los logs. */
    @Override
    public String toString() {
        return "DatabaseConfig[jdbcUrl=" + jdbcUrl + ", user=" + user + ", password=****]";
    }
}
