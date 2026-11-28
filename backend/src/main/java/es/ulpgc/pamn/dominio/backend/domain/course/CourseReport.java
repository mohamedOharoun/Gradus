package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.stream.Stream;

/**
 * Informe de validación de un curso (§3.7). Inmutable y siempre en orden de documento, se cree donde
 * se cree: así el mismo fichero produce siempre el mismo informe. Es válido si y solo si no hay errores.
 */
public record CourseReport(List<Issue> issues) {

    public static final CourseReport EMPTY = new CourseReport(List.of());

    public CourseReport {
        // sorted es estable: a igual ruta se conserva el orden en que llegaron.
        issues = issues.stream().sorted(DocumentOrder.ISSUES).toList();
    }

    public List<Issue> errors() {
        return issues.stream().filter(Issue::isError).toList();
    }

    public List<Issue> warnings() {
        return issues.stream().filter(issue -> !issue.isError()).toList();
    }

    public boolean isValid() {
        return issues.stream().noneMatch(Issue::isError);
    }

    /** Un informe nuevo con estos problemas añadidos (por ejemplo, {@code REMOVED_IDS} al publicar). */
    public CourseReport with(List<Issue> more) {
        return new CourseReport(Stream.concat(issues.stream(), more.stream()).toList());
    }
}
