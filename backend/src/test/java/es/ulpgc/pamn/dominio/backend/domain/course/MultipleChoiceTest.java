package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.EXPLANATION;
import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.PROMPT;
import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.options;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipleChoiceTest {

    @Test
    void acceptsBetween3And6Options() {
        assertDoesNotThrow(() -> new MultipleChoice("e", PROMPT, options(3), List.of("o0"), EXPLANATION));
        assertDoesNotThrow(() -> new MultipleChoice("e", PROMPT, options(6), List.of("o0"), EXPLANATION));
        assertThrows(IllegalArgumentException.class,
                () -> new MultipleChoice("e", PROMPT, options(2), List.of("o0"), EXPLANATION));
        assertThrows(IllegalArgumentException.class,
                () -> new MultipleChoice("e", PROMPT, options(7), List.of("o0"), EXPLANATION));
    }

    @Test
    void acceptsBetween1And5DistinctCorrectIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultipleChoice("e", PROMPT, options(6), List.of(), EXPLANATION));
        assertThrows(IllegalArgumentException.class,
                () -> new MultipleChoice("e", PROMPT, options(6), List.of("o0", "o1", "o2", "o3", "o4", "o5"), EXPLANATION));
        assertThrows(IllegalArgumentException.class,
                () -> new MultipleChoice("e", PROMPT, options(6), List.of("o0", "o0"), EXPLANATION));
        assertThrows(IllegalArgumentException.class,
                () -> new MultipleChoice("e", PROMPT, options(6), List.of("O0"), EXPLANATION));
    }

    @Test
    void whatOnlyTheRulesDetectCanBeBuilt() {
        // UNKNOWN_OPTION y ALL_OPTIONS_CORRECT son reglas de la capa 3: el record no debe impedirlas.
        assertDoesNotThrow(() -> new MultipleChoice("e", PROMPT, options(3), List.of("zzz"), EXPLANATION));
        assertDoesNotThrow(() -> new MultipleChoice("e", PROMPT, options(3), List.of("o0", "o1", "o2"), EXPLANATION));
    }
}
