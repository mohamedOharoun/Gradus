package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.application.course.FakeCourseReader;
import es.ulpgc.pamn.dominio.backend.application.course.InMemoryCourseRepository;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;
import es.ulpgc.pamn.dominio.backend.domain.submission.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.COURSE_ID;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.PUBLISHED_AT;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.photo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitPhotoTest {

    static final Instant NOW = Instant.parse("2026-11-24T10:00:00.123456789Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID UNKNOWN_COURSE = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    /** Versión 1: un paso {@code photo} («repr-foto») en el primer módulo; el resto, el curso por defecto. */
    static final CourseDocument WITH_PHOTO =
            course().module(0, m -> m.steps(lesson("representacion-l1"), photo("repr-foto"))).build();

    private final InMemoryCourseRepository courses = new InMemoryCourseRepository();
    private final InMemorySubmissionRepository submissions = new InMemorySubmissionRepository();
    private final FakeCourseReader reader = new FakeCourseReader().parsing("v1", WITH_PHOTO);
    private final SubmitPhoto submitPhoto = new SubmitPhoto(courses, reader, submissions, CLOCK, () -> SUBMISSION_ID);

    SubmitPhotoTest() {
        courses.create(COURSE_ID, AUTHOR, CourseVersionDraft.of(WITH_PHOTO, new CourseContent("v1"), PUBLISHED_AT));
    }

    static byte[] jpeg(int size) {
        byte[] bytes = new byte[size];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }

    SubmitPhotoResult submit(UUID courseId, int version, String exerciseId, byte[] photo) {
        return submitPhoto.submit(new SubmitPhotoCommand(STUDENT, courseId, version, exerciseId, photo));
    }

    // ── Entregar bien ──

    @Test
    void aJpegForAPhotoStepIsSubmittedAsPending() {
        SubmitPhotoResult.Submitted submitted = assertInstanceOf(SubmitPhotoResult.Submitted.class,
                submit(COURSE_ID, 1, "repr-foto", jpeg(500)));

        assertEquals(new Submission(SUBMISSION_ID, STUDENT, COURSE_ID, 1, "repr-foto",
                NOW.truncatedTo(ChronoUnit.MICROS), SubmissionStatus.PENDING, 500),
                submitted.submission());
    }

    @Test
    void theSubmissionAndItsPhotoAreStored() {
        submit(COURSE_ID, 1, "repr-foto", jpeg(500));

        assertEquals(1, submissions.all().size());
        assertEquals(Jpeg.check(jpeg(500)), new JpegCheck.Valid(submissions.photoOf(SUBMISSION_ID)));
    }

    @Test
    void theTimeIsTruncatedToMicrosecondsLikePostgres() {
        // Como en RegisterUser: lo devuelto coincide con lo que se leerá de la base de datos.
        SubmitPhotoResult.Submitted submitted = assertInstanceOf(SubmitPhotoResult.Submitted.class,
                submit(COURSE_ID, 1, "repr-foto", jpeg(500)));

        assertEquals(Instant.parse("2026-11-24T10:00:00.123456Z"), submitted.submission().submittedAt());
    }

    @Test
    void submittingAgainIsANewSubmission() {
        // Decisión I: cada envío es una entrega nueva; cuenta la última.
        UUID[] ids = {SUBMISSION_ID, UUID.fromString("00000000-0000-0000-0000-0000000000a2")};
        int[] next = {0};
        SubmitPhoto twice = new SubmitPhoto(courses, reader, submissions, CLOCK, () -> ids[next[0]++]);

        twice.submit(new SubmitPhotoCommand(STUDENT, COURSE_ID, 1, "repr-foto", jpeg(500)));
        twice.submit(new SubmitPhotoCommand(STUDENT, COURSE_ID, 1, "repr-foto", jpeg(600)));

        assertEquals(List.of(500, 600), submissions.all().stream().map(Submission::photoBytes).toList());
    }

    // ── El curso o el ejercicio no valen ──

    @Test
    void anUnknownCourseIsNotFound() {
        assertInstanceOf(SubmitPhotoResult.CourseNotFound.class, submit(UNKNOWN_COURSE, 1, "repr-foto", jpeg(500)));
    }

    @Test
    void aVersionThatDoesNotExistIsNotFound() {
        assertInstanceOf(SubmitPhotoResult.CourseNotFound.class, submit(COURSE_ID, 2, "repr-foto", jpeg(500)));
        assertInstanceOf(SubmitPhotoResult.CourseNotFound.class, submit(COURSE_ID, 0, "repr-foto", jpeg(500)));
    }

    @Test
    void anExerciseThatDoesNotExistIsNotAPhotoExercise() {
        assertInstanceOf(SubmitPhotoResult.NotAPhotoExercise.class, submit(COURSE_ID, 1, "no-existe", jpeg(500)));
    }

    @Test
    void anExerciseOfAnotherTypeIsNotAPhotoExercise() {
        // El primer ejercicio del curso por defecto es de elección; «bfs» es un módulo, no un ejercicio.
        CourseDocument withChoice = course().build();
        reader.parsing("v1", withChoice);

        assertInstanceOf(SubmitPhotoResult.NotAPhotoExercise.class, submit(COURSE_ID, 1, firstExerciseId(withChoice), jpeg(500)));
        assertInstanceOf(SubmitPhotoResult.NotAPhotoExercise.class, submit(COURSE_ID, 1, "bfs", jpeg(500)));
    }

    @Test
    void aPhotoInTheReviewIsNotAccepted() {
        // El validador no deja publicarlo (PHOTO_IN_REVIEW), pero la versión se vuelve a leer solo con el
        // lector: si algún día llegase una, tampoco se aceptarían entregas fuera de steps (decisión D).
        reader.parsing("v1", course().module(1, m -> m.review(photo("bfs-foto"))).build());

        assertInstanceOf(SubmitPhotoResult.NotAPhotoExercise.class, submit(COURSE_ID, 1, "bfs-foto", jpeg(500)));
    }

    @Test
    void nothingIsStoredWhenTheExerciseIsWrong() {
        submit(COURSE_ID, 1, "no-existe", jpeg(500));

        assertEquals(List.of(), submissions.all());
    }

    // ── La foto no vale ──

    @Test
    void aPhotoThatIsNotAJpegIsInvalidAndSaysWhy() {
        SubmitPhotoResult.InvalidPhoto invalid = assertInstanceOf(SubmitPhotoResult.InvalidPhoto.class,
                submit(COURSE_ID, 1, "repr-foto", new byte[] {1, 2, 3, 4}));

        assertInstanceOf(JpegCheck.NotJpeg.class, invalid.reason());
    }

    @Test
    void aMissingPhotoIsInvalid() {
        SubmitPhotoResult.InvalidPhoto invalid = assertInstanceOf(SubmitPhotoResult.InvalidPhoto.class,
                submit(COURSE_ID, 1, "repr-foto", null));

        assertInstanceOf(JpegCheck.Empty.class, invalid.reason());
    }

    @Test
    void aPhotoTooLargeIsInvalidWithItsSize() {
        SubmitPhotoResult.InvalidPhoto invalid = assertInstanceOf(SubmitPhotoResult.InvalidPhoto.class,
                submit(COURSE_ID, 1, "repr-foto", jpeg(Jpeg.MAX_BYTES + 1)));

        assertEquals(new JpegCheck.TooLarge(Jpeg.MAX_BYTES + 1), invalid.reason());
        assertEquals(List.of(), submissions.all());
    }

    @Test
    void theCourseIsCheckedBeforeThePhoto() {
        // Si el ejercicio no existe, eso es lo que hay que contar, aunque la foto tampoco valga.
        assertInstanceOf(SubmitPhotoResult.CourseNotFound.class, submit(UNKNOWN_COURSE, 1, "repr-foto", null));
        assertInstanceOf(SubmitPhotoResult.NotAPhotoExercise.class, submit(COURSE_ID, 1, "no-existe", null));
    }

    // ── Un fallo del servidor no se disfraza ──

    @Test
    void aStoredVersionThatCannotBeReadIsAServerFault() {
        // Lo publicado ya pasó el lector: si ahora no se entiende, es un fallo nuestro (500), no del alumno.
        FakeCourseReader broken = new FakeCourseReader();
        SubmitPhoto withBrokenReader = new SubmitPhoto(courses, broken, submissions, CLOCK, () -> SUBMISSION_ID);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> withBrokenReader.submit(
                new SubmitPhotoCommand(STUDENT, COURSE_ID, 1, "repr-foto", jpeg(500))));
        assertTrue(error.getMessage().contains(COURSE_ID.toString()), error.getMessage());
    }

    static String firstExerciseId(CourseDocument course) {
        return course.modules().getFirst().steps().stream()
                .filter(step -> step instanceof Exercise)
                .findFirst().orElseThrow().id();
    }
}
