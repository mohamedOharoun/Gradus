package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.codes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseRuleTest {

    static CourseRule reporting(CourseIssueCode code) {
        return course -> Stream.of(Issue.at(JsonPointer.root(), code, "Problema " + code));
    }

    @Test
    void allRunsEveryRuleInOrderAsIfItWereOne() {
        CourseRule both = CourseRule.all(reporting(CourseIssueCode.CYCLE), reporting(CourseIssueCode.DUPLICATE_TAG));

        assertEquals(List.of(CourseIssueCode.CYCLE, CourseIssueCode.DUPLICATE_TAG), codes(both.check(course().build())));
    }

    @Test
    void allOfNothingFindsNothing() {
        CourseDocument course = course().build();

        assertEquals(List.of(), codes(CourseRule.all().check(course)));
    }
}
