package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionTest {

    @Test
    void acceptsAValidOption() {
        assertDoesNotThrow(() -> new Option("a", "Matriz de adyacencia"));
    }

    @Test
    void textHasAtMost300Characters() {
        assertDoesNotThrow(() -> new Option("a", "x".repeat(300)));
        assertThrows(IllegalArgumentException.class, () -> new Option("a", "x".repeat(301)));
    }

    @Test
    void rejectsWhatTheSchemaForbids() {
        assertThrows(IllegalArgumentException.class, () -> new Option("A", "Texto"));
        assertThrows(IllegalArgumentException.class, () -> new Option("a", ""));
        assertThrows(NullPointerException.class, () -> new Option(null, "Texto"));
        assertThrows(NullPointerException.class, () -> new Option("a", null));
    }
}
