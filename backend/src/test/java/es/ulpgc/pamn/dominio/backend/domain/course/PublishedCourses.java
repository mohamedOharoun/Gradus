package es.ulpgc.pamn.dominio.backend.domain.course;

import java.time.Instant;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;

/** Piezas de publicación para los tests: un autor, un contenido y un borrador coherentes entre sí. */
public final class PublishedCourses {

    public static final UUID COURSE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    public static final CourseAuthor AUTHOR =
            new CourseAuthor(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Ana");
    public static final Instant PUBLISHED_AT = Instant.parse("2026-11-18T08:35:00Z");
    /** El texto no tiene por qué ser el JSON de {@link #DOCUMENT}: el dominio no lo vuelve a leer. */
    public static final CourseContent CONTENT = new CourseContent("{\"formatVersion\": 1}");
    public static final CourseDocument DOCUMENT = course().build();

    private PublishedCourses() {
    }

    public static CourseVersionDraft draft() {
        return CourseVersionDraft.of(DOCUMENT, CONTENT, PUBLISHED_AT);
    }

    public static CourseSummary summary(int version) {
        return new CourseSummary(COURSE_ID, version, AUTHOR, PUBLISHED_AT, draft().metadata());
    }
}
