package es.ulpgc.pamn.dominio.backend.domain.session;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final SessionTokenHash HASH = new SessionToken("abc").hash();
    static final Instant START = Instant.parse("2026-11-17T10:00:00Z");
    static final Instant EXPIRY = START.plus(Duration.ofDays(30));

    private final Session session = new Session(ID, USER_ID, HASH, START, START, EXPIRY);

    @Test
    void isActiveUntilJustBeforeItExpires() {
        assertTrue(session.isActiveAt(START));
        assertTrue(session.isActiveAt(EXPIRY.minusNanos(1)));
    }

    @Test
    void isNoLongerActiveFromTheMomentItExpires() {
        assertFalse(session.isActiveAt(EXPIRY));
        assertFalse(session.isActiveAt(EXPIRY.plusSeconds(1)));
    }

    @Test
    void cannotBeUsedBeforeItWasCreated() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(ID, USER_ID, HASH, START, START.minusSeconds(1), EXPIRY));
    }

    @Test
    void mustExpireAfterItsLastUse() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(ID, USER_ID, HASH, START, START, START));
    }

    @Test
    void requiresEveryField() {
        assertThrows(NullPointerException.class, () -> new Session(null, USER_ID, HASH, START, START, EXPIRY));
        assertThrows(NullPointerException.class, () -> new Session(ID, null, HASH, START, START, EXPIRY));
        assertThrows(NullPointerException.class, () -> new Session(ID, USER_ID, null, START, START, EXPIRY));
        assertThrows(NullPointerException.class, () -> new Session(ID, USER_ID, HASH, null, START, EXPIRY));
        assertThrows(NullPointerException.class, () -> new Session(ID, USER_ID, HASH, START, null, EXPIRY));
        assertThrows(NullPointerException.class, () -> new Session(ID, USER_ID, HASH, START, START, null));
    }
}
