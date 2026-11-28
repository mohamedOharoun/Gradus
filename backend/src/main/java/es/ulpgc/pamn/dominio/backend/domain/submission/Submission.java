package es.ulpgc.pamn.dominio.backend.domain.submission;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una entrega de un ejercicio con foto (docs/tareas/ejercicio-foto.md). No lleva los bytes de la foto:
 * listar entregas no debe cargar fotos de 2 MB. La foto va aparte ({@link Jpeg}) y aquí solo su tamaño.
 *
 * @param courseVersion la versión del curso que estudiaba el alumno (decisión M); el ejercicio existe en ella.
 */
public record Submission(UUID id, UUID studentId, UUID courseId, int courseVersion, String exerciseId,
                         Instant submittedAt, SubmissionStatus status, int photoBytes) {

    public Submission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(exerciseId, "exerciseId");
        Objects.requireNonNull(submittedAt, "submittedAt");
        Objects.requireNonNull(status, "status");
        if (courseVersion < 1) {
            throw new IllegalArgumentException("La versión de un curso empieza en 1: " + courseVersion);
        }
        if (exerciseId.isBlank()) {
            throw new IllegalArgumentException("Una entrega necesita el id de su ejercicio");
        }
        if (photoBytes < 1 || photoBytes > Jpeg.MAX_BYTES) {
            throw new IllegalArgumentException("Tamaño de foto imposible: " + photoBytes);
        }
    }

    /** Factory method: una entrega recién hecha, pendiente de revisión, con el tamaño de su foto. */
    public static Submission pending(UUID id, UUID studentId, UUID courseId, int courseVersion, String exerciseId,
                                     Instant submittedAt, Jpeg photo) {
        Objects.requireNonNull(photo, "photo");
        return new Submission(id, studentId, courseId, courseVersion, exerciseId, submittedAt,
                SubmissionStatus.PENDING, photo.size());
    }
}
