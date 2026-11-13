package es.ulpgc.pamn.dominio.backend.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UserName NAME = new UserName("Ana");
    private static final Email EMAIL = new Email("ana@ulpgc.es");
    private static final PasswordHash HASH = new PasswordHash("pbkdf2_sha256$1$c2Fs$aGFzaA==");
    private static final Instant CREATED_AT = Instant.parse("2026-11-13T10:00:00Z");

    @Test
    void requiresEveryField() {
        assertThrows(NullPointerException.class, () -> new User(null, NAME, EMAIL, HASH, CREATED_AT));
        assertThrows(NullPointerException.class, () -> new User(ID, null, EMAIL, HASH, CREATED_AT));
        assertThrows(NullPointerException.class, () -> new User(ID, NAME, null, HASH, CREATED_AT));
        assertThrows(NullPointerException.class, () -> new User(ID, NAME, EMAIL, null, CREATED_AT));
        assertThrows(NullPointerException.class, () -> new User(ID, NAME, EMAIL, HASH, null));
    }

    @Test
    void toStringNeverRevealsThePasswordHash() {
        User user = new User(ID, NAME, EMAIL, HASH, CREATED_AT);

        assertFalse(user.toString().contains(HASH.value()));
    }

    @Test
    void passwordHashCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("  "));
    }
}
