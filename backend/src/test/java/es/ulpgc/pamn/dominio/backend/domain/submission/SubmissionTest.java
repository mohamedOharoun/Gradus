package es.ulpgc.pamn.dominio.backend.domain.submission;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.submission.JpegTest.jpegOfSize;
import static es.ulpgc.pamn.dominio.backend.domain.submission.JpegTest.valid;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmissionTest {

    static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID COURSE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");

    static Submission submission(int courseVersion, String exerciseId, int photoBytes) {
        return new Submission(ID, STUDENT, COURSE, courseVersion, exerciseId, NOW, SubmissionStatus.PENDING, photoBytes);
    }

    @Test
    void aNewSubmissionIsPendingAndKnowsTheSizeOfItsPhoto() {
        Submission submission = Submission.pending(ID, STUDENT, COURSE, 2, "bfs-a-mano", NOW, valid(jpegOfSize(500)));

        assertEquals(new Submission(ID, STUDENT, COURSE, 2, "bfs-a-mano", NOW, SubmissionStatus.PENDING, 500),
                submission);
    }

    @Test
    void theOnlyStatusOfPhaseOneIsPending() {
        // Los estados de la revisión (bien / mal) llegan con la fase 2.
        assertEquals(1, SubmissionStatus.values().length);
        assertEquals(SubmissionStatus.PENDING, SubmissionStatus.valueOf("PENDING"));
    }

    @Test
    void theFirstVersionIsOne() {
        assertDoesNotThrow(() -> submission(1, "f", 10));
        assertThrows(IllegalArgumentException.class, () -> submission(0, "f", 10));
    }

    @Test
    void theExerciseIdCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> submission(1, " ", 10));
    }

    @Test
    void thePhotoSizeIsWithinTheJpegLimits() {
        assertDoesNotThrow(() -> submission(1, "f", Jpeg.MAX_BYTES));
        assertThrows(IllegalArgumentException.class, () -> submission(1, "f", 0));
        assertThrows(IllegalArgumentException.class, () -> submission(1, "f", Jpeg.MAX_BYTES + 1));
    }

    @Test
    void nothingCanBeMissing() {
        Jpeg photo = valid(jpegOfSize(10));
        assertThrows(NullPointerException.class, () -> Submission.pending(null, STUDENT, COURSE, 1, "f", NOW, photo));
        assertThrows(NullPointerException.class, () -> Submission.pending(ID, null, COURSE, 1, "f", NOW, photo));
        assertThrows(NullPointerException.class, () -> Submission.pending(ID, STUDENT, null, 1, "f", NOW, photo));
        assertThrows(NullPointerException.class, () -> Submission.pending(ID, STUDENT, COURSE, 1, null, NOW, photo));
        assertThrows(NullPointerException.class, () -> Submission.pending(ID, STUDENT, COURSE, 1, "f", null, photo));
        assertThrows(NullPointerException.class, () -> Submission.pending(ID, STUDENT, COURSE, 1, "f", NOW, null));
        assertThrows(NullPointerException.class,
                () -> new Submission(ID, STUDENT, COURSE, 1, "f", NOW, null, 10));
    }
}
