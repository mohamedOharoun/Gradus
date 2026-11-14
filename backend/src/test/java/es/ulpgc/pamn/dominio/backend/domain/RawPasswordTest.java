package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertFailure;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertSuccess;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.codesFor;
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
    @CsvSource({
            "Ab1!xyz,     TOO_SHORT",                  // 7 caracteres
            "12345678!,   MISSING_LETTER",
            "Gradus!!!,   MISSING_DIGIT",
            "Gradus2026,  MISSING_SPECIAL_CHARACTER",
            "Gradus 2026, MISSING_SPECIAL_CHARACTER"   // el espacio no cuenta como carácter especial
    })
    void reportsWhichRuleIsBroken(String raw, ErrorCode expected) {
        assertEquals(List.of(expected), codesFor("password", assertFailure(RawPassword.of(raw))));
    }

    @Test
    void reportsEveryBrokenRuleAtOnce() {
        assertEquals(
                List.of(ErrorCode.TOO_SHORT, ErrorCode.MISSING_DIGIT, ErrorCode.MISSING_SPECIAL_CHARACTER),
                codesFor("password", assertFailure(RawPassword.of("abc"))));
    }

    @Test
    void anEmptyPasswordIsOnlyRequired() {
        // Antes daba 4 errores (corta, sin letra, sin número, sin especial): ahora uno, como en la app.
        assertEquals(List.of(ErrorCode.REQUIRED), codesFor("password", assertFailure(RawPassword.of(""))));
    }

    @Test
    void countsAnEmojiAsOneCharacter() {
        // 7 caracteres reales (11 unidades UTF-16): es corta, igual que en la app.
        assertEquals(List.of(ErrorCode.TOO_SHORT), codesFor("password", assertFailure(RawPassword.of("😀😀😀😀a1!"))));
    }

    @ParameterizedTest
    @NullSource
    void rejectsNull(String raw) {
        assertEquals(List.of(ErrorCode.REQUIRED), codesFor("password", assertFailure(RawPassword.of(raw))));
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
