package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode.CYCLE;
import static es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode.DUPLICATE_TAG;
import static es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode.MASTERY_GUESSABLE;
import static es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode.MODULE_WITHOUT_LESSON;
import static es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode.REDUNDANT_PREREQUISITE;
import static es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode.SELF_PREREQUISITE;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.MasteryRulesTest.coinFlips;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.PrerequisiteGraphTest.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseValidatorTest {

    static final CourseValidator VALIDATOR = CourseValidator.standard();

    static List<CourseIssueCode> codes(CourseReport report) {
        return report.issues().stream().map(Issue::code).toList();
    }

    @Test
    void theDefaultTestCourseIsValidAndHasNoWarnings() {
        CourseReport report = VALIDATOR.validate(course().build());

        assertTrue(report.isValid());
        assertEquals(List.of(), report.issues());
    }

    @Test
    void anErrorInLayer3StopsLayers4And5() {
        // §6: un módulo que se requiere a sí mismo da SELF_PREREQUISITE y no CYCLE. Tampoco salen
        // los avisos de la capa 5 (MASTERY_GUESSABLE), aunque el módulo los merezca.
        CourseDocument course = course().module(1, m -> m.requires("bfs").review(coinFlips("bfs", 8))).build();

        CourseReport report = VALIDATOR.validate(course);

        assertEquals(List.of(SELF_PREREQUISITE), codes(report));
        assertFalse(report.isValid());
    }

    @Test
    void warningsOfLayer3DoNotStopAnything() {
        CourseDocument course = course().tags("grafos", "Grafos")
                .modules(node("a"), node("b", "a"), node("c", "a", "b"))
                .build();

        assertEquals(List.of(DUPLICATE_TAG, REDUNDANT_PREREQUISITE), codes(VALIDATOR.validate(course)));
    }

    @Test
    void layers4And5RunTogetherEvenIfLayer4HasErrors() {
        CourseDocument course = course()
                .modules(node("a", "b"), node("b", "a"))
                .module(1, m -> m.review(coinFlips("b", 8)))
                .build();

        assertEquals(List.of(CYCLE, MASTERY_GUESSABLE), codes(VALIDATOR.validate(course)));
    }

    @Test
    void issuesFromDifferentRulesComeOutInDocumentOrder() {
        // Las reglas los producen en otro orden: el grafo (módulo 2) antes que la pedagogía (módulo 0).
        CourseDocument course = course().tags("grafos", "Grafos")
                .modules(node("a"), node("b", "a"), node("c", "a", "b"))
                .module(0, m -> m.steps(singleChoice("a-f1", 3)))
                .build();

        assertEquals(List.of("/tags/1", "/modules/0/steps", "/modules/2/requires/0"),
                VALIDATOR.validate(course).issues().stream().map(issue -> issue.path().toString()).toList());
        assertEquals(List.of(DUPLICATE_TAG, MODULE_WITHOUT_LESSON, REDUNDANT_PREREQUISITE),
                codes(VALIDATOR.validate(course)));
    }

    @Test
    void thePipelineStopsAtTheFirstStageWithErrors() {
        CourseRule warning = course -> Stream.of(Issue.at(JsonPointer.root(), DUPLICATE_TAG, "Aviso."));
        CourseRule error = course -> Stream.of(Issue.at(JsonPointer.root(), CYCLE, "Error."));
        CourseRule never = course -> {
            throw new AssertionError("No debería ejecutarse");
        };

        CourseReport report = new CourseValidator(List.of(warning, error, never)).validate(course().build());

        assertEquals(List.of(DUPLICATE_TAG, CYCLE), codes(report));
    }
}
