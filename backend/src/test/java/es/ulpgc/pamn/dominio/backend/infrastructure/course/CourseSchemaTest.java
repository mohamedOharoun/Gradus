package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.node.ObjectNode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseSchemaTest {

    /** Se carga una sola vez, como en producción: compilar el schema es lo caro. */
    static final CourseSchema SCHEMA = CourseSchema.load();

    static ObjectNode firstStep(ObjectNode course) {
        return (ObjectNode) course.get("modules").get(0).get("steps").get(0);
    }

    static Issue single(List<Issue> issues) {
        assertEquals(1, issues.size(), issues.toString());
        return issues.getFirst();
    }

    @Test
    void theExampleSatisfiesTheSameSchemaFileAsProduction() {
        // §6: «Schema y ejemplo sincronizados».
        assertEquals(List.of(), SCHEMA.check(ExampleCourse.tree()));
    }

    @Test
    void aTitleEndingInALineBreakIsRejected() {
        // §2.7: los patrones acaban en (?![\s\S]) y no en $, que en Java aceptaría el salto final.
        ObjectNode course = ExampleCourse.tree().put("title", "Introducción a los grafos\n");

        Issue issue = single(SCHEMA.check(course));

        assertEquals(CourseIssueCode.SCHEMA_VIOLATION, issue.code());
        assertEquals("/title", issue.path().toString());
        assertTrue(issue.message().contains("una sola línea"), issue.message());
    }

    @Test
    void anUnknownPropertyIsReportedOnItsObject() {
        // §2.8: el autor no escribe autor, id ni versión; additionalProperties: false lo rechaza.
        ObjectNode course = ExampleCourse.tree().put("author", "Ana");

        Issue issue = single(SCHEMA.check(course));

        assertEquals("", issue.path().toString());
        assertTrue(issue.message().contains("'author'"), issue.message());
    }

    @Test
    void aMissingPropertyIsReportedOnItsObjectNamingIt() {
        ObjectNode course = ExampleCourse.tree();
        ((ObjectNode) course.get("modules").get(1).get("review").get(0)).remove("explanation");

        Issue issue = single(SCHEMA.check(course));

        assertEquals("/modules/1/review/0", issue.path().toString());
        assertTrue(issue.message().contains("'explanation'"), issue.message());
    }

    @Test
    void malformedIdsAndLanguagesGetAReadableMessageWithTheValue() {
        ObjectNode course = ExampleCourse.tree().put("language", "Spanish");
        firstStep(course).put("id", "Repr");

        List<Issue> issues = SCHEMA.check(course);

        Issue language = issues.stream().filter(issue -> issue.path().toString().equals("/language")).findFirst().orElseThrow();
        Issue id = issues.stream().filter(issue -> issue.path().toString().equals("/modules/0/steps/0/id")).findFirst().orElseThrow();
        assertTrue(language.message().contains("«Spanish»") && language.message().contains("es-ES"), language.message());
        assertTrue(id.message().contains("«Repr»") && id.message().contains("minúsculas"), id.message());
    }

    @Test
    void everyViolationIsReportedWithItsOwnPath() {
        ObjectNode course = ExampleCourse.tree().put("title", " Grafos");
        course.withArray("tags").removeAll();
        ((ObjectNode) course.get("modules").get(1).get("review").get(1)).put("type", "essay");

        Set<String> paths = SCHEMA.check(course).stream().map(issue -> issue.path().toString()).collect(Collectors.toSet());

        assertEquals(Set.of("/title", "/tags", "/modules/1/review/1/type"), paths);
    }
}
