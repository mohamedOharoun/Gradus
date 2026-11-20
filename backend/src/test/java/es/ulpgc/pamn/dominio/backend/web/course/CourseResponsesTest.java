package es.ulpgc.pamn.dominio.backend.web.course;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;
import es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los cuerpos JSON de §3.7 y §4.4: son el contrato con la app y con los autores. */
class CourseResponsesTest {

    static final ObjectMapper JSON = new ObjectMapper();

    static final Issue ERROR = Issue.at(JsonPointer.root().key("modules").index(1).key("requires").index(0),
            CourseIssueCode.UNKNOWN_PREREQUISITE, "El módulo «bfs» requiere «colas».");
    static final Issue WARNING = Issue.at(JsonPointer.root().key("tags").index(1),
            CourseIssueCode.DUPLICATE_TAG, "Etiqueta repetida.");

    @Test
    void theReportListsErrorsAndWarningsSeparately() throws JsonProcessingException {
        CourseReport report = new CourseReport(List.of(WARNING, ERROR));

        assertEquals("""
                {"valid":false,\
                "errors":[{"code":"UNKNOWN_PREREQUISITE","path":"/modules/1/requires/0",\
                "message":"El módulo «bfs» requiere «colas»."}],\
                "warnings":[{"code":"DUPLICATE_TAG","path":"/tags/1","message":"Etiqueta repetida."}]}""",
                JSON.writeValueAsString(CourseReportResponse.from(report)));
    }

    @Test
    void aReportWithoutErrorsIsValid() throws JsonProcessingException {
        assertEquals("{\"valid\":true,\"errors\":[],\"warnings\":[]}",
                JSON.writeValueAsString(CourseReportResponse.from(CourseReport.EMPTY)));
    }

    @Test
    void theMetadataOfAVersionHaveTheFieldsOfTheSpecification() throws JsonProcessingException {
        String sha256 = PublishedCourses.CONTENT.sha256();

        assertEquals("""
                {"id":"00000000-0000-0000-0000-0000000000c1","version":2,\
                "author":{"id":"00000000-0000-0000-0000-000000000001","name":"Ana"},\
                "publishedAt":"2026-11-18T08:35:00Z","formatVersion":1,\
                "title":"Introducción a los grafos",\
                "description":"Cómo se guarda un grafo en memoria y cómo se recorre en anchura.",\
                "language":"es","tags":["grafos","algoritmos"],"moduleCount":2,\
                "sha256":"%s"}""".formatted(sha256),
                JSON.writeValueAsString(CourseResponse.from(PublishedCourses.summary(2))));
    }

    @Test
    void aPublishedCourseComesWithItsWarnings() throws JsonProcessingException {
        String json = JSON.writeValueAsString(PublishedCourseResponse.from(PublishedCourses.summary(1),
                new CourseReport(List.of(WARNING))));

        assertEquals(JSON.readTree("""
                {"course": %s, "warnings": [{"code":"DUPLICATE_TAG","path":"/tags/1","message":"Etiqueta repetida."}]}"""
                        .formatted(JSON.writeValueAsString(CourseResponse.from(PublishedCourses.summary(1))))),
                JSON.readTree(json));
    }

    @Test
    void theSearchFieldsNeverLeaveTheServer() throws JsonProcessingException {
        String json = JSON.writeValueAsString(CourseResponse.from(PublishedCourses.summary(1)));

        assertEquals(false, json.contains("searchText") || json.contains("searchTags") || json.contains("sizeBytes"));
    }
}
