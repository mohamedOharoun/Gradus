package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.matching;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.reviewPool;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.paths;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PedagogyRulesTest {

    @Test
    void theDefaultCourseBreaksNoPedagogyRule() {
        CourseDocument course = course().build();

        assertEquals(List.of(), PedagogyRules.modulesWithoutLesson(course).toList());
        assertEquals(List.of(), PedagogyRules.modulesWithoutExercise(course).toList());
        assertEquals(List.of(), PedagogyRules.matchingsWithTooManyPairs(course).toList());
    }

    @Test
    void stepsWithoutAnyLessonAreAWarning() {
        CourseDocument course = course().module(1, m -> m.steps(singleChoice("bfs-f1", 3))).build();

        Issue issue = single(PedagogyRules.modulesWithoutLesson(course));

        assertEquals(CourseIssueCode.MODULE_WITHOUT_LESSON, issue.code());
        assertEquals(Severity.WARNING, issue.severity());
        assertEquals("/modules/1/steps", issue.path().toString());
        assertTrue(issue.message().contains("«bfs»"), issue.message());
    }

    @Test
    void stepsWithoutAnyExerciseAreAWarning() {
        CourseDocument course = course().module(0, m -> m.steps(lesson("representacion-l1"))).build();

        Issue issue = single(PedagogyRules.modulesWithoutExercise(course));

        assertEquals(CourseIssueCode.MODULE_WITHOUT_EXERCISE, issue.code());
        assertEquals("/modules/0/steps", issue.path().toString());
    }

    @Test
    void moreThanSixPairsIsAWarningWhereverTheMatchingIs() {
        CourseDocument course = course()
                .module(0, m -> m.steps(lesson("l"), matching("muchos", 7)))
                .module(1, m -> m.review(matching("seis", 6), matching("siete", 7), singleChoice("x", 5)))
                .build();

        assertEquals(List.of("/modules/0/steps/1/pairs", "/modules/1/review/1/pairs"),
                paths(PedagogyRules.matchingsWithTooManyPairs(course)));
    }

    @Test
    void reviewExercisesDoNotCountAsFormativeExercises() {
        // La reserva es la prueba de dominio: no sustituye a los ejercicios intercalados con las lecciones.
        CourseDocument course = course().module(1, m -> m.steps(lesson("bfs-l1")).review(reviewPool("bfs", 8))).build();

        assertEquals(List.of("/modules/1/steps"), paths(PedagogyRules.modulesWithoutExercise(course)));
    }
}
