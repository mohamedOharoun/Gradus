package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.application.course.CourseReadResult;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReader;
import es.ulpgc.pamn.dominio.backend.application.course.CourseRepository;
import es.ulpgc.pamn.dominio.backend.application.course.FindCourse;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.PhotoExercise;
import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Caso de uso: entregar la foto de un ejercicio {@code photo} (docs/tareas/ejercicio-foto.md, paso 3b).
 * Orden: el curso y la versión, el ejercicio y por último la foto. Si lo que falla es el ejercicio, eso es
 * lo que hay que contar, aunque la foto tampoco valga.
 */
public final class SubmitPhoto {

    private final FindCourse findCourse;
    private final CourseReader reader;
    private final SubmissionRepository submissions;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public SubmitPhoto(CourseRepository courses, CourseReader reader, SubmissionRepository submissions, Clock clock,
                       Supplier<UUID> idGenerator) {
        // FindCourse ya sabe que una versión menor que 1 no existe: se reutiliza en vez de repetirlo.
        this.findCourse = new FindCourse(Objects.requireNonNull(courses, "courses"));
        this.reader = Objects.requireNonNull(reader, "reader");
        this.submissions = Objects.requireNonNull(submissions, "submissions");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public SubmitPhotoResult submit(SubmitPhotoCommand command) {
        Optional<CourseVersion> version = findCourse.version(command.courseId(), command.courseVersion());
        if (version.isEmpty()) {
            return new SubmitPhotoResult.CourseNotFound();
        }
        if (photoStep(read(version.get()), command.exerciseId()).isEmpty()) {
            return new SubmitPhotoResult.NotAPhotoExercise();
        }
        return switch (Jpeg.check(command.photo())) {
            case JpegCheck.Valid valid -> store(command, valid.jpeg());
            case JpegCheck rejected -> new SubmitPhotoResult.InvalidPhoto(rejected);
        };
    }

    private SubmitPhotoResult store(SubmitPhotoCommand command, Jpeg photo) {
        // PostgreSQL guarda microsegundos: truncamos para que lo devuelto coincida con lo guardado.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Submission submission = Submission.pending(idGenerator.get(), command.studentId(), command.courseId(),
                command.courseVersion(), command.exerciseId(), now, photo);
        submissions.add(submission, photo);
        return new SubmitPhotoResult.Submitted(submission);
    }

    /**
     * Lo publicado se vuelve a leer solo con el lector (como en {@code PublishCourseVersion}). Ya lo pasó al
     * publicarse, así que si ahora no se entiende es un fallo del servidor (500), no del alumno.
     */
    private CourseDocument read(CourseVersion version) {
        return switch (reader.read(version.content().bytes())) {
            case CourseReadResult.Parsed parsed -> parsed.course();
            case CourseReadResult rejected -> throw new IllegalStateException("No se puede leer la versión "
                    + version.summary().version() + " del curso " + version.summary().id()
                    + ", ya publicada: " + rejected);
        };
    }

    /** El ejercicio {@code photo} con ese id en los {@code steps} de algún módulo. Los de {@code review} no cuentan. */
    private static Optional<PhotoExercise> photoStep(CourseDocument course, String exerciseId) {
        return course.modules().stream()
                .flatMap(module -> module.steps().stream())
                .filter(step -> step instanceof PhotoExercise && step.id().equals(exerciseId))
                .map(PhotoExercise.class::cast)
                .findFirst();
    }
}
