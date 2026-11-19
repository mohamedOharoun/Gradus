package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comparación entre dos versiones de un curso (§4.6). El progreso de los alumnos se refiere a los ids
 * de módulos, lecciones y ejercicios: si desaparecen, ese progreso se pierde.
 */
public final class VersionComparison {

    private VersionComparison() {
    }

    /**
     * {@code REMOVED_IDS} (aviso): un único problema en la raíz con los ids de {@code previous} que ya
     * no están en {@code next}, en el orden de {@code previous}. Lista vacía si no falta ninguno.
     */
    public static List<Issue> compareVersions(CourseDocument previous, CourseDocument next) {
        Set<String> kept = Locations.ids(next).map(Located::value).collect(Collectors.toSet());
        List<String> removed = Locations.ids(previous)
                .map(Located::value)
                .filter(id -> !kept.contains(id))
                .distinct()
                .toList();
        if (removed.isEmpty()) {
            return List.of();
        }
        return List.of(Issue.at(JsonPointer.root(), CourseIssueCode.REMOVED_IDS,
                "Esta versión elimina ids que existían en la anterior: " + String.join(", ", removed)
                        + ". El progreso de los alumnos sobre ellos se perderá. Si solo los has renombrado, "
                        + "vuelve a usar los ids anteriores."));
    }
}
