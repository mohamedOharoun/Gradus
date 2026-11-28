package es.ulpgc.pamn.dominio.backend.domain.session;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static es.ulpgc.pamn.dominio.backend.domain.session.SessionTest.HASH;
import static es.ulpgc.pamn.dominio.backend.domain.session.SessionTest.ID;
import static es.ulpgc.pamn.dominio.backend.domain.session.SessionTest.START;
import static es.ulpgc.pamn.dominio.backend.domain.session.SessionTest.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionPolicyTest {

    private final SessionPolicy policy = SessionPolicy.STANDARD;
    private final Session started = policy.start(ID, USER_ID, HASH, START);

    private static Instant daysAfterStart(int days) {
        return START.plus(Duration.ofDays(days));
    }

    @Test
    void theStandardPolicyIs30DaysSlidingWithA90DayCap() {
        assertEquals(Duration.ofDays(30), policy.idleTimeout());
        assertEquals(Duration.ofDays(90), policy.maxLifetime());
    }

    @Test
    void aNewSessionExpires30DaysAfterLoggingIn() {
        assertEquals(new Session(ID, USER_ID, HASH, START, START, daysAfterStart(30)), started);
    }

    @Test
    void usingTheSessionMovesItsExpiryForward() {
        Session renewed = policy.renew(started, daysAfterStart(10));

        assertEquals(daysAfterStart(10), renewed.lastUsedAt());
        assertEquals(daysAfterStart(40), renewed.expiresAt());
    }

    @Test
    void renewingKeepsEverythingElse() {
        Session renewed = policy.renew(started, daysAfterStart(10));

        assertEquals(started.id(), renewed.id());
        assertEquals(started.userId(), renewed.userId());
        assertEquals(started.tokenHash(), renewed.tokenHash());
        assertEquals(started.createdAt(), renewed.createdAt());
    }

    @Test
    void neverExpiresLaterThan90DaysAfterLoggingIn() {
        Session session = started;
        for (int day = 25; day <= 75; day += 25) { // usada los días 25, 50 y 75
            session = policy.renew(session, daysAfterStart(day));
        }

        assertEquals(daysAfterStart(90), session.expiresAt());
    }

    @Test
    void doesNotRenewAnExpiredSession() {
        assertThrows(IllegalStateException.class, () -> policy.renew(started, daysAfterStart(30)));
    }

    @Test
    void doesNotGoBackInTimeIfTheClockDoes() {
        Session renewed = policy.renew(started, daysAfterStart(10));

        assertEquals(renewed, policy.renew(renewed, daysAfterStart(9)));
    }

    @Test
    void rejectsNonPositiveDurations() {
        assertThrows(IllegalArgumentException.class, () -> new SessionPolicy(Duration.ZERO, Duration.ofDays(90)));
        assertThrows(IllegalArgumentException.class, () -> new SessionPolicy(Duration.ofDays(30), Duration.ofDays(-1)));
    }

    @Test
    void rejectsAnIdleTimeoutLongerThanTheCap() {
        assertThrows(IllegalArgumentException.class, () -> new SessionPolicy(Duration.ofDays(91), Duration.ofDays(90)));
    }
}
