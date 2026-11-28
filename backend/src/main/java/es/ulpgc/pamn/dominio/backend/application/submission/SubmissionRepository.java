package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: dónde se guardan las entregas y sus fotos. Hoy lo implementará PostgreSQL (paso 3d);
 * en los tests, uno en memoria. Las consultas (paso 3c) no ordenan: el orden lo decide el caso de uso.
 */
public interface SubmissionRepository {

    /** Guarda la entrega y su foto juntas: o las dos o ninguna. */
    void add(Submission submission, Jpeg photo);

    /** Las entregas de un alumno en un curso, en cualquier orden y <b>sin</b> los bytes de las fotos. */
    List<Submission> mine(UUID studentId, UUID courseId);

    Optional<Submission> find(UUID submissionId);

    /** Los bytes de la foto de una entrega. */
    Optional<Jpeg> photo(UUID submissionId);
}
