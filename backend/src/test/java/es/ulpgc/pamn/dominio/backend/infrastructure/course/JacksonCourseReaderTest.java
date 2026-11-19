package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.node.ObjectNode;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReadResult;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReader;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Lesson;
import es.ulpgc.pamn.dominio.backend.domain.course.Mastery;
import es.ulpgc.pamn.dominio.backend.domain.course.Matching;
import es.ulpgc.pamn.dominio.backend.domain.course.MatchingPair;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;
import es.ulpgc.pamn.dominio.backend.domain.course.MultipleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.Option;
import es.ulpgc.pamn.dominio.backend.domain.course.SingleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.Step;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JacksonCourseReaderTest {

    static final CourseReader READER = new JacksonCourseReader(CourseSchemaTest.SCHEMA);

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    static byte[] utf8(ObjectNode tree) {
        return utf8(tree.toString());
    }

    static CourseDocument parsed(byte[] body) {
        return assertInstanceOf(CourseReadResult.Parsed.class, READER.read(body)).course();
    }

    static List<CourseIssueCode> codes(CourseReport report) {
        return report.issues().stream().map(Issue::code).toList();
    }

    @Test
    void theExampleBecomesTheDomainRecords() {
        CourseDocument course = parsed(ExampleCourse.bytes());

        assertEquals("Introducción a los grafos", course.title());
        assertEquals("es", course.language());
        assertEquals(List.of("grafos", "algoritmos", "estructuras de datos"), course.tags());
        assertEquals(new Mastery(4, 3), course.defaultMastery());
        assertEquals(List.of("representacion", "bfs"), course.modules().stream().map(Module::id).toList());

        Module bfs = course.modules().get(1);
        assertEquals(List.of("representacion"), bfs.requires());
        assertEquals(Optional.empty(), bfs.mastery());
        assertEquals(8, bfs.review().size());
    }

    @Test
    void everyKindOfStepKeepsItsFieldsAndItsOrder() {
        Module representacion = parsed(ExampleCourse.bytes()).modules().getFirst();
        List<Step> steps = representacion.steps();

        assertEquals(List.of(Lesson.class, Lesson.class, SingleChoice.class, Matching.class),
                steps.stream().map(Object::getClass).toList());
        SingleChoice f1 = (SingleChoice) steps.get(2);
        assertEquals("a", f1.answer());
        assertEquals(new Option("b", "Lista de adyacencia"), f1.options().get(1));
        assertEquals(new MatchingPair("Matriz de adyacencia", "O(V²)"), ((Matching) steps.get(3)).pairs().getFirst());
        assertEquals(List.of("a", "c"), ((MultipleChoice) representacion.review().get(3)).correct());
    }

    @Test
    void theMappedExampleIsValidAndHasNoWarnings() {
        // §6: «ejemplo-grafos.json es válido y no produce ningún aviso».
        assertEquals(List.of(), CourseValidator.standard().validate(parsed(ExampleCourse.bytes())).issues());
    }

    @Test
    void aModuleOwnMasteryIsMapped() {
        ObjectNode tree = ExampleCourse.tree();
        ((ObjectNode) tree.get("modules").get(1)).putObject("mastery").put("questions", 5).put("minCorrect", 4);

        assertEquals(Optional.of(new Mastery(5, 4)), parsed(utf8(tree)).modules().get(1).mastery());
    }

    @Test
    void storedContentReadsBackToTheSameCourse() {
        // §3.1: las versiones guardadas se leen solo con el lector.
        CourseReadResult.Parsed first = assertInstanceOf(CourseReadResult.Parsed.class, READER.read(ExampleCourse.bytes()));

        assertEquals(first.course(), parsed(first.content().bytes()));
    }

    @Test
    void aSyntaxErrorIsUnreadable() {
        CourseReadResult.Unreadable result = assertInstanceOf(CourseReadResult.Unreadable.class, READER.read(utf8("{")));

        assertEquals(List.of(CourseIssueCode.INVALID_JSON), codes(result.report()));
    }

    @Test
    void aBodyOverTheLimitIsTooLarge() {
        byte[] huge = utf8("\"" + "x".repeat(2_097_152) + "\"");

        CourseReadResult.TooLarge result = assertInstanceOf(CourseReadResult.TooLarge.class, READER.read(huge));

        assertEquals(List.of(CourseIssueCode.PAYLOAD_TOO_LARGE), codes(result.report()));
    }

    @Test
    void anUnsupportedFormatVersionIsReportedAloneWithoutRunningTheSchema() {
        // La propiedad «author» incumpliría el schema, pero el schema no llega a ejecutarse.
        ObjectNode tree = ExampleCourse.tree().put("formatVersion", 2).put("author", "Ana");

        CourseReadResult.Invalid result = assertInstanceOf(CourseReadResult.Invalid.class, READER.read(utf8(tree)));

        Issue issue = result.report().issues().getFirst();
        assertEquals(List.of(CourseIssueCode.UNSUPPORTED_FORMAT_VERSION), codes(result.report()));
        assertEquals("/formatVersion", issue.path().toString());
    }

    @Test
    void anIntegralDecimalVersionCountsAsAnInteger() {
        ObjectNode tree = ExampleCourse.tree().put("formatVersion", 2.0);

        CourseReadResult.Invalid result = assertInstanceOf(CourseReadResult.Invalid.class, READER.read(utf8(tree)));

        assertEquals(List.of(CourseIssueCode.UNSUPPORTED_FORMAT_VERSION), codes(result.report()));
    }

    @Test
    void aVersionThatIsNotAnIntegerIsLeftToTheSchema() {
        ObjectNode tree = ExampleCourse.tree().put("formatVersion", "1");

        CourseReadResult.Invalid result = assertInstanceOf(CourseReadResult.Invalid.class, READER.read(utf8(tree)));

        assertEquals(List.of(CourseIssueCode.SCHEMA_VIOLATION), codes(result.report()));
        assertEquals("/formatVersion", result.report().issues().getFirst().path().toString());
    }

    @Test
    void schemaViolationsAreAllReportedInDocumentOrder() {
        ObjectNode tree = ExampleCourse.tree().put("author", "Ana").put("title", "Grafos\n");
        ((ObjectNode) tree.get("modules").get(1).get("review").get(0)).remove("explanation");

        CourseReadResult.Invalid result = assertInstanceOf(CourseReadResult.Invalid.class, READER.read(utf8(tree)));

        assertEquals(List.of("", "/title", "/modules/1/review/0"),
                result.report().issues().stream().map(issue -> issue.path().toString()).toList());
    }
}
