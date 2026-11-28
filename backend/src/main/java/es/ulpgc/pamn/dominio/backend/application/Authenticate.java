package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: averiguar quién hace una petición a partir de su token. Cada uso correcto renueva
 * la sesión (caducidad deslizante); una sesión caducada se borra.
 */
public final class Authenticate {

    private final SessionRepository sessions;
    private final UserRepository users;
    private final SessionPolicy policy;
    private final Clock clock;

    public Authenticate(SessionRepository sessions, UserRepository users, SessionPolicy policy, Clock clock) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.users = Objects.requireNonNull(users, "users");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AuthenticationResult authenticate(SessionToken token) {
        // PostgreSQL guarda microsegundos: truncamos para que lo devuelto coincida con lo guardado.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        return sessions.findByTokenHash(token.hash())
                .flatMap(session -> activeAt(session, now))
                .flatMap(session -> users.findById(session.userId())
                        .<AuthenticationResult>map(user -> new AuthenticationResult.Authenticated(user, renew(session, now))))
                .orElseGet(AuthenticationResult.Unauthenticated::new);
    }

    /** La sesión si sigue activa; si ha caducado, la borra para que no se acumulen. */
    private Optional<Session> activeAt(Session session, Instant now) {
        if (session.isActiveAt(now)) {
            return Optional.of(session);
        }
        sessions.remove(session.tokenHash());
        return Optional.empty();
    }

    private Session renew(Session session, Instant now) {
        Session renewed = policy.renew(session, now);
        sessions.update(renewed);
        return renewed;
    }
}
