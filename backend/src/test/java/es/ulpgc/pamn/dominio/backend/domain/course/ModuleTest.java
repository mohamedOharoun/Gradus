package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.module;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.reviewPool;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleTest {

    static final Mastery DEFAULTS = new Mastery(4, 3);

    @Test
    void theEffectiveMasteryIsItsOwnIfItHasOne() {
        Module module = module("bfs").mastery(5, 4).build();

        assertEquals(new Mastery(5, 4), module.effectiveMastery(DEFAULTS));
    }

    @Test
    void theEffectiveMasteryIsTheCourseDefaultOtherwise() {
        assertEquals(DEFAULTS, module("bfs").build().effectiveMastery(DEFAULTS));
    }

    @Test
    void titleIsSingleLinePlainTextUpTo120Characters() {
        assertDoesNotThrow(() -> module("m").title("x".repeat(120)).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").title("x".repeat(121)).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").title("Grafos\n").build());
    }

    @Test
    void requiresUpTo20DistinctWellFormedIds() {
        String[] twenty = IntStream.range(0, 20).mapToObj(i -> "m" + i).toArray(String[]::new);
        String[] twentyOne = IntStream.range(0, 21).mapToObj(i -> "m" + i).toArray(String[]::new);

        assertDoesNotThrow(() -> module("m").requires(twenty).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").requires(twentyOne).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").requires("a", "a").build());
        assertThrows(IllegalArgumentException.class, () -> module("m").requires("A").build());
    }

    @Test
    void unknownOrSelfPrerequisitesCanBeBuiltSoTheRulesCanReportThem() {
        // UNKNOWN_PREREQUISITE y SELF_PREREQUISITE son reglas de la capa 3.
        assertDoesNotThrow(() -> module("m").requires("m", "no-existe").build());
    }

    @Test
    void stepsHaveBetween1And50Elements() {
        List<Step> fifty = Collections.nCopies(50, lesson("l"));
        List<Step> fiftyOne = new ArrayList<>(fifty);
        fiftyOne.add(lesson("l"));

        assertDoesNotThrow(() -> module("m").steps(fifty).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").steps(List.of()).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").steps(fiftyOne).build());
    }

    @Test
    void reviewHasBetween1And100Exercises() {
        assertDoesNotThrow(() -> module("m").review(singleChoice("r", 2)).build());
        assertDoesNotThrow(() -> module("m").review(reviewPool("m", 100)).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").review(List.of()).build());
        assertThrows(IllegalArgumentException.class, () -> module("m").review(reviewPool("m", 101)).build());
    }

    @Test
    void requiresEveryField() {
        List<Step> steps = List.of(lesson("l"));
        List<Exercise> review = List.of(singleChoice("r", 2));
        Optional<Mastery> none = Optional.empty();

        assertThrows(NullPointerException.class, () -> new Module(null, "T", List.of(), none, steps, review));
        assertThrows(NullPointerException.class, () -> new Module("m", null, List.of(), none, steps, review));
        assertThrows(NullPointerException.class, () -> new Module("m", "T", null, none, steps, review));
        assertThrows(NullPointerException.class, () -> new Module("m", "T", List.of(), null, steps, review));
        assertThrows(NullPointerException.class, () -> new Module("m", "T", List.of(), none, null, review));
        assertThrows(NullPointerException.class, () -> new Module("m", "T", List.of(), none, steps, null));
    }
}
