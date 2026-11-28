package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida: dónde se guarda el progreso de cada usuario. La aplicación no sabe si es PostgreSQL
 * o memoria. La sincronización ({@link #mergeAll}) nunca borra nada: el progreso solo crece al fusionar;
 * la baja explícita de un curso se hace con {@link #delete}.
 */
public interface EnrollmentRepository {

    /**
     * Fusiona las inscripciones con lo que ya había guardado de ese usuario: unión, y gana la fecha más
     * antigua. Nunca borra inscripciones ni módulos existentes. La fusión la hace el repositorio y de forma
     * atómica (en PostgreSQL, {@code INSERT … ON CONFLICT}): quien llama no lee, fusiona y guarda, porque
     * dos móviles pueden sincronizar a la vez.
     */
    void mergeAll(UUID userId, List<Enrollment> enrollments);

    /** Todo el progreso del usuario; vacío si no tiene ninguno. */
    List<Enrollment> findAll(UUID userId);

    /**
     * Elimina la inscripción del usuario en el curso indicado y, en cascada, todos sus módulos dominados.
     * Es idempotente: si el usuario no estaba inscrito en ese curso, no hace nada y no lanza error.
     */
    void delete(UUID userId, UUID courseId);
}
