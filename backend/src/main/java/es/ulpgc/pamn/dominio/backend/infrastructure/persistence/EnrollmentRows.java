package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.infrastructure.persistence.Timestamps.fromColumn;

/**
 * Filas de {@code enrollments} y de {@code mastered_modules} → {@link Enrollment}. Los constructores del
 * dominio vuelven a comprobar los datos al leerlos.
 */
final class EnrollmentRows {

    /** Una fila de {@code enrollments}. */
    record EnrollmentRow(UUID courseId, Instant enrolledAt) {
    }

    /** Una fila de {@code mastered_modules}. */
    record ModuleRow(UUID courseId, MasteredModule module) {
    }

    private EnrollmentRows() {
    }

    static EnrollmentRow enrollment(ResultSet row) throws SQLException {
        return new EnrollmentRow(row.getObject("course_id", UUID.class), fromColumn(row, "enrolled_at"));
    }

    static ModuleRow module(ResultSet row) throws SQLException {
        return new ModuleRow(row.getObject("course_id", UUID.class),
                new MasteredModule(row.getString("module_id"), fromColumn(row, "mastered_at")));
    }

    /**
     * Función pura: cada módulo va con su inscripción; una inscripción sin módulos sale con la lista vacía.
     * Conserva el orden de las inscripciones y el de los módulos de cada una.
     */
    static List<Enrollment> assemble(List<EnrollmentRow> enrollments, List<ModuleRow> modules) {
        Map<UUID, List<MasteredModule>> byCourse = new LinkedHashMap<>();
        for (ModuleRow row : modules) {
            byCourse.computeIfAbsent(row.courseId(), id -> new ArrayList<>()).add(row.module());
        }
        return enrollments.stream()
                .map(e -> new Enrollment(e.courseId(), e.enrolledAt(), byCourse.getOrDefault(e.courseId(), List.of())))
                .toList();
    }
}
