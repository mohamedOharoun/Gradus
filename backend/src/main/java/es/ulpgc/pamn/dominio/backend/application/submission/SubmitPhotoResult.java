package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.Objects;

/** Los desenlaces posibles de entregar una foto. La capa web traduce cada uno a un código HTTP (paso 3e). */
public sealed interface SubmitPhotoResult {

    /** Entregada y guardada, pendiente de revisión. */
    record Submitted(Submission submission) implements SubmitPhotoResult {
        public Submitted {
            Objects.requireNonNull(submission, "submission");
        }
    }

    /** El curso, o esa versión del curso, no existe. */
    record CourseNotFound() implements SubmitPhotoResult {
    }

    /** En esa versión no hay un ejercicio {@code photo} con ese id en los {@code steps}. */
    record NotAPhotoExercise() implements SubmitPhotoResult {
    }

    /** La foto falta, no es un JPEG o es demasiado grande; {@code reason} dice cuál de las tres. */
    record InvalidPhoto(JpegCheck reason) implements SubmitPhotoResult {
        public InvalidPhoto {
            Objects.requireNonNull(reason, "reason");
            if (reason instanceof JpegCheck.Valid) {
                throw new IllegalArgumentException("Una foto válida no es un motivo de rechazo");
            }
        }
    }
}
