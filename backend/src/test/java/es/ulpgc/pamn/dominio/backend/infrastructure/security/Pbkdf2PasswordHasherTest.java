package es.ulpgc.pamn.dominio.backend.infrastructure.security;

import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pbkdf2PasswordHasherTest {

    // Pocas iteraciones para que los tests sean rápidos; la seguridad real usa withDefaults().
    private static final int FAST_ITERATIONS = 1_000;

    private static final RawPassword PASSWORD = new RawPassword("Gradus2026!");
    private static final RawPassword OTHER_PASSWORD = new RawPassword("Otra2026!!");

    private final Pbkdf2PasswordHasher hasher = Pbkdf2PasswordHasher.withIterations(FAST_ITERATIONS);

    @Test
    void producesASelfDescribingHash() {
        String[] parts = hasher.hash(PASSWORD).value().split("\\$");

        assertEquals(4, parts.length);
        assertEquals("pbkdf2_sha256", parts[0]);
        assertEquals(String.valueOf(FAST_ITERATIONS), parts[1]);
    }

    @Test
    void neverStoresThePasswordItself() {
        assertFalse(hasher.hash(PASSWORD).value().contains(PASSWORD.value()));
    }

    @Test
    void theSamePasswordGivesDifferentHashesBecauseOfTheSalt() {
        assertNotEquals(hasher.hash(PASSWORD), hasher.hash(PASSWORD));
    }

    @Test
    void everyHashOfAPasswordMatchesThatPassword() {
        PasswordHash first = hasher.hash(PASSWORD);
        PasswordHash second = hasher.hash(PASSWORD);

        assertTrue(hasher.matches(PASSWORD, first));
        assertTrue(hasher.matches(PASSWORD, second));
    }

    @Test
    void doesNotMatchADifferentPassword() {
        assertFalse(hasher.matches(OTHER_PASSWORD, hasher.hash(PASSWORD)));
    }

    @Test
    void verifiesWithTheIterationsStoredInTheHash() {
        // Si algún día subimos las iteraciones, los usuarios antiguos deben poder seguir entrando.
        PasswordHash oldHash = hasher.hash(PASSWORD);

        assertTrue(Pbkdf2PasswordHasher.withIterations(FAST_ITERATIONS * 2).matches(PASSWORD, oldHash));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "texto-sin-formato",
            "bcrypt$1000$c2Fs$aGFzaA==",
            "pbkdf2_sha256$no-es-un-numero$c2Fs$aGFzaA==",
            "pbkdf2_sha256$0$c2Fs$aGFzaA==",
            "pbkdf2_sha256$1000$***$aGFzaA==",
            "pbkdf2_sha256$1000$c2Fs"
    })
    void aMalformedHashSimplyDoesNotMatch(String malformed) {
        assertFalse(hasher.matches(PASSWORD, new PasswordHash(malformed)));
    }

    @Test
    void defaultsFollowTheOwaspRecommendation() {
        String hash = Pbkdf2PasswordHasher.withDefaults().hash(PASSWORD).value();

        assertTrue(hash.startsWith("pbkdf2_sha256$600000$"));
    }

    @Test
    void rejectsANonPositiveNumberOfIterations() {
        assertThrows(IllegalArgumentException.class, () -> Pbkdf2PasswordHasher.withIterations(0));
    }
}
