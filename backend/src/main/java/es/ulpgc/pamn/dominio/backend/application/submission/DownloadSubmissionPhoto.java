package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: descargar la foto de una entrega (docs/tareas/ejercicio-foto.md, paso 3c). En la fase 1 solo
 * la puede ver el alumno que la subió (decisión H); en la fase 2 también el autor del curso.
 *
 * <p>Orden: ¿existe? → ¿es suya? → la foto. Si no existe, da igual quién pregunte: {@code NotFound}.
 */
public final class DownloadSubmissionPhoto {

    private final SubmissionRepository submissions;

    public DownloadSubmissionPhoto(SubmissionRepository submissions) {
        this.submissions = Objects.requireNonNull(submissions, "submissions");
    }

    /** {@code requesterId} sale de la sesión, nunca del cliente. */
    public DownloadPhotoResult download(UUID requesterId, UUID submissionId) {
        Objects.requireNonNull(requesterId, "requesterId");
        Objects.requireNonNull(submissionId, "submissionId");
        Optional<Submission> submission = submissions.find(submissionId);
        if (submission.isEmpty()) {
            return new DownloadPhotoResult.NotFound();
        }
        if (!submission.get().studentId().equals(requesterId)) {
            return new DownloadPhotoResult.Forbidden();
        }
        // La entrega y su foto se guardan juntas (SubmissionRepository.add): si falta la foto, el fallo es
        // del servidor (500), no del alumno.
        Jpeg photo = submissions.photo(submissionId).orElseThrow(() -> new IllegalStateException(
                "La entrega " + submissionId + " no tiene foto guardada"));
        return new DownloadPhotoResult.Found(submission.get(), photo);
    }
}
