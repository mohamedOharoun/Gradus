package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Descargar la foto de una entrega: solo su dueño (docs/tareas/ejercicio-foto.md, paso 3c, decisión H). */
class DownloadSubmissionPhotoTest {

    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID LUIS = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID GRAFOS = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private final InMemorySubmissionRepository repository = new InMemorySubmissionRepository();
    private final DownloadSubmissionPhoto download = new DownloadSubmissionPhoto(repository);

    private final Jpeg photo = ((JpegCheck.Valid) Jpeg.check(SubmitPhotoTest.jpeg(300))).jpeg();
    private final Submission anasSubmission = Submission.pending(SUBMISSION_ID, ANA, GRAFOS, 1, "repr-foto",
            Instant.parse("2026-11-24T09:00:00Z"), photo);

    DownloadSubmissionPhotoTest() {
        repository.add(anasSubmission, photo);
    }

    @Test
    void theOwnerGetsHerPhotoAndItsSubmission() {
        var found = assertInstanceOf(DownloadPhotoResult.Found.class, download.download(ANA, SUBMISSION_ID));

        assertEquals(photo, found.photo());
        assertEquals(anasSubmission, found.submission());
    }

    @Test
    void anotherStudentIsForbidden() {
        assertInstanceOf(DownloadPhotoResult.Forbidden.class, download.download(LUIS, SUBMISSION_ID));
    }

    @Test
    void aSubmissionThatDoesNotExistIsNotFound() {
        assertInstanceOf(DownloadPhotoResult.NotFound.class,
                download.download(ANA, UUID.fromString("00000000-0000-0000-0000-0000000000ff")));
    }

    // Si la entrega no existe, da igual quién pregunte: no se distingue «no existe» de «no es tuya».
    @Test
    void aMissingSubmissionIsNotFoundForAnyone() {
        assertInstanceOf(DownloadPhotoResult.NotFound.class,
                download.download(LUIS, UUID.fromString("00000000-0000-0000-0000-0000000000ff")));
    }

    // Una entrega sin foto guardada no debería existir (se guardan juntas): es fallo del servidor (500).
    @Test
    void aSubmissionWithoutItsPhotoIsAServerFault() {
        UUID broken = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
        repository.addWithoutPhoto(Submission.pending(broken, ANA, GRAFOS, 1, "repr-foto",
                Instant.parse("2026-11-24T09:00:00Z"), photo));

        assertThrows(IllegalStateException.class, () -> download.download(ANA, broken));
    }

    @Test
    void foundCannotBeBuiltWithoutItsParts() {
        assertThrows(NullPointerException.class, () -> new DownloadPhotoResult.Found(null, photo));
        assertThrows(NullPointerException.class, () -> new DownloadPhotoResult.Found(anasSubmission, null));
    }
}
