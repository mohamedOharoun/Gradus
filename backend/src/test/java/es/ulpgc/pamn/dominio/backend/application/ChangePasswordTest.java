package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static es.ulpgc.pamn.dominio.backend.application.TestUsers.ANA;
import static es.ulpgc.pamn.dominio.backend.application.TestUsers.ANA_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Cambiar la contraseña desde Ajustes (docs/tareas/ajustes.md, fase 2, tarea 2, decisión G). */
class ChangePasswordTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final String NEW_PASSWORD = "Nueva2026?";

    private static final Session PHONE = sessionOf(ANA.id(), "token-del-movil");
    private static final Session TABLET = sessionOf(ANA.id(), "token-de-la-tablet");
    private static final Session LUIS_PHONE = sessionOf(UUID.fromString("00000000-0000-0000-0000-000000000002"), "token-de-luis");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final ChangePassword changePassword = new ChangePassword(users, sessions, hasher);

    ChangePasswordTest() {
        users.add(ANA);
        sessions.add(PHONE);
        sessions.add(TABLET);
        sessions.add(LUIS_PHONE);
    }

    private static Session sessionOf(UUID userId, String token) {
        return SessionPolicy.STANDARD.start(UUID.randomUUID(), userId, new SessionToken(token).hash(), NOW);
    }

    /** Ana cambia la contraseña desde el móvil. */
    private ChangePasswordResult change(String current, String next) {
        return changePassword.change(ANA, PHONE, new ChangePasswordCommand(current, next));
    }

    private User storedAna() {
        return users.findById(ANA.id()).orElseThrow();
    }

    // ── Bien ──

    @Test
    void storesTheHashOfTheNewPassword() {
        assertInstanceOf(ChangePasswordResult.Changed.class, change(ANA_PASSWORD, NEW_PASSWORD));

        assertEquals(FakePasswordHasher.hashOf(NEW_PASSWORD), storedAna().passwordHash());
    }

    @Test
    void onlyThePasswordChanges() {
        change(ANA_PASSWORD, NEW_PASSWORD);

        assertEquals(ANA.withPasswordHash(FakePasswordHasher.hashOf(NEW_PASSWORD)), storedAna());
    }

    // Decisión G: si alguien tenía la contraseña, deja de tener la sesión abierta en otro dispositivo.
    @Test
    void closesTheOtherSessionsButKeepsThisOne() {
        change(ANA_PASSWORD, NEW_PASSWORD);

        assertEquals(Set.of(PHONE, LUIS_PHONE), Set.copyOf(sessions.all()));
    }

    @Test
    void sessionsOfOtherUsersStayOpen() {
        change(ANA_PASSWORD, NEW_PASSWORD);

        assertEquals(List.of(LUIS_PHONE), sessions.all().stream().filter(s -> !s.userId().equals(ANA.id())).toList());
    }

    // ── La contraseña actual no es la suya ──

    @Test
    void aWrongCurrentPasswordChangesNothing() {
        assertInstanceOf(ChangePasswordResult.WrongPassword.class, change("NoEsLaMia1!", NEW_PASSWORD));

        assertEquals(ANA, storedAna());
        assertEquals(3, sessions.all().size());
        assertEquals(0, hasher.hashCalls());
    }

    // ── Datos no válidos (400): no se llega a comprobar la contraseña actual ──

    @Test
    void aWeakNewPasswordIsRejectedWithTheRulesOfTheRegistration() {
        var errors = rejected(change(ANA_PASSWORD, "abc"));

        assertEquals(Set.of("newPassword"), fields(errors));
        assertEquals(Set.of(ErrorCode.TOO_SHORT, ErrorCode.MISSING_DIGIT, ErrorCode.MISSING_SPECIAL_CHARACTER),
                errors.stream().map(ValidationError::code).collect(Collectors.toSet()));
        assertEquals(ANA, storedAna());
    }

    @Test
    void aMissingCurrentPasswordIsRequired() {
        var errors = rejected(change(null, NEW_PASSWORD));

        assertEquals(List.of(new ValidationError("currentPassword", ErrorCode.REQUIRED, "Es obligatorio.")), errors);
    }

    @Test
    void reportsBothFieldsAtOnce() {
        assertEquals(Set.of("currentPassword", "newPassword"), fields(rejected(change("", ""))));
    }

    @Test
    void whenRejectedItNeitherChecksNorHashesNorClosesSessions() {
        change(ANA_PASSWORD, "abc");

        assertEquals(0, hasher.matchesCalls());
        assertEquals(0, hasher.hashCalls());
        assertEquals(3, sessions.all().size());
    }

    // La contraseña actual se compara tal cual (como al iniciar sesión): sin quitar espacios.
    @Test
    void theCurrentPasswordIsComparedAsTyped() {
        assertInstanceOf(ChangePasswordResult.WrongPassword.class, change(" " + ANA_PASSWORD + " ", NEW_PASSWORD));
    }

    @Test
    void theCommandNeverRevealsThePasswords() {
        String text = new ChangePasswordCommand(ANA_PASSWORD, NEW_PASSWORD).toString();

        assertFalse(text.contains(ANA_PASSWORD));
        assertFalse(text.contains(NEW_PASSWORD));
    }

    private static List<ValidationError> rejected(ChangePasswordResult result) {
        return assertInstanceOf(ChangePasswordResult.Rejected.class, result).errors();
    }

    private static Set<String> fields(List<ValidationError> errors) {
        return errors.stream().map(ValidationError::field).collect(Collectors.toSet());
    }
}
