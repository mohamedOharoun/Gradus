package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;

import java.util.Comparator;
import java.util.List;

/**
 * El orden en que {@code JdbcEnrollmentRepository} escribe: inscripciones por {@code courseId} y, dentro de
 * cada una, módulos por {@code moduleId}. Si dos móviles sincronizan los mismos cursos en distinto orden,
 * podrían bloquearse mutuamente (<i>deadlock</i>) por los bloqueos de fila de {@code ON CONFLICT}; con el
 * mismo orden en todas las transacciones, una espera a la otra.
 *
 * <p>Da igual cuál sea el orden mientras sea siempre el mismo: es el orden natural de {@link
 * java.util.UUID} (con signo, distinto del de PostgreSQL y del del texto) y el del {@link String}.
 */
final class EnrollmentWriteOrder {

    private EnrollmentWriteOrder() {
    }

    /** Función pura: devuelve una lista nueva y no cambia la recibida ni las fechas. */
    static List<Enrollment> sorted(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(EnrollmentWriteOrder::withSortedModules)
                .sorted(Comparator.comparing(Enrollment::courseId))
                .toList();
    }

    private static Enrollment withSortedModules(Enrollment enrollment) {
        List<MasteredModule> modules = enrollment.mastered().stream()
                .sorted(Comparator.comparing(MasteredModule::moduleId))
                .toList();
        return new Enrollment(enrollment.courseId(), enrollment.enrolledAt(), modules);
    }
}
