package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Matching;
import es.ulpgc.pamn.dominio.backend.domain.course.MatchingPair;
import es.ulpgc.pamn.dominio.backend.domain.course.MultipleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.Option;
import es.ulpgc.pamn.dominio.backend.domain.course.SingleChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.multipleChoice;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.options;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.paths;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExerciseRulesTest {

    static final List<Function<CourseDocument, Stream<Issue>>> ALL_RULES = List.of(
            ExerciseRules::duplicateOptionIds, ExerciseRules::duplicateOptionTexts, ExerciseRules::unknownOptions,
            ExerciseRules::allOptionsCorrect, ExerciseRules::duplicateMatchingSides);

    /** El ejercicio queda en {@code /modules/0/steps/1}. */
    static CourseDocument withStep(Exercise exercise) {
        return course().module(0, m -> m.steps(lesson("representacion-l1"), exercise)).build();
    }

    /** El ejercicio queda en {@code /modules/1/review/0}. */
    static CourseDocument withReview(Exercise exercise) {
        return course().module(1, m -> m.review(exercise)).build();
    }

    static SingleChoice singleChoiceWith(List<Option> options, String answer) {
        return new SingleChoice("x", "Enunciado", options, answer, "Explicación");
    }

    static MultipleChoice multipleChoiceWith(List<Option> options, String... correct) {
        return new MultipleChoice("x", "Enunciado", options, List.of(correct), "Explicación");
    }

    static Matching matchingWith(MatchingPair... pairs) {
        return new Matching("x", "Enunciado", List.of(pairs), "Explicación");
    }

    @Test
    void theDefaultCourseBreaksNoExerciseRule() {
        CourseDocument course = course().build();

        ALL_RULES.forEach(rule -> assertEquals(List.of(), rule.apply(course).toList()));
    }

    @Test
    void repeatedOptionIdsAreReportedOnTheSecondOption() {
        List<Option> options = List.of(new Option("a", "Cola"), new Option("b", "Pila"), new Option("a", "Lista"));

        Issue issue = single(ExerciseRules.duplicateOptionIds(withStep(singleChoiceWith(options, "b"))));

        assertEquals(CourseIssueCode.DUPLICATE_OPTION_ID, issue.code());
        assertEquals("/modules/0/steps/1/options/2", issue.path().toString());
        assertTrue(issue.message().contains("«x»") && issue.message().contains("«a»"), issue.message());
    }

    @Test
    void optionsWithTheSameNormalizedTextAreReported() {
        List<Option> options = List.of(new Option("a", "Una cola"), new Option("b", "Pila"), new Option("c", " una  COLA "));

        assertEquals(List.of("/modules/1/review/0/options/2"),
                paths(ExerciseRules.duplicateOptionTexts(withReview(multipleChoiceWith(options, "a")))));
    }

    @Test
    void aSingleChoiceAnswerThatIsNoOptionIsReportedListingTheOptions() {
        Issue issue = single(ExerciseRules.unknownOptions(withStep(singleChoiceWith(options(3), "d"))));

        assertEquals(CourseIssueCode.UNKNOWN_OPTION, issue.code());
        assertEquals("/modules/0/steps/1/answer", issue.path().toString());
        assertTrue(issue.message().contains("«d»") && issue.message().contains("a, b, c"), issue.message());
    }

    @Test
    void eachUnknownCorrectIdIsReportedOnItsElement() {
        assertEquals(List.of("/modules/1/review/0/correct/1", "/modules/1/review/0/correct/2"),
                paths(ExerciseRules.unknownOptions(withReview(multipleChoiceWith(options(4), "a", "y", "z")))));
    }

    @Test
    void markingEveryOptionAsCorrectIsReported() {
        Issue issue = single(ExerciseRules.allOptionsCorrect(withReview(multipleChoice("x", 3, "c", "a", "b"))));

        assertEquals(CourseIssueCode.ALL_OPTIONS_CORRECT, issue.code());
        assertEquals("/modules/1/review/0/correct", issue.path().toString());
    }

    @Test
    void leavingOneOptionOutIsFine() {
        assertEquals(List.of(), ExerciseRules.allOptionsCorrect(withReview(multipleChoice("x", 3, "a", "b"))).toList());
    }

    @Test
    void repeatedLeftOrRightSidesAreReportedOnTheSecondPair() {
        Matching repeatedLeft = matchingWith(new MatchingPair("Grado", "1"), new MatchingPair("Cola", "2"),
                new MatchingPair("grado", "3"));
        Matching repeatedRight = matchingWith(new MatchingPair("A", "O(V²)"), new MatchingPair("B", "O(V²)"));

        assertEquals(List.of("/modules/0/steps/1/pairs/2"),
                paths(ExerciseRules.duplicateMatchingSides(withStep(repeatedLeft))));
        assertEquals(List.of("/modules/0/steps/1/pairs/1"),
                paths(ExerciseRules.duplicateMatchingSides(withStep(repeatedRight))));
    }

    @Test
    void theSameTextOnBothSidesOfAPairIsFine() {
        Matching mirrored = matchingWith(new MatchingPair("Cola", "Cola"), new MatchingPair("Pila", "Pila"));

        assertEquals(List.of(), ExerciseRules.duplicateMatchingSides(withStep(mirrored)).toList());
    }

    @Test
    void exercisesFromEveryModuleStepAndReviewAreChecked() {
        CourseDocument course = course()
                .module(0, m -> m.steps(lesson("l"), singleChoice("e1", 2)))
                .module(1, m -> m.review(singleChoiceWith(options(2), "zz")))
                .build();

        assertEquals(List.of("/modules/1/review/0/answer"), paths(ExerciseRules.unknownOptions(course)));
        assertEquals(CourseIssueCode.UNKNOWN_OPTION, single(ExerciseRules.unknownOptions(course)).code());
    }
}
