package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.matching;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.multipleChoice;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.photo;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GuessingOddsTest {

    static final double TOLERANCE = 1e-12;

    @ParameterizedTest(name = "q={0}, k={1}, p={2}/{3} → {4}")
    @CsvSource({
            "5,  4, 1, 4, 0.015625",
            "5,  4, 1, 2, 0.1875",
            "10, 8, 1, 2, 0.0546875",
            "4,  3, 1, 5, 0.0272"})
    void theBoundMatchesTheExactValuesOfTheSpecification(int questions, int minCorrect, int numerator,
                                                         int denominator, double expected) {
        double p = (double) numerator / denominator;

        assertEquals(expected, GuessingOdds.passBound(questions, minCorrect, p), TOLERANCE);
    }

    @Test
    void needingEveryAnswerIsTheProbabilityToTheQuestions() {
        assertEquals(1.0 / 32, GuessingOdds.passBound(5, 5, 0.5), TOLERANCE);
    }

    @Test
    void aPhotoExerciseCountsAsAlwaysPassed() {
        // No se corrige solo: en una prueba de dominio sería un acierto seguro. La regla
        // PHOTO_IN_REVIEW (tarea 2) lo prohíbe en la reserva; aquí solo se evita subestimar la cota.
        assertEquals(1.0, GuessingOdds.guessProbability(photo("f")), TOLERANCE);
    }

    @Test
    void aSingleChoiceIsGuessedWithOneInN() {
        assertEquals(1.0 / 5, GuessingOdds.guessProbability(singleChoice("e", 5)), TOLERANCE);
    }

    @Test
    void aMultipleChoiceIsGuessedWithOneInTwoToTheNMinusOne() {
        // Con 3 opciones hay 2³ − 1 = 7 selecciones no vacías.
        assertEquals(1.0 / 7, GuessingOdds.guessProbability(multipleChoice("e", 3, "a")), TOLERANCE);
    }

    @Test
    void aMatchingIsGuessedWithOneInMFactorial() {
        assertEquals(1.0 / 6, GuessingOdds.guessProbability(matching("e", 3)), TOLERANCE);
        assertEquals(1.0 / 40_320, GuessingOdds.guessProbability(matching("e", 8)), TOLERANCE);
    }

    @Test
    void theHardestCaseIsTheExerciseEasiestToGuess() {
        assertEquals(1.0 / 2, GuessingOdds.maxGuessProbability(
                List.of(singleChoice("a", 5), matching("b", 2), multipleChoice("c", 3, "a"))), TOLERANCE);
    }
}
