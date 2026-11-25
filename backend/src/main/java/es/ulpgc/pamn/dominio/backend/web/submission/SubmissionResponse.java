package es.ulpgc.pamn.dominio.backend.web.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

/**
 * Una entrega en JSON (docs/tareas/ejercicio-foto.md, decisión Q): la respuesta 201 y cada elemento de
 * «mis entregas». Sin el alumno (es siempre el de la sesión) ni la foto (se descarga aparte).
 */
public record SubmissionResponse(String id, String courseId, int courseVersion, String exerciseId,
                                 String submittedAt, String status, int photoBytes) {

    public static SubmissionResponse from(Submission submission) {
        return new SubmissionResponse(
                submission.id().toString(),
                submission.courseId().toString(),
                submission.courseVersion(),
                submission.exerciseId(),
                submission.submittedAt().toString(), // ISO-8601, como en el resto de la API
                submission.status().name(),
                submission.photoBytes());
    }
}
