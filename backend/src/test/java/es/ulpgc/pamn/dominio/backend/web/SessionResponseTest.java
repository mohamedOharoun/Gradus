package es.ulpgc.pamn.dominio.backend.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.application.LogInResult;
import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.TermsAcceptance;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SessionResponseTest {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");
    private static final SessionToken TOKEN = new SessionToken("token-de-prueba");
    private static final User ANA = new User(
            UUID.fromString("00000000-0000-0000-0000-000000000001"), new UserName("Ana"), new Email("ana@ulpgc.es"),
            new PasswordHash("pbkdf2_sha256$1$c2Fs$aGFzaA=="), NOW, new TermsAcceptance(NOW));
    private static final LogInResult.LoggedIn LOGGED_IN = new LogInResult.LoggedIn(ANA, TOKEN,
            SessionPolicy.STANDARD.start(UUID.randomUUID(), ANA.id(), TOKEN.hash(), NOW));

    @Test
    void sendsTheTokenItsExpiryAndTheUser() throws JsonProcessingException {
        // Es el contrato con la app: si cambia, hay que cambiar también la app.
        assertEquals("""
                {"token":"token-de-prueba","expiresAt":"2026-12-17T10:00:00Z",\
                "user":{"id":"00000000-0000-0000-0000-000000000001","name":"Ana",\
                "email":"ana@ulpgc.es","createdAt":"2026-11-17T10:00:00Z"}}""",
                new ObjectMapper().writeValueAsString(SessionResponse.from(LOGGED_IN)));
    }

    @Test
    void neverSendsThePasswordHashNorTheTokenHash() throws JsonProcessingException {
        String json = new ObjectMapper().writeValueAsString(SessionResponse.from(LOGGED_IN));

        assertFalse(json.contains(ANA.passwordHash().value()));
        assertFalse(json.contains(TOKEN.hash().value()));
    }

    @Test
    void toStringNeverRevealsTheToken() {
        assertFalse(SessionResponse.from(LOGGED_IN).toString().contains(TOKEN.value()));
    }
}
