package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.EXPLANATION;
import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.PROMPT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchingTest {

    static List<MatchingPair> pairs(int count) {
        return IntStream.range(0, count).mapToObj(i -> new MatchingPair("Izquierda " + i, "Derecha " + i)).toList();
    }

    @Test
    void acceptsBetween2And8Pairs() {
        assertDoesNotThrow(() -> new Matching("e", PROMPT, pairs(2), EXPLANATION));
        assertDoesNotThrow(() -> new Matching("e", PROMPT, pairs(8), EXPLANATION));
        assertThrows(IllegalArgumentException.class, () -> new Matching("e", PROMPT, pairs(1), EXPLANATION));
        assertThrows(IllegalArgumentException.class, () -> new Matching("e", PROMPT, pairs(9), EXPLANATION));
    }

    @Test
    void repeatedSidesCanBeBuiltSoTheRulesCanReportThem() {
        // DUPLICATE_MATCHING_SIDE es una regla de la capa 3: el record no debe impedirla.
        List<MatchingPair> repeated = List.of(new MatchingPair("A", "1"), new MatchingPair("A", "2"));

        assertDoesNotThrow(() -> new Matching("e", PROMPT, repeated, EXPLANATION));
    }

    @Test
    void rejectsWhatTheSchemaForbids() {
        assertThrows(IllegalArgumentException.class, () -> new Matching("e-", PROMPT, pairs(2), EXPLANATION));
        assertThrows(IllegalArgumentException.class, () -> new Matching("e", "", pairs(2), EXPLANATION));
        assertThrows(NullPointerException.class, () -> new Matching("e", PROMPT, null, EXPLANATION));
    }
}
