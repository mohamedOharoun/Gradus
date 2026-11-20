package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.CONTENT;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseMetadataTest {

    @Test
    void isDerivedFromTheDocumentAndItsContent() {
        CourseDocument document = course().tags("Grafos", "Estructuras  de datos").build();

        CourseMetadata metadata = CourseMetadata.of(document, CONTENT);

        assertEquals(1, metadata.formatVersion());
        assertEquals("Introducción a los grafos", metadata.title());
        assertEquals(document.description(), metadata.description());
        assertEquals("es", metadata.language());
        assertEquals(List.of("Grafos", "Estructuras  de datos"), metadata.tags());
        assertEquals(2, metadata.moduleCount());
        assertEquals(CONTENT.sha256(), metadata.sha256());
        assertEquals(CONTENT.sizeBytes(), metadata.sizeBytes());
    }

    @Test
    void searchFieldsAreNormalizedWhileTheOriginalsAreKeptAsWritten() {
        CourseDocument document = course().title("Introducción a los GRAFOS").description("Cómo  se recorren.")
                .tags("Grafos", "Estructuras  de datos").build();

        CourseMetadata metadata = CourseMetadata.of(document, CONTENT);

        assertEquals(List.of("grafos", "estructuras de datos"), metadata.searchTags());
        assertEquals("introducción a los grafos cómo se recorren.", metadata.searchText());
        assertEquals("Introducción a los GRAFOS", metadata.title());
    }

    @Test
    void everyTagHasItsSearchTag() {
        CourseMetadata valid = CourseMetadata.of(course().build(), CONTENT);

        assertThrows(IllegalArgumentException.class, () -> new CourseMetadata(valid.formatVersion(), valid.title(),
                valid.description(), valid.language(), valid.tags(), List.of("grafos"), valid.searchText(),
                valid.moduleCount(), valid.sha256(), valid.sizeBytes()));
    }

    @Test
    void theHashMustBeLowercaseHexSha256() {
        CourseMetadata valid = CourseMetadata.of(course().build(), CONTENT);

        assertThrows(IllegalArgumentException.class, () -> new CourseMetadata(valid.formatVersion(), valid.title(),
                valid.description(), valid.language(), valid.tags(), valid.searchTags(), valid.searchText(),
                valid.moduleCount(), valid.sha256().toUpperCase(), valid.sizeBytes()));
    }
}
