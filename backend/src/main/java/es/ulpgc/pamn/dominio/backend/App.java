package es.ulpgc.pamn.dominio.backend;

import es.ulpgc.pamn.dominio.backend.application.RegisterUser;
import es.ulpgc.pamn.dominio.backend.config.DatabaseConfig;
import es.ulpgc.pamn.dominio.backend.config.HttpConfig;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcUserRepository;
import es.ulpgc.pamn.dominio.backend.infrastructure.security.Pbkdf2PasswordHasher;
import com.fasterxml.jackson.core.JacksonException;
import es.ulpgc.pamn.dominio.backend.web.ExceptionHandlers;
import es.ulpgc.pamn.dominio.backend.web.UserController;
import io.javalin.Javalin;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/** Composition root: el único sitio que conoce las implementaciones concretas y las conecta. */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        HttpConfig http = HttpConfig.fromEnvironment(env);
        UserController users = new UserController(registerUser(DatabaseConfig.fromEnvironment(env)));

        Javalin.create(config -> {
            config.jetty.port = http.port();
            config.routes.post("/api/users", users::register);
            config.routes.exception(JacksonException.class, ExceptionHandlers::invalidJson);
        }).start();
    }

    private static RegisterUser registerUser(DatabaseConfig database) {
        return new RegisterUser(
                new JdbcUserRepository(database.dataSource()),
                Pbkdf2PasswordHasher.withDefaults(),
                Clock.systemUTC(),
                UUID::randomUUID);
    }
}
