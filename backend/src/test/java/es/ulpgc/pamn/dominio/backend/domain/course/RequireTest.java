package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequireTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "bfs", "repr-l1", "a-b-c", "0", "modulo-10"})
    void acceptsWellFormedIds(String id) {
        assertEquals(id, Require.id(id, "id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-a", "a-", "a--b", "Bfs", "a_b", "a b", "módulo", "a\n"})
    void rejectsMalformedIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> Require.id(id, "id"));
    }

    @Test
    void idsHaveAtMost64Characters() {
        assertDoesNotThrow(() -> Require.id("a".repeat(64), "id"));
        assertThrows(IllegalArgumentException.class, () -> Require.id("a".repeat(65), "id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"G", "Introducción a los grafos", "BFS (anchura)", "a\tb"})
    void acceptsSingleLinePlainText(String text) {
        assertEquals(text, Require.plainText(text, "title", 120));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " Grafos", "Grafos ", "Grafos\n", "Dos\nlíneas", "Dos\rlíneas"})
    void rejectsPlainTextWithOuterSpacesOrLineBreaks(String text) {
        assertThrows(IllegalArgumentException.class, () -> Require.plainText(text, "title", 120));
    }

    @Test
    void plainTextHasAMaximumLength() {
        assertDoesNotThrow(() -> Require.plainText("x".repeat(32), "tags", 32));
        assertThrows(IllegalArgumentException.class, () -> Require.plainText("x".repeat(33), "tags", 32));
    }

    @ParameterizedTest
    @ValueSource(strings = {"es", "en", "es-ES", "pt-BR"})
    void acceptsLanguageCodes(String language) {
        assertEquals(language, Require.language(language, "language"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ES", "esp", "es-es", "es_ES", "es-", "es\n"})
    void rejectsMalformedLanguageCodes(String language) {
        assertThrows(IllegalArgumentException.class, () -> Require.language(language, "language"));
    }

    @Test
    void rangeAcceptsValuesWithinBounds() {
        assertEquals(1, Require.range(1, "questions", 1, 20));
        assertEquals(20, Require.range(20, "questions", 1, 20));
        assertThrows(IllegalArgumentException.class, () -> Require.range(0, "questions", 1, 20));
        assertThrows(IllegalArgumentException.class, () -> Require.range(21, "questions", 1, 20));
    }

    @Test
    void markdownNeedsSomethingOtherThanWhitespace() {
        assertDoesNotThrow(() -> Require.markdown("**grafo**", "text", 10));
        assertThrows(IllegalArgumentException.class, () -> Require.markdown("", "text", 10));
        assertThrows(IllegalArgumentException.class, () -> Require.markdown(" \n\t ", "text", 10));
    }

    @Test
    void markdownLengthCountsRealCharacters() {
        String threeEmojis = "📚".repeat(3);   // 3 caracteres reales, 6 «char» de Java

        assertDoesNotThrow(() -> Require.markdown(threeEmojis, "text", 3));
        assertThrows(IllegalArgumentException.class, () -> Require.markdown(threeEmojis + "x", "text", 3));
    }

    @Test
    void sizeAcceptsListsWithinBoundsAndReturnsAnImmutableCopy() {
        List<String> original = new ArrayList<>(List.of("a", "b"));

        List<String> copy = Require.size(original, "options", 2, 6);
        original.add("c");

        assertEquals(List.of("a", "b"), copy);
        assertThrows(UnsupportedOperationException.class, () -> copy.add("d"));
    }

    @Test
    void sizeRejectsListsOutOfBoundsOrWithNulls() {
        assertThrows(IllegalArgumentException.class, () -> Require.size(List.of("a"), "options", 2, 6));
        assertThrows(IllegalArgumentException.class, () -> Require.size(List.of("a", "b", "c"), "options", 1, 2));
        assertThrows(NullPointerException.class, () -> Require.size(null, "options", 0, 6));
        assertThrows(NullPointerException.class, () -> Require.size(Arrays.asList("a", null), "options", 0, 6));
    }

    @Test
    void distinctRejectsRepeatedElements() {
        assertEquals(List.of("a", "b"), Require.distinct(List.of("a", "b"), "correct"));
        assertThrows(IllegalArgumentException.class, () -> Require.distinct(List.of("a", "b", "a"), "correct"));
    }
}
