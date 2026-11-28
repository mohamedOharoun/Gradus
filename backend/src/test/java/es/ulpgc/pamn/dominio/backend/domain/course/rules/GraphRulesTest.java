package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;
import es.ulpgc.pamn.dominio.backend.domain.course.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.paths;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.single;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.PrerequisiteGraphTest.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphRulesTest {

    static CourseDocument courseWith(Module... modules) {
        return course().modules(modules).build();
    }

    @Test
    void theDefaultCourseHasNoGraphIssues() {
        CourseDocument course = course().build();

        assertEquals(List.of(), GraphRules.cycles(course).toList());
        assertEquals(List.of(), GraphRules.redundantPrerequisites(course).toList());
    }

    @Test
    void aCycleIsReportedOnceWithTheConcreteCycleInTheMessage() {
        CourseDocument course = courseWith(
                node("representacion", "camino"), node("bfs", "representacion"), node("camino", "bfs"));

        Issue issue = single(GraphRules.cycles(course));

        assertEquals(CourseIssueCode.CYCLE, issue.code());
        assertEquals("/modules/0", issue.path().toString());
        assertEquals("Los prerrequisitos forman un ciclo: representacion requiere camino, que requiere bfs, "
                + "que requiere representacion.", issue.message());
    }

    @Test
    void theCycleIsReportedOnTheModuleWhereItStarts() {
        CourseDocument course = courseWith(node("a", "b"), node("b", "c"), node("c", "b"));

        assertEquals(List.of("/modules/1"), paths(GraphRules.cycles(course)));
    }

    @Test
    void aPrerequisiteAlreadyReachedThroughAnotherIsRedundant() {
        CourseDocument course = courseWith(
                node("representacion"), node("bfs", "representacion"), node("camino", "representacion", "bfs"));

        Issue issue = single(GraphRules.redundantPrerequisites(course));

        assertEquals(CourseIssueCode.REDUNDANT_PREREQUISITE, issue.code());
        assertEquals(Severity.WARNING, issue.severity());
        assertEquals("/modules/2/requires/0", issue.path().toString());
        assertTrue(issue.message().contains("«representacion»") && issue.message().contains("«bfs»"),
                issue.message());
    }

    @Test
    void redundancyIsFoundThroughLongerChains() {
        CourseDocument course = courseWith(node("a"), node("b", "a"), node("c", "b"), node("d", "c", "a"));

        Issue issue = single(GraphRules.redundantPrerequisites(course));

        assertEquals("/modules/3/requires/1", issue.path().toString());
        assertTrue(issue.message().contains("c requiere b, que requiere a"), issue.message());
    }

    @Test
    void independentPrerequisitesAreNotRedundant() {
        CourseDocument course = courseWith(node("a"), node("b"), node("c", "a", "b"));

        assertEquals(List.of(), GraphRules.redundantPrerequisites(course).toList());
    }

    @Test
    void redundancyIsNotCheckedWhenThereIsACycle() {
        // §3.5: aristas redundantes «solo si no hay ciclos».
        CourseDocument course = courseWith(node("a", "b"), node("b", "a"), node("c", "a", "b"));

        assertEquals(List.of(), GraphRules.redundantPrerequisites(course).toList());
    }
}
