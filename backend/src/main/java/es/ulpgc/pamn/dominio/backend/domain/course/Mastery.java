package es.ulpgc.pamn.dominio.backend.domain.course;

/**
 * Prueba de dominio (§2.6): se sortean {@code questions} ejercicios de la reserva y hacen falta
 * {@code minCorrect} aciertos. Que {@code minCorrect} no supere a {@code questions} lo comprueba la
 * regla {@code MASTERY_MIN_EXCEEDS_QUESTIONS}, no el constructor.
 */
public record Mastery(int questions, int minCorrect) {

    public static final int MIN = 1;
    public static final int MAX = 20;

    public Mastery {
        Require.range(questions, "questions", MIN, MAX);
        Require.range(minCorrect, "minCorrect", MIN, MAX);
    }
}
