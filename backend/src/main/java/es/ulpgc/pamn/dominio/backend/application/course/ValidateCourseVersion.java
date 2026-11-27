package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: validar una nueva versión de un curso existente sin guardarla. Primero comprueba el recurso y
 * el permiso (404, 403) antes de inspeccionar el cuerpo; si es válido, detecta duplicados contra la última
 * versión (409) y añade el aviso {@code REMOVED_IDS} sin modificar nada en el repositorio.
 */
public final class ValidateCourseVersion {

    private final CourseInspector inspector;
    private final RemovedIdsCheck removedIds;
    private final CourseRepository repository;

    /** @param reader para leer la versión anterior y avisar de los ids eliminados (solo el lector, §3.1) */
    public ValidateCourseVersion(CourseInspector inspector, CourseReader reader, CourseRepository repository) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
        this.removedIds = new RemovedIdsCheck(reader);
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public VersionValidationResult validate(UUID requesterId, UUID courseId, byte[] body) {
        Objects.requireNonNull(requesterId, "requesterId");
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(body, "body");

        Optional<CourseVersion> latest = repository.findLatest(courseId);
        if (latest.isEmpty()) {
            return new VersionValidationResult.CourseNotFound(courseId);
        }
        if (!latest.get().summary().author().id().equals(requesterId)) {
            return new VersionValidationResult.NotAuthor(courseId);
        }
        return switch (inspector.inspect(body)) {
            case Inspection.TooLarge tooLarge -> new VersionValidationResult.TooLarge(tooLarge.report());
            case Inspection.Unreadable unreadable -> new VersionValidationResult.Checked(unreadable.report());
            case Inspection.Rejected rejected -> new VersionValidationResult.Checked(rejected.report());
            case Inspection.Accepted accepted -> compareWithLatest(courseId, latest.get(), accepted);
        };
    }

    private VersionValidationResult compareWithLatest(UUID courseId, CourseVersion latest,
                                                      Inspection.Accepted accepted) {
        if (latest.content().sha256().equals(accepted.content().sha256())) {
            return new VersionValidationResult.IdenticalToLatest(courseId);
        }
        CourseReport report = accepted.report().with(removedIds.removedFrom(latest, accepted.course()));
        return new VersionValidationResult.Checked(report);
    }
}
