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
        assertFalse(messagesFor("name", assertFailure(UserName.of(raw))).isEmpty());
    }

    @Test
    void rejectsNamesThatAreTooLong() {
        String tooLong = "a".repeat(UserName.MAX_LENGTH + 1);

        assertFalse(messagesFor("name", assertFailure(UserName.of(tooLong))).isEmpty());
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
        assertEquals(1, messagesFor("name", assertFailure(UserName.of(raw))).size());
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThrows(IllegalArgumentException.class, () -> new UserName("A"));
    }
}
