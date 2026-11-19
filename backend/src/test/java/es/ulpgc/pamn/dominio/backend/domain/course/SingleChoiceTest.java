package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SingleChoiceTest {

    static final String PROMPT = "¿Qué estructura usa BFS?";
    static final String EXPLANATION = "La cola es FIFO.";

    static List<Option> options(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new Option("o" + i, "Opción " + i))
                .toList();
    }

    @Test
    void acceptsBetween2And6Options() {
        assertDoesNotThrow(() -> new SingleChoice("e", PROMPT, options(2), "o0", EXPLANATION));
        assertDoesNotThrow(() -> new SingleChoice("e", PROMPT, options(6), "o0", EXPLANATION));
        assertThrows(IllegalArgumentException.class, () -> new SingleChoice("e", PROMPT, options(1), "o0", EXPLANATION));
        assertThrows(IllegalArgumentException.class, () -> new SingleChoice("e", PROMPT, options(7), "o0", EXPLANATION));
    }

    @Test
    void anUnknownAnswerCanBeBuiltSoTheRulesCanReportIt() {
        // UNKNOWN_OPTION es una regla de la capa 3: el record no debe impedirlo.
        assertDoesNotThrow(() -> new SingleChoice("e", PROMPT, options(2), "zzz", EXPLANATION));
    }

    @Test
    void promptAndExplanationHaveTheirMaximumLengths() {
        assertThrows(IllegalArgumentException.class,
                () -> new SingleChoice("e", "x".repeat(2001), options(2), "o0", EXPLANATION));
        assertThrows(IllegalArgumentException.class,
                () -> new SingleChoice("e", PROMPT, options(2), "o0", "x".repeat(3001)));
    }

    @Test
    void rejectsWhatTheSchemaForbids() {
        assertThrows(IllegalArgumentException.class, () -> new SingleChoice("E", PROMPT, options(2), "o0", EXPLANATION));
        assertThrows(IllegalArgumentException.class, () -> new SingleChoice("e", PROMPT, options(2), "O0", EXPLANATION));
        assertThrows(IllegalArgumentException.class, () -> new SingleChoice("e", PROMPT, options(2), "o0", " "));
        assertThrows(NullPointerException.class, () -> new SingleChoice("e", PROMPT, null, "o0", EXPLANATION));
    }
}
