package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: publicar una versión nueva de un curso existente (§4.6). Primero el recurso y el permiso
 * (404, 403), después el cuerpo (decisión del paso 4, opción A); la comprobación definitiva la hace
 * {@link CourseRepository#appendVersion} de forma atómica.
 */
public final class PublishCourseVersion {

    private final CourseInspector inspector;
    private final RemovedIdsCheck removedIds;
    private final CourseRepository repository;
    private final Clock clock;

    /** @param reader para leer la versión anterior y avisar de los ids eliminados (solo el lector, §3.1) */
    public PublishCourseVersion(CourseInspector inspector, CourseReader reader, CourseRepository repository,
                                Clock clock) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
        this.removedIds = new RemovedIdsCheck(reader);
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public PublishVersionResult publish(UUID requesterId, UUID courseId, byte[] body) {
        Optional<CourseVersion> latest = repository.findLatest(courseId);
        if (latest.isEmpty()) {
            return new Publication.CourseNotFound(courseId);
        }
        if (!latest.get().summary().author().id().equals(requesterId)) {
            return new Publication.NotAuthor(courseId);
        }
        return switch (inspector.inspect(body)) {
            case Inspection.Accepted accepted -> append(requesterId, courseId, latest.get(), accepted);
            case Inspection.TooLarge tooLarge -> new Publication.TooLarge(tooLarge.report());
            case Inspection.Unreadable unreadable -> new Publication.Unreadable(unreadable.report());
            case Inspection.Rejected rejected -> new Publication.Rejected(rejected.report());
        };
    }

    private PublishVersionResult append(UUID requesterId, UUID courseId, CourseVersion latest,
                                        Inspection.Accepted accepted) {
        CourseReport report = accepted.report().with(removedIds.removedFrom(latest, accepted.course()));
        CourseVersionDraft draft = CourseVersionDraft.of(accepted.course(), accepted.content(), clock.instant());
        return switch (repository.appendVersion(courseId, requesterId, draft)) {
            case AppendVersionResult.Appended appended -> new Publication.Published(appended.course(), report);
            case AppendVersionResult.CourseNotFound notFound -> new Publication.CourseNotFound(courseId);
            case AppendVersionResult.NotAuthor notAuthor -> new Publication.NotAuthor(courseId);
            case AppendVersionResult.IdenticalToLatest identical -> new Publication.IdenticalToLatest(courseId);
        };
    }
}
