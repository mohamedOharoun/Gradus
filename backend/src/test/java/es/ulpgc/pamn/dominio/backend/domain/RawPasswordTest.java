package es.ulpgc.pamn.dominio.backend.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertFailure;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertSuccess;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.messagesFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawPasswordTest {

    private static final String VALID = "Gradus2026!";

    @Test
    void acceptsAPasswordThatMeetsAllRules() {
        assertEquals(VALID, assertSuccess(RawPassword.of(VALID)).value());
    }

    @Test
    void keepsSpacesBecauseTheyArePartOfThePassword() {
        assertEquals(" Gradus2026! ", assertSuccess(RawPassword.of(" Gradus2026! ")).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Ab1!xyz",      // 7 caracteres
            "12345678!",    // sin letra
            "Gradus!!!",    // sin número
            "Gradus2026",   // sin carácter especial
            "Gradus 2026"   // el espacio no cuenta como carácter especial
    })
    void rejectsPasswordsThatBreakOneRule(String raw) {
        assertEquals(1, messagesFor("password", assertFailure(RawPassword.of(raw))).size());
    }

    @Test
    void reportsEveryBrokenRuleAtOnce() {
        // Corta, sin número y sin carácter especial: tres errores.
        assertEquals(3, messagesFor("password", assertFailure(RawPassword.of("abc"))).size());
    }

    @ParameterizedTest
    @NullSource
    void rejectsNull(String raw) {
        assertEquals(1, messagesFor("password", assertFailure(RawPassword.of(raw))).size());
    }

    @Test
    void toStringNeverRevealsThePassword() {
        RawPassword password = assertSuccess(RawPassword.of(VALID));

        assertFalse(password.toString().contains(VALID));
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword("abc"));
    }
}
