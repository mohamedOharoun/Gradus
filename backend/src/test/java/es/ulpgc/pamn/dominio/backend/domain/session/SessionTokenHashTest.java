package es.ulpgc.pamn.dominio.backend.domain.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionTokenHashTest {

    @Test
    void acceptsA64CharacterLowercaseHexValue() {
        assertDoesNotThrow(() -> new SessionTokenHash("a".repeat(64)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "abc",
            "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD", // mayúsculas
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015a",  // 63 caracteres
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015adf", // 65 caracteres
            "zz7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"  // no hexadecimal
    })
    void rejectsAnythingThatIsNotASha256InHex(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new SessionTokenHash(invalid));
    }

    @Test
    void cannotBeNull() {
        assertThrows(NullPointerException.class, () -> new SessionTokenHash(null));
    }
}
