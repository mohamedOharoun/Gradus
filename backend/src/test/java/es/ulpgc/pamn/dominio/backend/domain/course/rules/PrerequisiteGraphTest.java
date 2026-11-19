package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.module;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PrerequisiteGraphTest {

    /** Un módulo con sus prerrequisitos, en una línea: {@code node("bfs", "representacion")}. */
    static Module node(String id, String... requires) {
        return module(id).requires(requires).build();
    }

    static PrerequisiteGraph graph(Module... modules) {
        CourseDocument course = course().modules(modules).build();
        return PrerequisiteGraph.of(course);
    }

    @Test
    void anAcyclicGraphHasNoCycle() {
        PrerequisiteGraph graph = graph(node("a"), node("b", "a"), node("c", "a", "b"));

        assertEquals(Optional.empty(), graph.findCycle());
    }

    @Test
    void findsTheCycleOfTheSpecificationFollowingTheFirstPrerequisite() {
        // §3.5: «representacion requiere camino, que requiere bfs, que requiere representacion».
        PrerequisiteGraph graph = graph(
                node("representacion", "camino"), node("bfs", "representacion"), node("camino", "bfs"));

        assertEquals(Optional.of(List.of("representacion", "camino", "bfs", "representacion")), graph.findCycle());
    }

    @Test
    void theCycleMayStartAfterTheFirstRemainingModule() {
        // «a» queda sin tomar porque depende del ciclo b ↔ c, pero no forma parte de él.
        PrerequisiteGraph graph = graph(node("a", "b"), node("b", "c"), node("c", "b"));

        assertEquals(Optional.of(List.of("b", "c", "b")), graph.findCycle());
    }

    @Test
    void prerequisitesAlreadyTakenAreSkippedWhenFollowingTheCycle() {
        // El primer prerrequisito de «a» es «x», que no está en ningún ciclo: se sigue «b».
        PrerequisiteGraph graph = graph(node("x"), node("a", "x", "b"), node("b", "a"));

        assertEquals(Optional.of(List.of("a", "b", "a")), graph.findCycle());
    }

    @Test
    void findsAPathFollowingRequires() {
        PrerequisiteGraph graph = graph(node("a"), node("b", "a"), node("c", "b"), node("d"));

        assertEquals(Optional.of(List.of("c", "b", "a")), graph.path("c", "a"));
        assertEquals(Optional.empty(), graph.path("a", "c"));
        assertEquals(Optional.empty(), graph.path("d", "a"));
    }

    @Test
    void theShortestPathIsChosen() {
        PrerequisiteGraph graph = graph(node("a"), node("b", "a"), node("c", "b", "a"));

        assertEquals(Optional.of(List.of("c", "a")), graph.path("c", "a"));
    }
}
