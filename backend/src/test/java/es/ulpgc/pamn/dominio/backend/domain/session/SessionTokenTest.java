package es.ulpgc.pamn.dominio.backend.domain.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTokenTest {

    @Test
    void generatesUrlSafeTokensOf32RandomBytes() {
        // 32 bytes en Base64 URL sin relleno son 43 caracteres que no hay que escapar en una cabecera.
        String value = SessionToken.generate().value();

        assertTrue(value.matches("[A-Za-z0-9_-]{43}"), value);
    }

    @Test
    void everyGeneratedTokenIsDifferent() {
        assertNotEquals(SessionToken.generate(), SessionToken.generate());
    }

    @Test
    void itsHashIsTheSha256InHexadecimal() {
        // Vector de prueba oficial de SHA-256 (FIPS 180-2) para «abc».
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                new SessionToken("abc").hash().value());
    }

    @Test
    void theSameTokenAlwaysGivesTheSameHash() {
        SessionToken token = SessionToken.generate();

        assertEquals(token.hash(), new SessionToken(token.value()).hash());
    }

    @Test
    void toStringNeverRevealsTheToken() {
        SessionToken token = SessionToken.generate();

        assertFalse(token.toString().contains(token.value()));
    }

    @Test
    void cannotBeNull() {
        assertThrows(NullPointerException.class, () -> new SessionToken(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void cannotBeBlank(String blank) {
        assertThrows(IllegalArgumentException.class, () -> new SessionToken(blank));
    }
}
