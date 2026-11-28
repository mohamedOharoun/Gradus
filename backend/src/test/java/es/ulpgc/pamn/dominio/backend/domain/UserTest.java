package es.ulpgc.pamn.dominio.backend.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UserName NAME = new UserName("Ana");
    private static final Email EMAIL = new Email("ana@ulpgc.es");
    private static final PasswordHash HASH = new PasswordHash("pbkdf2_sha256$1$c2Fs$aGFzaA==");
    private static final Instant CREATED_AT = Instant.parse("2026-11-13T10:00:00Z");
    private static final TermsAcceptance TERMS = new TermsAcceptance(CREATED_AT);

    @Test
    void requiresEveryField() {
        assertThrows(NullPointerException.class, () -> new User(null, NAME, EMAIL, HASH, CREATED_AT, TERMS));
        assertThrows(NullPointerException.class, () -> new User(ID, null, EMAIL, HASH, CREATED_AT, TERMS));
        assertThrows(NullPointerException.class, () -> new User(ID, NAME, null, HASH, CREATED_AT, TERMS));
        assertThrows(NullPointerException.class, () -> new User(ID, NAME, EMAIL, null, CREATED_AT, TERMS));
        assertThrows(NullPointerException.class, () -> new User(ID, NAME, EMAIL, HASH, null, TERMS));
        assertThrows(NullPointerException.class, () -> new User(ID, NAME, EMAIL, HASH, CREATED_AT, null));
    }

    @Test
    void toStringNeverRevealsThePasswordHash() {
        User user = new User(ID, NAME, EMAIL, HASH, CREATED_AT, TERMS);

        assertFalse(user.toString().contains(HASH.value()));
    }

    // Cambiar el nombre (Ajustes, fase 2) da un usuario nuevo: el resto no cambia y el original tampoco.
    @Test
    void renamingChangesOnlyTheName() {
        User ana = new User(ID, NAME, EMAIL, HASH, CREATED_AT, TERMS);

        User renamed = ana.withName(new UserName("Ana Ruiz"));

        assertEquals(new User(ID, new UserName("Ana Ruiz"), EMAIL, HASH, CREATED_AT, TERMS), renamed);
        assertEquals(NAME, ana.name());
    }

    @Test
    void changingThePasswordChangesOnlyItsHash() {
        User ana = new User(ID, NAME, EMAIL, HASH, CREATED_AT, TERMS);
        PasswordHash newHash = new PasswordHash("pbkdf2_sha256$1$c2Fs$bnVldm8=");

        assertEquals(new User(ID, NAME, EMAIL, newHash, CREATED_AT, TERMS), ana.withPasswordHash(newHash));
        assertEquals(HASH, ana.passwordHash());
    }

    @Test
    void passwordHashCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("  "));
    }
}
