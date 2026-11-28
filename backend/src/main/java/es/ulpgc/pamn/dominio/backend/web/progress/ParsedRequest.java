package es.ulpgc.pamn.dominio.backend.web.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.EnrollmentDraft;

import java.util.List;

/**
 * Resultado de leer el cuerpo de {@code POST /api/progress/sync}: o los borradores (ya con todos sus campos
 * presentes y las fechas parseadas) o «no tiene la forma del contrato» (respuesta 400).
 */
public sealed interface ParsedRequest {

    record Parsed(List<EnrollmentDraft> enrollments) implements ParsedRequest {

        public Parsed {
            enrollments = List.copyOf(enrollments);
        }
    }

    record Malformed() implements ParsedRequest {
    }
}
