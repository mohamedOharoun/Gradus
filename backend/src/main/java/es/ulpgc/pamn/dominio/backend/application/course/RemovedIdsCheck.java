package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.VersionComparison;

import java.util.List;
import java.util.Objects;

/**
 * Colaborador compartido entre validar y publicar una versión nueva: calcula los avisos {@code REMOVED_IDS}
 * leyendo la versión anterior solo con {@link CourseReader} (sin reglas, §3.1) para que siga pudiendo
 * compararse aunque las reglas se endurezcan. Si ni el lector la entiende, no se puede comparar y no avisa.
 */
final class RemovedIdsCheck {

    private final CourseReader reader;

    RemovedIdsCheck(CourseReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    List<Issue> removedFrom(CourseVersion previous, CourseDocument next) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(next, "next");
        return switch (reader.read(previous.content().bytes())) {
            case CourseReadResult.Parsed parsed -> VersionComparison.compareVersions(parsed.course(), next);
            case CourseReadResult.TooLarge tooLarge -> List.of();
            case CourseReadResult.Unreadable unreadable -> List.of();
            case CourseReadResult.Invalid invalid -> List.of();
        };
    }
}
