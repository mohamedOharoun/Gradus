package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.BODY;
import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.codes;
import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.inspectorReading;
import static es.ulpgc.pamn.dominio.backend.application.course.CourseInspectorTest.reportOf;
import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.CONTENT;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateCourseTest {

    static ValidateCourse validatingAs(CourseReadResult result) {
        return new ValidateCourse(inspectorReading(result));
    }

    @Test
    void aValidCourseGivesAValidReport() {
        CourseValidationResult.Checked checked = assertInstanceOf(CourseValidationResult.Checked.class,
                validatingAs(new CourseReadResult.Parsed(course().build(), CONTENT)).validate(BODY));

        assertTrue(checked.report().isValid());
    }

    @Test
    void anInvalidCourseIsStillAnAnswerWithTheReport() {
        // §4.3: validar responde 200 «sea válido o no»; los errores van en el informe.
        CourseReport syntax = reportOf(CourseIssueCode.INVALID_JSON);

        CourseValidationResult result = validatingAs(new CourseReadResult.Unreadable(syntax)).validate(BODY);

        assertEquals(new CourseValidationResult.Checked(syntax), result);
        assertFalse(syntax.isValid());
    }

    @Test
    void warningsAndErrorsOfTheRulesAreReported() {
        CourseValidationResult.Checked checked = assertInstanceOf(CourseValidationResult.Checked.class,
                validatingAs(new CourseReadResult.Parsed(course().module(1, m -> m.requires("bfs")).build(), CONTENT))
                        .validate(BODY));

        assertEquals(List.of(CourseIssueCode.SELF_PREREQUISITE), codes(checked.report()));
    }

    @Test
    void aBodyOverTheLimitIsTheOnlyDifferentAnswer() {
        CourseReport tooLarge = reportOf(CourseIssueCode.PAYLOAD_TOO_LARGE);

        assertEquals(new CourseValidationResult.TooLarge(tooLarge),
                validatingAs(new CourseReadResult.TooLarge(tooLarge)).validate(BODY));
    }
}
