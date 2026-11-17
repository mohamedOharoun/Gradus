package es.ulpgc.pamn.dominio.backend;

import es.ulpgc.pamn.dominio.backend.application.Authenticate;
import es.ulpgc.pamn.dominio.backend.application.LogIn;
import es.ulpgc.pamn.dominio.backend.application.LogOut;
import es.ulpgc.pamn.dominio.backend.application.PasswordHasher;
import es.ulpgc.pamn.dominio.backend.application.RegisterUser;
import es.ulpgc.pamn.dominio.backend.application.SessionRepository;
import es.ulpgc.pamn.dominio.backend.application.UserRepository;
import es.ulpgc.pamn.dominio.backend.config.DatabaseConfig;
import es.ulpgc.pamn.dominio.backend.config.HttpConfig;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcSessionRepository;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcUserRepository;
import es.ulpgc.pamn.dominio.backend.infrastructure.security.Pbkdf2PasswordHasher;
import com.fasterxml.jackson.core.JacksonException;
import es.ulpgc.pamn.dominio.backend.web.ExceptionHandlers;
import es.ulpgc.pamn.dominio.backend.web.RequireSession;
import es.ulpgc.pamn.dominio.backend.web.SessionController;
import es.ulpgc.pamn.dominio.backend.web.UserController;
import io.javalin.Javalin;

import javax.sql.DataSource;
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
        DataSource dataSource = DatabaseConfig.fromEnvironment(env).dataSource();

        // Una sola instancia de cada pieza compartida: el hasher calcula su hash señuelo al crearse.
        UserRepository userRepository = new JdbcUserRepository(dataSource);
        SessionRepository sessionRepository = new JdbcSessionRepository(dataSource);
        PasswordHasher hasher = Pbkdf2PasswordHasher.withDefaults();
        Clock clock = Clock.systemUTC();

        UserController users = new UserController(
                new RegisterUser(userRepository, hasher, clock, UUID::randomUUID));
        SessionController sessions = new SessionController(
                new LogIn(userRepository, sessionRepository, hasher, SessionPolicy.STANDARD, clock,
                        UUID::randomUUID, SessionToken::generate),
                new LogOut(sessionRepository));
        RequireSession requireSession = new RequireSession(
                new Authenticate(sessionRepository, userRepository, SessionPolicy.STANDARD, clock));

        Javalin.create(config -> {
            config.jetty.port = http.port();
            config.routes.post("/api/users", users::register);
            config.routes.post("/api/sessions", sessions::logIn);
            // Rutas protegidas: sin un token válido responden 401 sin llegar al controlador.
            config.routes.get("/api/users/me", requireSession.around(users::me));
            config.routes.delete("/api/sessions/current", requireSession.around(sessions::logOut));
            config.routes.exception(JacksonException.class, ExceptionHandlers::invalidJson);
        }).start();
    }
}
