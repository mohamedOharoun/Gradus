package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertFailure;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertSuccess;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.codesFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void normalizesToLowercaseWithoutSurroundingSpaces() {
        assertEquals("ana@ulpgc.es", assertSuccess(Email.of("  Ana@ULPGC.es ")).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "nombre.apellido+gradus@sub.dominio.es"})
    void acceptsValidFormats(String raw) {
        assertSuccess(Email.of(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sin-arroba", "a@b", "a@@b.com", "a b@c.com", "a@b..com", "@b.com", "a@.com"})
    void rejectsInvalidFormats(String raw) {
        assertEquals(List.of(ErrorCode.INVALID_EMAIL), codesFor("email", assertFailure(Email.of(raw))));
    }

    @Test
    void rejectsEmailsThatAreTooLong() {
        String tooLong = "a".repeat(Email.MAX_LENGTH) + "@b.com";

        assertEquals(List.of(ErrorCode.TOO_LONG), codesFor("email", assertFailure(Email.of(tooLong))));
    }

    @ParameterizedTest
    @NullSource
    void rejectsNull(String raw) {
        assertEquals(List.of(ErrorCode.REQUIRED), codesFor("email", assertFailure(Email.of(raw))));
    }

    @Test
    void constructorAlsoNormalizes() {
        assertEquals(new Email("ana@ulpgc.es"), new Email("ANA@ulpgc.es"));
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThrows(IllegalArgumentException.class, () -> new Email("sin-arroba"));
    }
}
