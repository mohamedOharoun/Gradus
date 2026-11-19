package es.ulpgc.pamn.dominio.backend.domain.course;

/** Una pareja correcta de un ejercicio de unir: el lado izquierdo va con el derecho. */
public record MatchingPair(String left, String right) {

    public static final int MAX_SIDE_LENGTH = 200;

    public MatchingPair {
        Require.markdown(left, "left", MAX_SIDE_LENGTH);
        Require.markdown(right, "right", MAX_SIDE_LENGTH);
    }
}
