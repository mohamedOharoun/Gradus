package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida: dónde se guarda el progreso de cada usuario. La aplicación no sabe si es PostgreSQL
 * o memoria. Nunca borra nada: el progreso solo crece (regla 1 de {@code api-progreso.md}).
 */
public interface EnrollmentRepository {

    /**
     * Fusiona las inscripciones con lo que ya había guardado de ese usuario: unión, y gana la fecha más
     * antigua. La fusión la hace el repositorio y de forma atómica (en PostgreSQL, {@code INSERT … ON
     * CONFLICT}): quien llama no lee, fusiona y guarda, porque dos móviles pueden sincronizar a la vez.
     */
    void mergeAll(UUID userId, List<Enrollment> enrollments);

    /** Todo el progreso del usuario; vacío si no tiene ninguno. */
    List<Enrollment> findAll(UUID userId);
}
