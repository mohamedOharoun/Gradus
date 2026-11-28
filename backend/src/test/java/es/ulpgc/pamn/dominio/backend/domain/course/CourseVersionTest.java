package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.AUTHOR;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.CONTENT;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.COURSE_ID;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.DOCUMENT;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.PUBLISHED_AT;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.draft;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.summary;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@link CourseAuthor}, {@link CourseVersionDraft}, {@link CourseSummary} y {@link CourseVersion}. */
class CourseVersionTest {

    @Test
    void anAuthorNeedsAnIdAndAName() {
        assertThrows(NullPointerException.class, () -> new CourseAuthor(null, "Ana"));
        assertThrows(IllegalArgumentException.class, () -> new CourseAuthor(UUID.randomUUID(), " "));
    }

    @Test
    void aDraftDerivesItsMetadataFromTheDocument() {
        assertEquals(CourseMetadata.of(DOCUMENT, CONTENT), draft().metadata());
    }

    @Test
    void aDraftTruncatesItsInstantToMicrosecondsAsPostgresqlStoresIt() {
        Instant withNanos = Instant.parse("2026-11-18T08:35:00.123456789Z");

        assertEquals(Instant.parse("2026-11-18T08:35:00.123456Z"),
                CourseVersionDraft.of(DOCUMENT, CONTENT, withNanos).publishedAt());
    }

    @Test
    void aDraftsMetadataMustDescribeItsOwnContent() {
        CourseMetadata otherContent = CourseMetadata.of(DOCUMENT, new CourseContent("{}"));

        assertThrows(IllegalArgumentException.class, () -> new CourseVersionDraft(otherContent, CONTENT, PUBLISHED_AT));
    }

    @Test
    void versionsStartAtOne() {
        assertDoesNotThrow(() -> summary(1));
        assertThrows(IllegalArgumentException.class, () -> summary(0));
    }

    @Test
    void aSummaryKeepsWhoPublishedWhatAndWhen() {
        CourseSummary summary = summary(2);

        assertEquals(COURSE_ID, summary.id());
        assertEquals(2, summary.version());
        assertEquals(AUTHOR, summary.author());
        assertEquals(PUBLISHED_AT, summary.publishedAt());
    }

    @Test
    void aDraftBecomesAConcreteVersionOfACourse() {
        CourseVersion version = CourseVersion.of(COURSE_ID, 3, AUTHOR, draft());

        assertEquals(new CourseSummary(COURSE_ID, 3, AUTHOR, PUBLISHED_AT, draft().metadata()), version.summary());
        assertEquals(CONTENT, version.content());
    }

    @Test
    void aVersionsContentMustMatchItsHash() {
        // Si no coincidieran, el contenido servido no sería el publicado (§4.1).
        assertDoesNotThrow(() -> new CourseVersion(summary(1), CONTENT));
        assertThrows(IllegalArgumentException.class, () -> new CourseVersion(summary(1), new CourseContent("{}")));
    }
}
