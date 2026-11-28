package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Matching;
import es.ulpgc.pamn.dominio.backend.domain.course.MultipleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.PhotoExercise;
import es.ulpgc.pamn.dominio.backend.domain.course.SingleChoice;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Probabilidad de aprobar una prueba de dominio adivinando (§3.6). Funciones puras. Supone el contrato
 * con la app (§5): en {@code multipleChoice} hay que marcar al menos una opción y en {@code matching}
 * hay que completar todas las parejas.
 */
public final class GuessingOdds {

    private GuessingOdds() {
    }

    /** Probabilidad de acertar un ejercicio al azar. */
    public static double guessProbability(Exercise exercise) {
        return switch (exercise) {
            case SingleChoice single -> 1.0 / single.options().size();
            case MultipleChoice multiple -> 1.0 / (Math.pow(2, multiple.options().size()) - 1);
            case Matching matching -> 1.0 / factorial(matching.pairs().size());
            // No se corrige solo: en una prueba de dominio sería un acierto seguro. PHOTO_IN_REVIEW
            // lo prohíbe en la reserva; con 1 la cota nunca se queda corta si algún día llegase.
            case PhotoExercise photo -> 1.0;
        };
    }

    /** La mayor probabilidad de la reserva: con ella, la binomial es una cota superior (§3.6). */
    public static double maxGuessProbability(List<Exercise> review) {
        return review.stream().mapToDouble(GuessingOdds::guessProbability).max().orElse(0);
    }

    /** P(X ≥ k) con X ~ Binomial(q, p): Σ desde i = k hasta q de C(q, i) · pⁱ · (1 − p)^(q − i). */
    public static double passBound(int questions, int minCorrect, double p) {
        return IntStream.rangeClosed(minCorrect, questions)
                .mapToDouble(i -> binomial(questions, i) * Math.pow(p, i) * Math.pow(1 - p, questions - i))
                .sum();
    }

    /** C(n, k) multiplicando de uno en uno: con n ≤ 20 es exacto en un double. */
    private static double binomial(int n, int k) {
        return IntStream.rangeClosed(1, k).mapToDouble(i -> (double) (n - k + i) / i).reduce(1, (a, b) -> a * b);
    }

    private static double factorial(int m) {
        return IntStream.rangeClosed(2, m).asDoubleStream().reduce(1, (a, b) -> a * b);
    }
}
