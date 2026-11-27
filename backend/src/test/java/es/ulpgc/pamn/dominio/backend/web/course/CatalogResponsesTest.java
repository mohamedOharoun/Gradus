package es.ulpgc.pamn.dominio.backend.web.course;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.course.PageRequest;
import es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los cuerpos de la consulta (§4.4 y §4.5). */
class CatalogResponsesTest {

    static final ObjectMapper JSON = new ObjectMapper();

    /** Contenido con espacios y saltos de línea propios del autor: deben llegar tal cual. */
    static final String AUTHOR_TEXT = "{\n  \"formatVersion\": 1,   \"title\":\"Grafos\"\n}";

    static CourseVersion version() {
        CourseVersionDraft draft = CourseVersionDraft.of(PublishedCourses.DOCUMENT, new CourseContent(AUTHOR_TEXT),
                PublishedCourses.PUBLISHED_AT);
        return CourseVersion.of(PublishedCourses.COURSE_ID, 1, PublishedCourses.AUTHOR, draft);
    }

    @Test
    void aVersionIsItsMetadataPlusTheContentAsJson() throws JsonProcessingException {
        JsonNode json = JSON.readTree(JSON.writeValueAsString(CourseVersionResponse.from(version())));

        assertEquals("00000000-0000-0000-0000-0000000000c1", json.get("id").asText());
        assertEquals("Ana", json.get("author").get("name").asText());
        // «content» es un objeto JSON, no un texto con el JSON dentro.
        assertTrue(json.get("content").isObject());
        assertEquals("Grafos", json.get("content").get("title").asText());
    }

    @Test
    void theContentIsInsertedExactlyAsItWasPublished() throws JsonProcessingException {
        String json = JSON.writeValueAsString(CourseVersionResponse.from(version()));

        // Ni se reformatea ni se vuelve a serializar: los mismos caracteres, y por tanto los mismos bytes.
        assertTrue(json.endsWith(",\"content\":" + AUTHOR_TEXT + "}"), json);
    }

    @Test
    void aPageListsItsItemsWithItsPositionAndTheTotal() throws JsonProcessingException {
        CourseSummary summary = PublishedCourses.summary(1);
        Page<CourseSummary> page = new Page<>(List.of(summary), new PageRequest(2, 1), 37);

        assertEquals(JSON.readTree("""
                {"items": [%s], "page": 2, "size": 1, "total": 37}"""
                        .formatted(JSON.writeValueAsString(CourseResponse.from(summary)))),
                JSON.readTree(JSON.writeValueAsString(CoursePageResponse.from(page))));
    }

    @Test
    void aCourseListWrapsEachCourseResponseUnderCourses() throws JsonProcessingException {
        CourseSummary first = PublishedCourses.summary(1);
        CourseSummary second = PublishedCourses.summary(2);

        assertEquals(JSON.readTree("""
                {"courses": [%s, %s]}"""
                        .formatted(JSON.writeValueAsString(CourseResponse.from(first)),
                                JSON.writeValueAsString(CourseResponse.from(second)))),
                JSON.readTree(JSON.writeValueAsString(CourseListResponse.from(List.of(first, second)))));
    }

    @Test
    void anEmptyCourseListSerializesAsEmptyCoursesArray() throws JsonProcessingException {
        assertEquals(JSON.readTree("""
                {"courses": []}"""),
                JSON.readTree(JSON.writeValueAsString(CourseListResponse.from(List.of()))));
    }
}
