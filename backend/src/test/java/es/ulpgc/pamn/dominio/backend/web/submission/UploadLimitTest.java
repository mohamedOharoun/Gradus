package es.ulpgc.pamn.dominio.backend.web.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El tamaño máximo de una petición con foto (docs/tareas/ejercicio-foto.md, decisiones N y O). */
class UploadLimitTest {

    @Test
    void theLimitIsTwoMebibytes() {
        assertEquals(2 * 1024 * 1024, UploadLimit.MAX_REQUEST_BYTES);
    }

    // La foto más grande que se acepta tiene que caber con margen: el multipart añade separadores y «version».
    @Test
    void theLargestPhotoFitsWithRoomForTheRestOfTheRequest() {
        assertTrue(Jpeg.MAX_BYTES + 64 * 1024 <= UploadLimit.MAX_REQUEST_BYTES);
    }

    @Test
    void aRequestUpToTheLimitIsNotTooLarge() {
        assertFalse(UploadLimit.exceededBy(1_000));
        assertFalse(UploadLimit.exceededBy(UploadLimit.MAX_REQUEST_BYTES));
    }

    @Test
    void oneByteOverTheLimitIsTooLarge() {
        assertTrue(UploadLimit.exceededBy(UploadLimit.MAX_REQUEST_BYTES + 1L));
        assertTrue(UploadLimit.exceededBy(20_000_000L));
    }

    // Sin cabecera Content-Length (Javalin da -1) no se sabe: decide el límite del multipart de Jetty.
    @Test
    void anUnknownLengthIsNotJudgedHere() {
        assertFalse(UploadLimit.exceededBy(-1));
    }
}
