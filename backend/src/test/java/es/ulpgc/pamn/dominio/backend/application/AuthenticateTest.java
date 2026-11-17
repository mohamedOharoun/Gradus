package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.TestUsers.ANA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticateTest {

    private static final SessionPolicy POLICY = SessionPolicy.STANDARD;
    private static final Instant LOGGED_IN_AT = Instant.parse("2026-11-17T10:00:00Z");
    private static final SessionToken TOKEN = new SessionToken("token-de-ana");
    private static final Session ANA_SESSION = POLICY.start(
            UUID.fromString("00000000-0000-0000-0000-00000000000a"), ANA.id(), TOKEN.hash(), LOGGED_IN_AT);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemorySessionRepository sessions = new InMemorySessionRepository();

    AuthenticateTest() {
        users.add(ANA);
        sessions.add(ANA_SESSION);
    }

    private static Instant daysAfterLogin(int days) {
        return LOGGED_IN_AT.plus(Duration.ofDays(days));
    }

    private AuthenticationResult authenticateAt(Instant now, SessionToken token) {
        return new Authenticate(sessions, users, POLICY, Clock.fixed(now, ZoneOffset.UTC)).authenticate(token);
    }

    @Test
    void recognisesTheUserOfAnActiveSession() {
        var authenticated = assertInstanceOf(AuthenticationResult.Authenticated.class,
                authenticateAt(daysAfterLogin(10), TOKEN));

        assertEquals(ANA, authenticated.user());
    }

    @Test
    void eachUseMovesTheExpiryForwardAndStoresIt() {
        var authenticated = assertInstanceOf(AuthenticationResult.Authenticated.class,
                authenticateAt(daysAfterLogin(10), TOKEN));

        Session renewed = POLICY.renew(ANA_SESSION, daysAfterLogin(10));
        assertEquals(renewed, authenticated.session());
        assertEquals(daysAfterLogin(40), renewed.expiresAt());
        assertEquals(List.of(renewed), sessions.all());
    }

    @Test
    void canKeepUsingItForDaysAsLongAsItIsUsedEvery30() {
        for (int day = 29; day <= 87; day += 29) { // días 29, 58 y 87
            assertInstanceOf(AuthenticationResult.Authenticated.class, authenticateAt(daysAfterLogin(day), TOKEN));
        }
    }

    @Test
    void stopsWorking90DaysAfterLoggingInEvenIfUsedEveryDay() {
        for (int day = 1; day < 90; day++) {
            assertInstanceOf(AuthenticationResult.Authenticated.class, authenticateAt(daysAfterLogin(day), TOKEN));
        }

        assertInstanceOf(AuthenticationResult.Unauthenticated.class, authenticateAt(daysAfterLogin(90), TOKEN));
    }

    @Test
    void rejectsASessionUnused30DaysAndDeletesIt() {
        assertInstanceOf(AuthenticationResult.Unauthenticated.class, authenticateAt(daysAfterLogin(30), TOKEN));
        assertTrue(sessions.all().isEmpty());
    }

    @Test
    void rejectsAnUnknownToken() {
        var result = authenticateAt(daysAfterLogin(1), new SessionToken("token-inventado"));

        assertInstanceOf(AuthenticationResult.Unauthenticated.class, result);
        assertEquals(List.of(ANA_SESSION), sessions.all());
    }

    @Test
    void rejectsASessionWhoseUserNoLongerExists() {
        SessionToken orphanToken = new SessionToken("token-huerfano");
        sessions.add(POLICY.start(UUID.randomUUID(), UUID.randomUUID(), orphanToken.hash(), LOGGED_IN_AT));

        assertInstanceOf(AuthenticationResult.Unauthenticated.class, authenticateAt(daysAfterLogin(1), orphanToken));
    }

    @Test
    void storesTheLastUseWithTheMicrosecondPrecisionOfPostgres() {
        Instant withNanos = Instant.parse("2026-11-18T10:00:00.123456789Z");

        var authenticated = assertInstanceOf(AuthenticationResult.Authenticated.class, authenticateAt(withNanos, TOKEN));

        assertEquals(Instant.parse("2026-11-18T10:00:00.123456Z"), authenticated.session().lastUsedAt());
    }
}
