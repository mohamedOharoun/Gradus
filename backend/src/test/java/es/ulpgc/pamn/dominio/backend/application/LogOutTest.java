package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.TestUsers.ANA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LogOutTest {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");
    private static final Session PHONE = sessionOf(new SessionToken("token-del-movil"));
    private static final Session TABLET = sessionOf(new SessionToken("token-de-la-tablet"));

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final LogOut logOut = new LogOut(sessions);

    private static Session sessionOf(SessionToken token) {
        return SessionPolicy.STANDARD.start(UUID.randomUUID(), ANA.id(), token.hash(), NOW);
    }

    @Test
    void closesOnlyTheSessionOfThatDevice() {
        sessions.add(PHONE);
        sessions.add(TABLET);

        logOut.logOut(PHONE);

        assertEquals(List.of(TABLET), sessions.all());
    }

    @Test
    void closingASessionTwiceIsHarmless() {
        sessions.add(PHONE);

        logOut.logOut(PHONE);

        assertDoesNotThrow(() -> logOut.logOut(PHONE));
    }
}
