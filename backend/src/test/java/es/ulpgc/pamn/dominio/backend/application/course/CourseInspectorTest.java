package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.CONTENT;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class CourseInspectorTest {

    static final byte[] BODY = {'{', '}'};

    /** Un lector de mentira: siempre devuelve {@code result}. {@code CourseReader} tiene un solo método. */
    static CourseReader readerReturning(CourseReadResult result) {
        return body -> result;
    }

    static CourseInspector inspectorReading(CourseReadResult result) {
        return new CourseInspector(readerReturning(result), CourseValidator.standard());
    }

    static CourseReport reportOf(CourseIssueCode code) {
        return new CourseReport(List.of(Issue.at(JsonPointer.root(), code, "Problema " + code)));
    }

    static List<CourseIssueCode> codes(CourseReport report) {
        return report.issues().stream().map(Issue::code).toList();
    }

    @Test
    void aValidCourseIsAcceptedWithItsContentAndAnEmptyReport() {
        CourseDocument course = course().build();

        Inspection.Accepted accepted = assertInstanceOf(Inspection.Accepted.class,
                inspectorReading(new CourseReadResult.Parsed(course, CONTENT)).inspect(BODY));

        assertSame(course, accepted.course());
        assertSame(CONTENT, accepted.content());
        assertEquals(List.of(), accepted.report().issues());
    }

    @Test
    void warningsDoNotPreventAcceptance() {
        CourseDocument course = course().tags("grafos", "Grafos").build();

        Inspection.Accepted accepted = assertInstanceOf(Inspection.Accepted.class,
                inspectorReading(new CourseReadResult.Parsed(course, CONTENT)).inspect(BODY));

        assertEquals(List.of(CourseIssueCode.DUPLICATE_TAG), codes(accepted.report()));
    }

    @Test
    void aCourseThatBreaksTheRulesIsRejected() {
        CourseDocument course = course().module(1, m -> m.requires("bfs")).build();

        Inspection.Rejected rejected = assertInstanceOf(Inspection.Rejected.class,
                inspectorReading(new CourseReadResult.Parsed(course, CONTENT)).inspect(BODY));

        assertEquals(List.of(CourseIssueCode.SELF_PREREQUISITE), codes(rejected.report()));
    }

    @Test
    void aStructureProblemIsAlsoRejected() {
        CourseReport schema = reportOf(CourseIssueCode.SCHEMA_VIOLATION);

        Inspection inspection = inspectorReading(new CourseReadResult.Invalid(schema)).inspect(BODY);

        assertEquals(new Inspection.Rejected(schema), inspection);
    }

    @Test
    void aSyntaxProblemIsUnreadable() {
        CourseReport syntax = reportOf(CourseIssueCode.INVALID_JSON);

        assertEquals(new Inspection.Unreadable(syntax),
                inspectorReading(new CourseReadResult.Unreadable(syntax)).inspect(BODY));
    }

    @Test
    void aBodyOverTheLimitIsTooLarge() {
        CourseReport tooLarge = reportOf(CourseIssueCode.PAYLOAD_TOO_LARGE);

        assertEquals(new Inspection.TooLarge(tooLarge),
                inspectorReading(new CourseReadResult.TooLarge(tooLarge)).inspect(BODY));
    }

    @Test
    void theBodyReachesTheReaderUntouched() {
        List<byte[]> received = new ArrayList<>();
        CourseReader spy = body -> {
            received.add(body);
            return new CourseReadResult.Unreadable(reportOf(CourseIssueCode.INVALID_JSON));
        };

        new CourseInspector(spy, CourseValidator.standard()).inspect(BODY);

        assertArrayEquals(BODY, received.getFirst());
    }
}
