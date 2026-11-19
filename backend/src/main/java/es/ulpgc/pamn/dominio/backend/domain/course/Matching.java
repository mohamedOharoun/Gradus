package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;

/**
 * Unir con flechas: es correcto reconstruir todas las parejas. Que ningún lado se repita (para que sea
 * una biyección) lo comprueba la regla {@code DUPLICATE_MATCHING_SIDE}, no el constructor.
 */
public record Matching(String id, String prompt, List<MatchingPair> pairs, String explanation) implements Exercise {

    public static final int MIN_PAIRS = 2;
    public static final int MAX_PAIRS = 8;

    public Matching {
        Require.exerciseFields(id, prompt, explanation);
        pairs = Require.size(pairs, "pairs", MIN_PAIRS, MAX_PAIRS);
    }
}
