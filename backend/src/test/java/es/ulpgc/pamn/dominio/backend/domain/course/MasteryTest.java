package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MasteryTest {

    @Test
    void questionsAndMinCorrectGoFrom1To20() {
        assertDoesNotThrow(() -> new Mastery(1, 1));
        assertDoesNotThrow(() -> new Mastery(20, 20));
        assertThrows(IllegalArgumentException.class, () -> new Mastery(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Mastery(21, 1));
        assertThrows(IllegalArgumentException.class, () -> new Mastery(4, 0));
        assertThrows(IllegalArgumentException.class, () -> new Mastery(4, 21));
    }

    @Test
    void moreRequiredHitsThanQuestionsCanBeBuiltSoTheRulesCanReportIt() {
        // MASTERY_MIN_EXCEEDS_QUESTIONS es una regla de la capa 5: el record no debe impedirla.
        assertDoesNotThrow(() -> new Mastery(3, 4));
    }
}
