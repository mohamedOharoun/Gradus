package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.module;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.reviewPool;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionComparisonTest {

    static final CourseDocument PREVIOUS = course().build();

    @Test
    void anIdenticalVersionRemovesNothing() {
        assertEquals(List.of(), VersionComparison.compareVersions(PREVIOUS, course().build()));
    }

    @Test
    void onlyAddingIdsIsFine() {
        CourseDocument next = course().modules(
                PREVIOUS.modules().getFirst(), PREVIOUS.modules().get(1), module("dijkstra").requires("bfs").build())
                .build();

        assertEquals(List.of(), VersionComparison.compareVersions(PREVIOUS, next));
    }

    @Test
    void removedModuleLessonAndExerciseIdsAreOneWarningAtTheRoot() {
        // Desaparecen el módulo «bfs» (con sus ejercicios) y la lección «representacion-l1».
        CourseDocument next = course()
                .modules(module("representacion")
                        .steps(lesson("representacion-intro"), singleChoice("representacion-f1", 3))
                        .build())
                .build();

        List<Issue> warnings = VersionComparison.compareVersions(PREVIOUS, next);

        assertEquals(1, warnings.size());
        Issue warning = warnings.getFirst();
        assertEquals(CourseIssueCode.REMOVED_IDS, warning.code());
        assertEquals(Severity.WARNING, warning.severity());
        assertEquals("", warning.path().toString());
        assertTrue(warning.message().contains("representacion-l1, bfs, bfs-l1, bfs-f1, bfs-r1"), warning.message());
    }

    @Test
    void optionIdsAreNotTracked() {
        // El progreso se refiere a módulos, lecciones y ejercicios; las opciones son locales a su ejercicio.
        CourseDocument next = course()
                .module(0, m -> m.review(reviewPool("representacion", 8).stream()
                        .<Exercise>map(exercise -> singleChoice(exercise.id(), 3))
                        .toList()))
                .build();

        assertEquals(List.of(), VersionComparison.compareVersions(PREVIOUS, next));
    }
}
