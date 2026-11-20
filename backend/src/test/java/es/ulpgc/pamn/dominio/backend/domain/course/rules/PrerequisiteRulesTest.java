package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.paths;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.single;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PrerequisiteRulesTest {

    @Test
    void theDefaultCourseHasValidPrerequisites() {
        CourseDocument course = course().build();

        assertEquals(List.of(), PrerequisiteRules.unknownPrerequisites(course).toList());
        assertEquals(List.of(), PrerequisiteRules.selfPrerequisites(course).toList());
    }

    @Test
    void anUnknownPrerequisiteIsReportedWithTheMessageOfTheSpecification() {
        CourseDocument course = course().module(1, m -> m.requires("colas")).build();

        Issue issue = single(PrerequisiteRules.unknownPrerequisites(course));

        assertEquals(CourseIssueCode.UNKNOWN_PREREQUISITE, issue.code());
        assertEquals("/modules/1/requires/0", issue.path().toString());
        assertEquals("El módulo «bfs» requiere «colas», que no es un módulo de este curso. "
                + "Módulos existentes: representacion, bfs.", issue.message());
    }

    @Test
    void aLessonOrExerciseIdIsNotAModule() {
        CourseDocument course = course().module(1, m -> m.requires("representacion", "representacion-l1")).build();

        assertEquals(List.of("/modules/1/requires/1"), paths(PrerequisiteRules.unknownPrerequisites(course)));
    }

    @Test
    void aModuleRequiringItselfIsASelfPrerequisiteNotAnUnknownOne() {
        CourseDocument course = course().module(1, m -> m.requires("representacion", "bfs")).build();

        Issue issue = single(PrerequisiteRules.selfPrerequisites(course));

        assertEquals(CourseIssueCode.SELF_PREREQUISITE, issue.code());
        assertEquals("/modules/1/requires/1", issue.path().toString());
        assertEquals(List.of(), PrerequisiteRules.unknownPrerequisites(course).toList());
    }
}
