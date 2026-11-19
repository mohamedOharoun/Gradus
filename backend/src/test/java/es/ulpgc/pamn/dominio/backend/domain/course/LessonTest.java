package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonTest {

    @Test
    void acceptsAValidLesson() {
        assertDoesNotThrow(() -> new Lesson("repr-l1", "Un **grafo** G = (V, E)…"));
    }

    @Test
    void textHasAtMost10000Characters() {
        assertDoesNotThrow(() -> new Lesson("l", "x".repeat(10_000)));
        assertThrows(IllegalArgumentException.class, () -> new Lesson("l", "x".repeat(10_001)));
    }

    @Test
    void rejectsWhatTheSchemaForbids() {
        assertThrows(IllegalArgumentException.class, () -> new Lesson("Repr", "Texto"));
        assertThrows(IllegalArgumentException.class, () -> new Lesson("repr", "   "));
        assertThrows(NullPointerException.class, () -> new Lesson(null, "Texto"));
        assertThrows(NullPointerException.class, () -> new Lesson("repr", null));
    }
}
