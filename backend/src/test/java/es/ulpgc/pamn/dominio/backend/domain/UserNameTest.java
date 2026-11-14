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

class UserNameTest {

    @Test
    void acceptsAValidNameAndRemovesSurroundingSpaces() {
        assertEquals("Ana", assertSuccess(UserName.of("  Ana  ")).value());
    }

    @Test
    void acceptsTheMinimumLength() {
        assertEquals("Al", assertSuccess(UserName.of("Al")).value());
    }

    @Test
    void acceptsTheMaximumLength() {
        String longest = "a".repeat(UserName.MAX_LENGTH);

        assertEquals(longest, assertSuccess(UserName.of(longest)).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A", "   ", " A "})
    void rejectsNamesThatAreTooShort(String raw) {
        assertEquals(List.of(ErrorCode.TOO_SHORT), codesFor("name", assertFailure(UserName.of(raw))));
    }

    @Test
    void rejectsNamesThatAreTooLong() {
        String tooLong = "a".repeat(UserName.MAX_LENGTH + 1);

        assertEquals(List.of(ErrorCode.TOO_LONG), codesFor("name", assertFailure(UserName.of(tooLong))));
    }

    @Test
    void countsCharactersNotUtf16Units() {
        // Cada emoji ocupa 2 unidades UTF-16 pero es 1 carácter, igual que cuenta PostgreSQL.
        String emojis = "😀".repeat(UserName.MAX_LENGTH);

        assertSuccess(UserName.of(emojis));
    }

    @ParameterizedTest
    @NullSource
    void rejectsNull(String raw) {
        assertEquals(List.of(ErrorCode.REQUIRED), codesFor("name", assertFailure(UserName.of(raw))));
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThrows(IllegalArgumentException.class, () -> new UserName("A"));
    }
}
