package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.module;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.paths;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityRulesTest {

    @Test
    void theDefaultCourseHasNoRepeatedIdsOrTags() {
        CourseDocument course = course().build();

        assertEquals(List.of(), IdentityRules.duplicateIds(course).toList());
        assertEquals(List.of(), IdentityRules.duplicateTags(course).toList());
    }

    @Test
    void aRepeatedModuleIdIsReportedOnItsSecondAppearanceCitingTheFirst() {
        CourseDocument course = course().modules(module("grafos").build(), module("grafos").build()).build();

        // Los dos módulos repiten también sus lecciones y ejercicios (grafos-l1, grafos-r1…): se mira solo el módulo.
        Issue issue = IdentityRules.duplicateIds(course).toList().getFirst();

        assertEquals(CourseIssueCode.DUPLICATE_ID, issue.code());
        assertEquals("/modules/1", issue.path().toString());
        assertTrue(issue.message().contains("«grafos»"), issue.message());
        assertTrue(issue.message().contains("/modules/0"), issue.message());
    }

    @Test
    void modulesLessonsAndExercisesShareOneNamespace() {
        // Una lección del segundo módulo se llama igual que el primer módulo.
        CourseDocument course = course().module(1, m -> m.steps(lesson("representacion"), singleChoice("bfs-f1", 3)))
                .build();

        assertEquals(List.of("/modules/1/steps/0"), paths(IdentityRules.duplicateIds(course)));
    }

    @Test
    void reviewExercisesAreIncludedAndEveryLaterAppearanceIsReported() {
        CourseDocument course = course()
                .module(0, m -> m.review(singleChoice("x", 5), singleChoice("x", 5)))
                .module(1, m -> m.review(singleChoice("x", 5)))
                .build();

        assertEquals(List.of("/modules/0/review/1", "/modules/1/review/0"), paths(IdentityRules.duplicateIds(course)));
    }

    @Test
    void tagsThatMatchOnceNormalizedAreAWarningOnTheSecondOne() {
        CourseDocument course = course().tags("Grafos", "algoritmos", "grafos").build();

        Issue issue = single(IdentityRules.duplicateTags(course));

        assertEquals(CourseIssueCode.DUPLICATE_TAG, issue.code());
        assertEquals(Severity.WARNING, issue.severity());
        assertEquals("/tags/2", issue.path().toString());
        assertTrue(issue.message().contains("«grafos»") && issue.message().contains("«Grafos»"), issue.message());
    }

    @Test
    void tagsWithComposedAndDecomposedAccentsAreTheSame() {
        CourseDocument course = course().tags("teoría", "teoría").build();

        assertEquals(List.of("/tags/1"), paths(IdentityRules.duplicateTags(course)));
    }
}
