package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.MatchingTest.pairs;
import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.EXPLANATION;
import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.PROMPT;
import static es.ulpgc.pamn.dominio.backend.domain.course.SingleChoiceTest.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StepTest {

    /**
     * Sin {@code default}: como {@link Step} y {@link Exercise} están sellados, el compilador sabe que
     * estos cuatro casos son todos. Si mañana se añade un tipo de ejercicio, este switch no compilará
     * hasta tratarlo, y lo mismo pasará en las reglas.
     */
    static String kindOf(Step step) {
        return switch (step) {
            case Lesson lesson -> "lesson";
            case SingleChoice exercise -> "singleChoice";
            case MultipleChoice exercise -> "multipleChoice";
            case Matching exercise -> "matching";
        };
    }

    @Test
    void everyStepIsALessonOrOneOfTheThreeExercises() {
        List<Step> steps = List.of(
                new Lesson("l", "Texto"),
                new SingleChoice("s", PROMPT, options(2), "o0", EXPLANATION),
                new MultipleChoice("m", PROMPT, options(3), List.of("o0"), EXPLANATION),
                new Matching("p", PROMPT, pairs(2), EXPLANATION));

        assertEquals(List.of("lesson", "singleChoice", "multipleChoice", "matching"),
                steps.stream().map(StepTest::kindOf).toList());
    }

    @Test
    void everyExerciseHasPromptAndExplanation() {
        Exercise exercise = new Matching("p", PROMPT, pairs(2), EXPLANATION);

        assertEquals(PROMPT, exercise.prompt());
        assertEquals(EXPLANATION, exercise.explanation());
        assertEquals("p", exercise.id());
    }
}
