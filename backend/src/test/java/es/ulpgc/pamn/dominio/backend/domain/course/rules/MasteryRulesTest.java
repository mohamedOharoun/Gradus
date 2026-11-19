package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.reviewPool;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.paths;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasteryRulesTest {

    static final List<Function<CourseDocument, Stream<Issue>>> ALL_RULES = List.of(
            MasteryRules::minExceedsQuestions, MasteryRules::poolTooSmall, MasteryRules::poolSmallForRetry,
            MasteryRules::requiresPerfect, MasteryRules::guessable);

    /** Reserva de {@code size} ejercicios de respuesta única con 2 opciones: p = 1/2, fácil de adivinar. */
    static List<Exercise> coinFlips(String prefix, int size) {
        return IntStream.rangeClosed(1, size).<Exercise>mapToObj(i -> singleChoice(prefix + "-c" + i, 2)).toList();
    }

    @Test
    void theDefaultCourseBreaksNoMasteryRule() {
        CourseDocument course = course().build();

        ALL_RULES.forEach(rule -> assertEquals(List.of(), rule.apply(course).toList()));
    }

    @Test
    void moreRequiredHitsThanQuestionsIsReportedOncePerMasteryObject() {
        CourseDocument course = course().defaultMastery(3, 4).module(1, m -> m.mastery(2, 5)).build();

        List<Issue> issues = MasteryRules.minExceedsQuestions(course).toList();

        assertEquals(List.of("/defaults/mastery", "/modules/1/mastery"), paths(issues.stream()));
        assertTrue(issues.getFirst().isError());
        assertTrue(issues.get(1).message().contains("«bfs»"), issues.get(1).message());
    }

    @Test
    void needingEveryAnswerIsAWarningOncePerMasteryObject() {
        CourseDocument course = course().defaultMastery(4, 4).module(0, m -> m.mastery(5, 5)).build();

        List<Issue> issues = MasteryRules.requiresPerfect(course).toList();

        assertEquals(List.of("/defaults/mastery", "/modules/0/mastery"), paths(issues.stream()));
        assertEquals(Severity.WARNING, issues.getFirst().severity());
    }

    @Test
    void aReviewSmallerThanTheEffectiveQuestionsIsAnError() {
        CourseDocument course = course().module(1, m -> m.review(reviewPool("bfs", 3))).build();

        Issue issue = single(MasteryRules.poolTooSmall(course));

        assertEquals(CourseIssueCode.MASTERY_POOL_TOO_SMALL, issue.code());
        assertEquals("/modules/1/review", issue.path().toString());
        assertTrue(issue.message().contains("«bfs»") && issue.message().contains("4") && issue.message().contains("3"),
                issue.message());
    }

    @Test
    void theModuleOwnMasteryIsTheOneThatCounts() {
        // Con su propia prueba de 2 preguntas, 3 ejercicios bastan para montarla.
        CourseDocument course = course().module(1, m -> m.mastery(2, 2).review(reviewPool("bfs", 3))).build();

        assertEquals(List.of(), MasteryRules.poolTooSmall(course).toList());
    }

    @Test
    void aReviewSmallerThanTwiceTheQuestionsWarnsThatRetriesRepeat() {
        CourseDocument course = course().module(0, m -> m.review(reviewPool("representacion", 7))).build();

        Issue issue = single(MasteryRules.poolSmallForRetry(course));

        assertEquals(CourseIssueCode.MASTERY_POOL_SMALL_FOR_RETRY, issue.code());
        assertEquals("/modules/0/review", issue.path().toString());
    }

    @Test
    void aTestThatCanBePassedByGuessingIsAWarningWithTheBoundInPercent() {
        // q = 4, k = 3, p = 1/2: P(X ≥ 3) = 5/16 = 31,25 %.
        CourseDocument course = course().module(1, m -> m.review(coinFlips("bfs", 8))).build();

        Issue issue = single(MasteryRules.guessable(course));

        assertEquals(CourseIssueCode.MASTERY_GUESSABLE, issue.code());
        assertEquals("/modules/1/review", issue.path().toString());
        assertTrue(issue.message().contains("«bfs»") && issue.message().contains("31,3 %"), issue.message());
    }

    @Test
    void aPoolTooSmallSilencesTheMasteryWarningsOfItsModule() {
        // Opción A: 2 ejercicios fáciles de adivinar para una prueba de 4 preguntas solo dan el error.
        CourseDocument course = course().module(1, m -> m.review(coinFlips("bfs", 2))).build();

        assertEquals(List.of("/modules/1/review"), paths(MasteryRules.poolTooSmall(course)));
        assertEquals(List.of(), MasteryRules.poolSmallForRetry(course).toList());
        assertEquals(List.of(), MasteryRules.guessable(course).toList());
    }

    @Test
    void anInvalidDefaultSilencesOnlyTheModulesThatInheritIt() {
        // La prueba por defecto pide 4 aciertos de 3 preguntas; «bfs» tiene la suya propia, válida.
        CourseDocument course = course().defaultMastery(3, 4)
                .module(0, m -> m.review(coinFlips("representacion", 8)))
                .module(1, m -> m.mastery(4, 3).review(coinFlips("bfs", 8)))
                .build();

        assertEquals(List.of("/modules/1/review"), paths(MasteryRules.guessable(course)));
    }
}
