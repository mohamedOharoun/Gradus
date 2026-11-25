package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.Objects;

/** Los desenlaces de descargar la foto de una entrega. La capa web los traduce a 200, 404 y 403 (paso 3e). */
public sealed interface DownloadPhotoResult {

    /** Es suya: la entrega (para la fecha y el tamaño) y los bytes de la foto. */
    record Found(Submission submission, Jpeg photo) implements DownloadPhotoResult {
        public Found {
            Objects.requireNonNull(submission, "submission");
            Objects.requireNonNull(photo, "photo");
        }
    }

    /** No existe ninguna entrega con ese id. */
    record NotFound() implements DownloadPhotoResult {
    }

    /** Existe, pero es de otro alumno (decisión H). */
    record Forbidden() implements DownloadPhotoResult {
    }
}
