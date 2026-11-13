package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterUserTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");
    private static final String VALID_PASSWORD = "Gradus2026!";

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final RegisterUser registerUser = registerUserAt(NOW);

    private RegisterUser registerUserAt(Instant now) {
        return new RegisterUser(repository, hasher, Clock.fixed(now, ZoneOffset.UTC), () -> ID);
    }

    @Test
    void registersAValidUser() {
        var result = registerUser.register(new RegisterUserCommand(" Ana ", "Ana@ULPGC.es", VALID_PASSWORD));

        User user = assertInstanceOf(RegistrationResult.Registered.class, result).user();
        assertEquals(ID, user.id());
        assertEquals("Ana", user.name().value());
        assertEquals("ana@ulpgc.es", user.email().value());
        assertEquals("fake$" + VALID_PASSWORD, user.passwordHash().value());
        assertEquals(NOW, user.createdAt());
        assertEquals(List.of(user), repository.all());
    }

    @Test
    void rejectsInvalidDataReportingEveryFieldAtOnce() {
        var result = registerUser.register(new RegisterUserCommand("A", "no-es-un-correo", "abc"));

        List<ValidationError> errors = assertInstanceOf(RegistrationResult.Rejected.class, result).errors();
        Set<String> fields = errors.stream().map(ValidationError::field).collect(Collectors.toSet());
        assertEquals(Set.of("name", "email", "password"), fields);
        assertEquals(5, errors.size()); // 1 del nombre + 1 del correo + 3 de la contraseña
    }

    @Test
    void doesNotHashNorStoreWhenDataIsInvalid() {
        registerUser.register(new RegisterUserCommand(null, null, null));

        assertEquals(0, hasher.hashCalls());
        assertTrue(repository.all().isEmpty());
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegisteredIgnoringCase() {
        registerUser.register(new RegisterUserCommand("Ana", "ana@ulpgc.es", VALID_PASSWORD));

        var result = registerUser.register(new RegisterUserCommand("Otra Ana", "ANA@ulpgc.es", VALID_PASSWORD));

        var taken = assertInstanceOf(RegistrationResult.EmailAlreadyRegistered.class, result);
        assertEquals("ana@ulpgc.es", taken.email().value());
        assertEquals(1, repository.all().size());
        assertEquals("Ana", repository.all().getFirst().name().value());
    }

    @Test
    void storesCreationTimeWithTheMicrosecondPrecisionOfPostgres() {
        Instant withNanos = Instant.parse("2026-11-13T10:00:00.123456789Z");

        var result = registerUserAt(withNanos).register(new RegisterUserCommand("Ana", "ana@ulpgc.es", VALID_PASSWORD));

        User user = assertInstanceOf(RegistrationResult.Registered.class, result).user();
        assertEquals(Instant.parse("2026-11-13T10:00:00.123456Z"), user.createdAt());
    }

    @Test
    void commandToStringNeverRevealsThePassword() {
        assertFalse(new RegisterUserCommand("Ana", "ana@ulpgc.es", VALID_PASSWORD).toString().contains(VALID_PASSWORD));
    }
}
