package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhotoExerciseTest {

    @Test
    void acceptsAValidPhotoExercise() {
        assertDoesNotThrow(() -> new PhotoExercise("bfs-a-mano",
                "Recorre en anchura el grafo empezando por **A**.", "Orden: A, B, C, D, E."));
    }

    @Test
    void isAnExerciseSoItCanBeAStep() {
        // Que pueda ir en la reserva (review) también: lo prohíbe una regla con su ruta, no el tipo.
        assertInstanceOf(Exercise.class, new PhotoExercise("f", "Enunciado", "Solución"));
    }

    @Test
    void promptHasAtMost2000Characters() {
        assertDoesNotThrow(() -> new PhotoExercise("f", "x".repeat(2_000), "Solución"));
        assertThrows(IllegalArgumentException.class, () -> new PhotoExercise("f", "x".repeat(2_001), "Solución"));
    }

    @Test
    void explanationHasAtMost3000Characters() {
        assertDoesNotThrow(() -> new PhotoExercise("f", "Enunciado", "x".repeat(3_000)));
        assertThrows(IllegalArgumentException.class, () -> new PhotoExercise("f", "Enunciado", "x".repeat(3_001)));
    }

    @Test
    void rejectsWhatTheSchemaForbids() {
        assertThrows(IllegalArgumentException.class, () -> new PhotoExercise("Bfs", "Enunciado", "Solución"));
        assertThrows(IllegalArgumentException.class, () -> new PhotoExercise("f", "   ", "Solución"));
        assertThrows(IllegalArgumentException.class, () -> new PhotoExercise("f", "Enunciado", "   "));
        assertThrows(NullPointerException.class, () -> new PhotoExercise(null, "Enunciado", "Solución"));
        assertThrows(NullPointerException.class, () -> new PhotoExercise("f", null, "Solución"));
        assertThrows(NullPointerException.class, () -> new PhotoExercise("f", "Enunciado", null));
    }
}
