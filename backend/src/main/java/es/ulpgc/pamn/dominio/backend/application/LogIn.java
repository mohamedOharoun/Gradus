package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.LoginPassword;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.domain.validation.Result;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Caso de uso: iniciar sesión con correo y contraseña. */
public final class LogIn {

    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordHasher hasher;
    private final SessionPolicy policy;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;
    private final Supplier<SessionToken> tokenGenerator;

    public LogIn(UserRepository users, SessionRepository sessions, PasswordHasher hasher, SessionPolicy policy,
                 Clock clock, Supplier<UUID> idGenerator, Supplier<SessionToken> tokenGenerator) {
        this.users = Objects.requireNonNull(users, "users");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator");
    }

    public LogInResult logIn(LogInCommand command) {
        return Result.combine(Email.of(command.email()), LoginPassword.of(command.password()), Credentials::new)
                .fold(LogInResult.Rejected::new, this::authenticate);
    }

    private LogInResult authenticate(Credentials credentials) {
        Optional<User> user = users.findByEmail(credentials.email());
        // Se compara siempre, también si el correo no existe (contra el señuelo): así ambos casos
        // tardan lo mismo y dan la misma respuesta.
        boolean passwordMatches = hasher.matches(credentials.password(),
                user.map(User::passwordHash).orElseGet(hasher::decoyHash));
        return user.filter(found -> passwordMatches)
                .<LogInResult>map(this::startSession)
                .orElseGet(LogInResult.InvalidCredentials::new);
    }

    private LogInResult.LoggedIn startSession(User user) {
        // PostgreSQL guarda microsegundos: truncamos para que lo devuelto coincida con lo guardado.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        SessionToken token = tokenGenerator.get();
        Session session = policy.start(idGenerator.get(), user.id(), token.hash(), now);
        sessions.add(session);
        return new LogInResult.LoggedIn(user, token, session);
    }

    /** Correo y contraseña ya validados. Solo existe dentro de este caso de uso. */
    private record Credentials(Email email, LoginPassword password) {
    }
}
