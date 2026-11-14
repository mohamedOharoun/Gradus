package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;
import es.ulpgc.pamn.dominio.backend.domain.TermsAcceptance;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;
import es.ulpgc.pamn.dominio.backend.domain.validation.Result;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Caso de uso: dar de alta un usuario nuevo. */
public final class RegisterUser {

    private final UserRepository repository;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public RegisterUser(UserRepository repository, PasswordHasher hasher, Clock clock, Supplier<UUID> idGenerator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public RegistrationResult register(RegisterUserCommand command) {
        // El reloj se lee una sola vez: el alta y la aceptación de los términos ocurren en el mismo momento.
        // PostgreSQL guarda microsegundos: truncamos para que lo devuelto coincida con lo guardado.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        return Result.combine(
                        UserName.of(command.name()),
                        Email.of(command.email()),
                        RawPassword.of(command.password()),
                        TermsAcceptance.of(command.acceptedTerms(), now),
                        (name, email, password, terms) -> newUser(name, email, password, terms, now))
                .fold(RegistrationResult.Rejected::new, this::store);
    }

    /** Solo se llama si todos los datos son válidos, así que nunca se hashea una contraseña inválida. */
    private User newUser(UserName name, Email email, RawPassword password, TermsAcceptance terms, Instant now) {
        return new User(idGenerator.get(), name, email, hasher.hash(password), now, terms);
    }

    private RegistrationResult store(User user) {
        return repository.add(user)
                ? new RegistrationResult.Registered(user)
                : new RegistrationResult.EmailAlreadyRegistered(user.email());
    }
}
