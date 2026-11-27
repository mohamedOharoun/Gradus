package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListMyCoursesTest {

    static final CourseAuthor OTHER_AUTHOR =
            new CourseAuthor(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Joel");

    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private final ListMyCourses listMyCourses = new ListMyCourses(repository);

    CourseSummary publish(UUID courseId, CourseAuthor author, String title, String content, String publishedAt) {
        CourseVersionDraft draft = CourseVersionDraft.of(
                course().title(title).build(),
                new CourseContent(content),
                Instant.parse(publishedAt));
        return repository.create(courseId, author, draft);
    }

    CourseSummary publishVersion(UUID courseId, CourseAuthor author, String title, String content, String publishedAt) {
        CourseVersionDraft draft = CourseVersionDraft.of(
                course().title(title).build(),
                new CourseContent(content),
                Instant.parse(publishedAt));
        AppendVersionResult result = repository.appendVersion(courseId, author.id(), draft);
        if (result instanceof AppendVersionResult.Appended appended) {
            return appended.course();
        }
        throw new AssertionError("No se pudo añadir la versión: " + result);
    }

    @Test
    void returnsAuthorsCoursesOrderedByPublishedAtDescending() {
        UUID oldest = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID newest = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID middle = UUID.fromString("00000000-0000-0000-0000-000000000003");
        CourseSummary first = publish(oldest, AUTHOR, "Primero", "c1", "2026-11-20T10:00:00Z");
        CourseSummary third = publish(newest, AUTHOR, "Tercero", "c3", "2026-11-22T10:00:00Z");
        CourseSummary second = publish(middle, AUTHOR, "Segundo", "c2", "2026-11-21T10:00:00Z");

        assertEquals(List.of(third, second, first), listMyCourses.list(AUTHOR.id()));
    }

    @Test
    void returnsOnlyTheLatestVersionOfEachCourseAndOrdersByItsPublishedAt() {
        UUID updatedCourse = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID otherCourse = UUID.fromString("00000000-0000-0000-0000-000000000002");
        publish(updatedCourse, AUTHOR, "Grafos v1", "v1", "2026-11-20T10:00:00Z");
        CourseSummary other = publish(otherCourse, AUTHOR, "Árboles", "other", "2026-11-21T10:00:00Z");
        CourseSummary latest = publishVersion(updatedCourse, AUTHOR, "Grafos v2", "v2", "2026-11-22T10:00:00Z");

        assertEquals(List.of(latest, other), listMyCourses.list(AUTHOR.id()));
        assertEquals(2, listMyCourses.list(AUTHOR.id()).getFirst().version());
    }

    @Test
    void breaksTiesByIdAscendingAsTextMatchingPostgresUuidOrder() {
        // 80000000-... tiene el bit de signo a 1: UUID.compareTo lo pondría antes que 7fffffff-...,
        // pero en PostgreSQL (orden byte a byte) y como texto va después.
        UUID highBitId = UUID.fromString("80000000-0000-0000-0000-000000000001");
        UUID lowBitId = UUID.fromString("7fffffff-0000-0000-0000-000000000001");
        String sameInstant = "2026-11-22T10:00:00Z";
        CourseSummary second = publish(highBitId, AUTHOR, "Segundo", "c-high", sameInstant);
        CourseSummary first = publish(lowBitId, AUTHOR, "Primero", "c-low", sameInstant);

        assertEquals(List.of(first, second), listMyCourses.list(AUTHOR.id()));
    }

    @Test
    void returnsEmptyListWhenAuthorHasNoCourses() {
        assertEquals(List.of(), listMyCourses.list(AUTHOR.id()));
    }

    @Test
    void excludesCoursesFromOtherAuthors() {
        UUID mineId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID otherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        CourseSummary mine = publish(mineId, AUTHOR, "Mío", "mine", "2026-11-20T10:00:00Z");
        publish(otherId, OTHER_AUTHOR, "De otro", "other", "2026-11-21T10:00:00Z");

        assertEquals(List.of(mine), listMyCourses.list(AUTHOR.id()));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new ListMyCourses(null));
        assertThrows(NullPointerException.class, () -> listMyCourses.list(null));
    }
}
