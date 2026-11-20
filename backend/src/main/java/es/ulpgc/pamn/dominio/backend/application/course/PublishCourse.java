package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Caso de uso: publicar un curso nuevo, que se guarda como su versión 1 (§4.6). */
public final class PublishCourse {

    private final CourseInspector inspector;
    private final CourseRepository repository;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public PublishCourse(CourseInspector inspector, CourseRepository repository, Clock clock,
                         Supplier<UUID> idGenerator) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    /** El autor es siempre el usuario de la sesión; nunca se lee del cuerpo (§4.3). */
    public PublishResult publish(CourseAuthor author, byte[] body) {
        return switch (inspector.inspect(body)) {
            case Inspection.Accepted accepted -> new Publication.Published(
                    repository.create(idGenerator.get(), author,
                            CourseVersionDraft.of(accepted.course(), accepted.content(), clock.instant())),
                    accepted.report());
            case Inspection.TooLarge tooLarge -> new Publication.TooLarge(tooLarge.report());
            case Inspection.Unreadable unreadable -> new Publication.Unreadable(unreadable.report());
            case Inspection.Rejected rejected -> new Publication.Rejected(rejected.report());
        };
    }
}
