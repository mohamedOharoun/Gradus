package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.submission.SubmitPhotoTest.jpeg;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los valores que acompañan al caso de uso: el comando y el resultado. */
class SubmitPhotoValuesTest {

    static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Test
    void aValidPhotoIsNotAReasonToReject() {
        JpegCheck valid = Jpeg.check(jpeg(10));

        assertThrows(IllegalArgumentException.class, () -> new SubmitPhotoResult.InvalidPhoto(valid));
        assertDoesNotThrow(() -> new SubmitPhotoResult.InvalidPhoto(new JpegCheck.NotJpeg()));
    }

    @Test
    void theCommandNeverPrintsThePhoto() {
        byte[] photo = jpeg(1234);
        photo[3] = 42;

        String text = new SubmitPhotoCommand(ID, ID, 1, "f", photo).toString();

        assertTrue(text.contains("1234 bytes"), text);
        assertFalse(text.contains("42"), text);
    }

    @Test
    void theCommandSaysWhenThereIsNoPhoto() {
        assertTrue(new SubmitPhotoCommand(ID, ID, 1, "f", null).toString().contains("photo=ninguna"));
    }

    @Test
    void theCommandNeedsWhoWhatAndWhere() {
        assertThrows(NullPointerException.class, () -> new SubmitPhotoCommand(null, ID, 1, "f", null));
        assertThrows(NullPointerException.class, () -> new SubmitPhotoCommand(ID, null, 1, "f", null));
        assertThrows(NullPointerException.class, () -> new SubmitPhotoCommand(ID, ID, 1, null, null));
    }
}
