package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

/**
 * Puerto de salida: dónde se guardan las entregas y sus fotos. Hoy lo implementará PostgreSQL (paso 3d);
 * en los tests, uno en memoria. Crecerá en el paso 3c con las consultas.
 */
public interface SubmissionRepository {

    /** Guarda la entrega y su foto juntas: o las dos o ninguna. */
    void add(Submission submission, Jpeg photo);
}
