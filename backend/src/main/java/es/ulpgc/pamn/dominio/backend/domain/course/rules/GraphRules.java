package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Reglas de la capa 4 sobre el grafo de prerrequisitos (§3.5): ciclos y aristas redundantes. */
public final class GraphRules {

    private GraphRules() {
    }

    /** {@code CYCLE}: se informa un solo ciclo; el autor corrige y vuelve a validar. */
    public static Stream<Issue> cycles(CourseDocument course) {
        return PrerequisiteGraph.of(course).findCycle().stream()
                .map(cycle -> Issue.at(modulePath(course, cycle.getFirst()), CourseIssueCode.CYCLE,
                        "Los prerrequisitos forman un ciclo: " + chain(cycle) + "."));
    }

    /**
     * {@code REDUNDANT_PREREQUISITE} (aviso): el prerrequisito p de m es redundante si ya se alcanza
     * desde otro prerrequisito q de m. Solo se comprueba si no hay ciclos.
     */
    public static Stream<Issue> redundantPrerequisites(CourseDocument course) {
        PrerequisiteGraph graph = PrerequisiteGraph.of(course);
        if (graph.findCycle().isPresent()) {
            return Stream.empty();
        }
        return Locations.modules(course).stream().flatMap(module -> {
            List<String> requires = module.value().requires();
            return Locations.indexed(requires, module.path().key("requires")).stream()
                    .flatMap(required -> reachedThroughAnother(graph, requires, required.value()).stream()
                            .map(via -> Issue.at(required.path(), CourseIssueCode.REDUNDANT_PREREQUISITE,
                                    "El prerrequisito «" + required.value() + "» de «" + module.value().id()
                                            + "» es redundante: ya se alcanza a través de «" + via.getFirst()
                                            + "» (" + chain(via) + "). Puedes quitarlo de requires; "
                                            + "el orden de estudio no cambia.")));
        });
    }

    /** El primer camino, en el orden de {@code requires}, que llega a {@code target} desde otro prerrequisito. */
    private static Optional<List<String>> reachedThroughAnother(PrerequisiteGraph graph, List<String> requires,
                                                                String target) {
        return requires.stream()
                .filter(other -> !other.equals(target))
                .flatMap(other -> graph.path(other, target).stream())
                .findFirst();
    }

    /** {@code [a, b, c]} → «a requiere b, que requiere c». */
    private static String chain(List<String> ids) {
        return ids.getFirst() + " requiere " + ids.get(1) + ids.stream().skip(2)
                .map(id -> ", que requiere " + id)
                .collect(Collectors.joining());
    }

    private static JsonPointer modulePath(CourseDocument course, String moduleId) {
        return IntStream.range(0, course.modules().size())
                .filter(i -> course.modules().get(i).id().equals(moduleId))
                .mapToObj(Locations.MODULES::index)
                .findFirst()
                .orElseThrow();
    }
}
