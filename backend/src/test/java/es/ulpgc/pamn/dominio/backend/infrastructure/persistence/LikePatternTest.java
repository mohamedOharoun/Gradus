package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LikePatternTest {

    @ParameterizedTest(name = "«{0}» → «{1}»")
    @CsvSource(delimiter = '|', value = {
            "grafos      | %grafos%",
            "100%        | %100\\%%",
            "a_b         | %a\\_b%",
            "c:\\temp    | %c:\\\\temp%",
            "\\%         | %\\\\\\%%"})
    void wildcardsAndTheEscapeCharacterBecomeLiteral(String text, String pattern) {
        assertEquals(pattern, LikePattern.containing(text));
    }

    @Test
    void theEscapeCharacterIsEscapedFirst() {
        // Si se escapara después, el «\» añadido delante de «%» se duplicaría: «\%» acabaría como «\\%».
        assertEquals("\\\\\\%", LikePattern.escape("\\%"));
    }

    @Test
    void anEmptyTextMatchesEverything() {
        assertEquals("%%", LikePattern.containing(""));
    }
}
