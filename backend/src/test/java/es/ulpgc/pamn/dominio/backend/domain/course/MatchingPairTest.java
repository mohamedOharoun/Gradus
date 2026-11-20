package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchingPairTest {

    @Test
    void acceptsAValidPair() {
        assertDoesNotThrow(() -> new MatchingPair("Matriz de adyacencia", "O(V²)"));
    }

    @Test
    void eachSideHasAtMost200Characters() {
        assertDoesNotThrow(() -> new MatchingPair("x".repeat(200), "y".repeat(200)));
        assertThrows(IllegalArgumentException.class, () -> new MatchingPair("x".repeat(201), "y"));
        assertThrows(IllegalArgumentException.class, () -> new MatchingPair("x", "y".repeat(201)));
    }

    @Test
    void rejectsBlankOrMissingSides() {
        assertThrows(IllegalArgumentException.class, () -> new MatchingPair(" ", "y"));
        assertThrows(IllegalArgumentException.class, () -> new MatchingPair("x", "\n"));
        assertThrows(NullPointerException.class, () -> new MatchingPair(null, "y"));
        assertThrows(NullPointerException.class, () -> new MatchingPair("x", null));
    }
}
