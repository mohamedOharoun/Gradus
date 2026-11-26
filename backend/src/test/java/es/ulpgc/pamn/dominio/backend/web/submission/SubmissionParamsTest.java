package es.ulpgc.pamn.dominio.backend.web.submission;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lectura de los ids y de la versión que llegan como texto (docs/tareas/ejercicio-foto.md, paso 3e-1). */
class SubmissionParamsTest {

    private static final String ID = "0b6f3c2e-8d1a-4f5b-9c7e-2a4d6f8b0c1e";

    @Test
    void aCanonicalUuidIsRead() {
        assertEquals(Optional.of(UUID.fromString(ID)), SubmissionParams.id(ID));
    }

    @Test
    void upperCaseHexIsAlsoAUuid() {
        assertEquals(Optional.of(UUID.fromString(ID)), SubmissionParams.id(ID.toUpperCase()));
    }

    // UUID.fromString sola aceptaría «1-2-3-4-5»: aquí solo vale la forma canónica.
    @Test
    void aShortenedUuidIsNotAnId() {
        assertTrue(SubmissionParams.id("1-2-3-4-5").isEmpty());
    }

    @Test
    void textOrNothingIsNotAnId() {
        assertTrue(SubmissionParams.id("grafos").isEmpty());
        assertTrue(SubmissionParams.id("").isEmpty());
        assertTrue(SubmissionParams.id(null).isEmpty());
    }

    @Test
    void aPositiveVersionIsRead() {
        assertEquals(Optional.of(1), SubmissionParams.version("1"));
        assertEquals(Optional.of(42), SubmissionParams.version("42"));
    }

    @Test
    void theLargestAcceptedVersionHasNineDigits() {
        assertEquals(Optional.of(999_999_999), SubmissionParams.version("999999999"));
        // Diez cifras podrían desbordar un int: no se intenta.
        assertTrue(SubmissionParams.version("2147483648").isEmpty());
    }

    @Test
    void zeroNegativeSignsAndLeadingZerosAreNotVersions() {
        assertTrue(SubmissionParams.version("0").isEmpty());
        assertTrue(SubmissionParams.version("-1").isEmpty());
        assertTrue(SubmissionParams.version("+1").isEmpty());
        assertTrue(SubmissionParams.version("01").isEmpty());
    }

    // Lo escribe la app, pero quien prueba con Yaak puede dejar un espacio: no se adivina.
    @Test
    void spacesOrDecimalsAreNotVersions() {
        assertTrue(SubmissionParams.version(" 1").isEmpty());
        assertTrue(SubmissionParams.version("1.0").isEmpty());
        assertTrue(SubmissionParams.version("uno").isEmpty());
    }

    @Test
    void anEmptyVersionIsNotAVersion() {
        assertTrue(SubmissionParams.version("").isEmpty());
        assertTrue(SubmissionParams.version(null).isEmpty());
    }
}
