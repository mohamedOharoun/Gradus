package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grafo de prerrequisitos de un curso (§3.5): una arista m → p por cada p de {@code m.requires}.
 * Inmutable. Supone que la capa 3 ya ha pasado (ids únicos, prerrequisitos que existen); aun así,
 * ignora los prerrequisitos desconocidos para no fallar nunca.
 */
public final class PrerequisiteGraph {

    /** Ids de los módulos en orden de documento. */
    private final List<String> modules;
    /** Prerrequisitos de cada módulo, en el orden de su {@code requires}. */
    private final Map<String, List<String>> requires;
    /** Dirección contraria: los módulos que requieren a cada uno. Solo se deduce, no se guarda (§2.3). */
    private final Map<String, List<String>> requiredBy;

    private PrerequisiteGraph(List<String> modules, Map<String, List<String>> requires) {
        this.modules = modules;
        this.requires = requires;
        this.requiredBy = modules.stream().collect(Collectors.toUnmodifiableMap(Function.identity(),
                module -> modules.stream().filter(other -> requires.get(other).contains(module)).toList()));
    }

    /** Factory method: construye el grafo a partir de los módulos del curso. */
    public static PrerequisiteGraph of(CourseDocument course) {
        List<String> ids = course.modules().stream().map(Module::id).distinct().toList();
        Set<String> known = Set.copyOf(ids);
        // El orden lo da la lista de ids; si un id se repitiera (la capa 3 lo impide), cuenta el primero.
        Map<String, List<String>> requires = course.modules().stream().collect(Collectors.toUnmodifiableMap(
                Module::id,
                module -> module.requires().stream().filter(known::contains).toList(),
                (first, later) -> first));
        return new PrerequisiteGraph(ids, requires);
    }

    /**
     * Un ciclo concreto, si lo hay, como lista que empieza y acaba en el mismo módulo:
     * {@code [representacion, camino, bfs, representacion]}. Sigue el procedimiento de §3.5: empieza
     * por el primer módulo que Kahn no pudo tomar y sigue siempre su primer prerrequisito sin tomar.
     */
    public Optional<List<String>> findCycle() {
        List<String> remaining = notTakenByKahn();
        if (remaining.isEmpty()) {
            return Optional.empty();
        }
        Set<String> rest = Set.copyOf(remaining);
        List<String> walk = new ArrayList<>();
        String current = remaining.getFirst();
        while (!walk.contains(current)) {
            walk.add(current);
            // Siempre existe: si un módulo restante no tuviera prerrequisitos restantes, Kahn lo habría tomado.
            current = requires.get(current).stream().filter(rest::contains).findFirst().orElseThrow();
        }
        List<String> cycle = new ArrayList<>(walk.subList(walk.indexOf(current), walk.size()));
        cycle.add(current);
        return Optional.of(List.copyOf(cycle));
    }

    /**
     * El camino más corto de {@code from} a {@code to} siguiendo {@code requires} (recorrido en
     * anchura), con al menos una arista: {@code [c, b, a]} si c requiere b y b requiere a.
     */
    public Optional<List<String>> path(String from, String to) {
        Map<String, String> reachedFrom = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>(List.of(from));
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String next : requires.getOrDefault(current, List.of())) {
                if (next.equals(from) || reachedFrom.containsKey(next)) {
                    continue;
                }
                reachedFrom.put(next, current);
                if (next.equals(to)) {
                    return Optional.of(rebuild(from, to, reachedFrom));
                }
                queue.add(next);
            }
        }
        return Optional.empty();
    }

    /**
     * Algoritmo de Kahn: se toma cada módulo cuyos prerrequisitos ya se han tomado todos. Devuelve los
     * que nunca se pueden tomar, en orden de documento: si hay alguno, hay al menos un ciclo.
     */
    private List<String> notTakenByKahn() {
        Map<String, Integer> pending = new HashMap<>();
        modules.forEach(module -> pending.put(module, requires.get(module).size()));
        Deque<String> ready = modules.stream()
                .filter(module -> pending.get(module) == 0)
                .collect(Collectors.toCollection(ArrayDeque::new));
        Set<String> taken = new HashSet<>();
        while (!ready.isEmpty()) {
            String module = ready.poll();
            taken.add(module);
            requiredBy.get(module).stream()
                    .filter(dependent -> pending.merge(dependent, -1, Integer::sum) == 0)
                    .forEach(ready::add);
        }
        return modules.stream().filter(module -> !taken.contains(module)).toList();
    }

    /** Reconstruye el camino hacia atrás, de {@code to} a {@code from}, y le da la vuelta. */
    private static List<String> rebuild(String from, String to, Map<String, String> reachedFrom) {
        List<String> path = new ArrayList<>(List.of(to));
        for (String step = to; !step.equals(from); step = reachedFrom.get(step)) {
            path.addFirst(reachedFrom.get(step));
        }
        return List.copyOf(path);
    }
}
