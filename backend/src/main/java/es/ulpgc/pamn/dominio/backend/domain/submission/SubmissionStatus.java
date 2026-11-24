package es.ulpgc.pamn.dominio.backend.domain.submission;

/**
 * En qué punto está una entrega. En la fase 1 solo existe {@code PENDING}; la revisión del profesor
 * (fase 2, docs/tareas/ejercicio-foto.md) añadirá los suyos. El nombre se guarda tal cual en la base
 * de datos y viaja a la app, así que no se renombra.
 */
public enum SubmissionStatus {
    /** Entregada; nadie la ha revisado todavía. */
    PENDING
}
