package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.TermsAcceptance;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.codesFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogInTest {

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");
    private static final SessionToken TOKEN = new SessionToken("token-de-prueba");
    private static final String PASSWORD = TestUsers.ANA_PASSWORD;
    private static final User ANA = TestUsers.ANA;

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();

    LogInTest() {
        users.add(ANA);
    }

    private LogIn logInAt(Instant now) {
        return new LogIn(users, sessions, hasher, SessionPolicy.STANDARD,
                Clock.fixed(now, ZoneOffset.UTC), () -> SESSION_ID, () -> TOKEN);
    }

    private LogInResult logIn(String email, String password) {
        return logInAt(NOW).logIn(new LogInCommand(email, password));
    }

    @Test
    void startsASessionWithTheRightPassword() {
        var loggedIn = assertInstanceOf(LogInResult.LoggedIn.class, logIn("ana@ulpgc.es", PASSWORD));

        Session expected = SessionPolicy.STANDARD.start(SESSION_ID, ANA.id(), TOKEN.hash(), NOW);
        assertEquals(ANA, loggedIn.user());
        assertEquals(TOKEN, loggedIn.token());
        assertEquals(expected, loggedIn.session());
        assertEquals(List.of(expected), sessions.all());
    }

    @Test
    void theEmailIsNormalizedLikeInTheRegistration() {
        assertInstanceOf(LogInResult.LoggedIn.class, logIn("  ANA@ulpgc.es ", PASSWORD));
    }

    @Test
    void storesOnlyTheHashOfTheToken() {
        logIn("ana@ulpgc.es", PASSWORD);

        assertEquals(TOKEN.hash(), sessions.all().getFirst().tokenHash());
    }

    @Test
    void doesNotApplyTheRegistrationRulesToThePassword() {
        // Una cuenta antigua con una contraseña que hoy no pasaría las reglas sigue pudiendo entrar.
        User oldAccount = new User(UUID.randomUUID(), new UserName("Luis"), new Email("luis@ulpgc.es"),
                FakePasswordHasher.hashOf("abc"), NOW, new TermsAcceptance(NOW));
        users.add(oldAccount);

        assertInstanceOf(LogInResult.LoggedIn.class, logIn("luis@ulpgc.es", "abc"));
    }

    @Test
    void rejectsAWrongPassword() {
        assertInstanceOf(LogInResult.InvalidCredentials.class, logIn("ana@ulpgc.es", "Otra2026!!"));
        assertTrue(sessions.all().isEmpty());
    }

    @Test
    void anUnknownEmailGivesExactlyTheSameAnswerAsAWrongPassword() {
        LogInResult unknownEmail = logIn("nadie@ulpgc.es", PASSWORD);
        LogInResult wrongPassword = logIn("ana@ulpgc.es", "Otra2026!!");

        assertEquals(wrongPassword, unknownEmail);
        assertTrue(sessions.all().isEmpty());
    }

    @Test
    void comparesAPasswordEvenIfTheEmailDoesNotExist() {
        // Así tarda lo mismo que con un correo existente y el tiempo no delata qué correos hay.
        logIn("nadie@ulpgc.es", PASSWORD);

        assertEquals(1, hasher.matchesCalls());
    }

    @Test
    void rejectsMissingFieldsReportingBoth() {
        var rejected = assertInstanceOf(LogInResult.Rejected.class, logIn(null, ""));

        List<ValidationError> errors = rejected.errors();
        assertEquals(List.of(ErrorCode.REQUIRED), codesFor("email", errors));
        assertEquals(List.of(ErrorCode.REQUIRED), codesFor("password", errors));
        assertEquals(0, hasher.matchesCalls());
    }

    @Test
    void rejectsAMalformedEmailWithoutLookingForIt() {
        var rejected = assertInstanceOf(LogInResult.Rejected.class, logIn("no-es-un-correo", PASSWORD));

        assertEquals(List.of(ErrorCode.INVALID_EMAIL), codesFor("email", rejected.errors()));
        assertEquals(0, hasher.matchesCalls());
    }

    @Test
    void storesTheMomentWithTheMicrosecondPrecisionOfPostgres() {
        Instant withNanos = Instant.parse("2026-11-17T10:00:00.123456789Z");

        var loggedIn = assertInstanceOf(LogInResult.LoggedIn.class,
                logInAt(withNanos).logIn(new LogInCommand("ana@ulpgc.es", PASSWORD)));

        assertEquals(Instant.parse("2026-11-17T10:00:00.123456Z"), loggedIn.session().createdAt());
    }

    @Test
    void neitherTheCommandNorTheResultRevealSecrets() {
        String command = new LogInCommand("ana@ulpgc.es", PASSWORD).toString();
        String result = logIn("ana@ulpgc.es", PASSWORD).toString();

        assertFalse(command.contains(PASSWORD));
        assertFalse(result.contains(TOKEN.value()));
    }
}
