package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;
import es.ulpgc.pamn.dominio.backend.domain.validation.Result;

import java.time.Clock;
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
        return Result.combine(
                        UserName.of(command.name()),
                        Email.of(command.email()),
                        RawPassword.of(command.password()),
                        this::newUser)
                .fold(RegistrationResult.Rejected::new, this::store);
    }

    /** Solo se llama si los tres campos son válidos, así que nunca se hashea una contraseña inválida. */
    private User newUser(UserName name, Email email, RawPassword password) {
        // PostgreSQL guarda microsegundos: truncamos para que lo devuelto coincida con lo guardado.
        return new User(idGenerator.get(), name, email, hasher.hash(password),
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    private RegistrationResult store(User user) {
        return repository.add(user)
                ? new RegistrationResult.Registered(user)
                : new RegistrationResult.EmailAlreadyRegistered(user.email());
    }
}
