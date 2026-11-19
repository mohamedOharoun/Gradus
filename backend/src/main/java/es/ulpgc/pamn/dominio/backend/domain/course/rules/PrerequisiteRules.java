package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;

import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/** Reglas de la capa 3 sobre las referencias de {@code requires}: que apunten a otro módulo del curso. */
public final class PrerequisiteRules {

    private PrerequisiteRules() {
    }

    /** {@code UNKNOWN_PREREQUISITE}: un id de {@code requires} que no es un módulo del curso. */
    public static Stream<Issue> unknownPrerequisites(CourseDocument course) {
        List<String> moduleIds = course.modules().stream().map(Module::id).distinct().toList();
        Set<String> known = Set.copyOf(moduleIds);
        return prerequisites(course, (module, required) -> !known.contains(required))
                .map(found -> Issue.at(found.path(), CourseIssueCode.UNKNOWN_PREREQUISITE,
                        "El módulo «" + found.value().module() + "» requiere «" + found.value().required()
                                + "», que no es un módulo de este curso. Módulos existentes: "
                                + String.join(", ", moduleIds) + "."));
    }

    /** {@code SELF_PREREQUISITE}: un módulo que se requiere a sí mismo. */
    public static Stream<Issue> selfPrerequisites(CourseDocument course) {
        return prerequisites(course, String::equals)
                .map(found -> Issue.at(found.path(), CourseIssueCode.SELF_PREREQUISITE,
                        "El módulo «" + found.value().module() + "» se requiere a sí mismo. "
                                + "Quita «" + found.value().required() + "» de su lista requires."));
    }

    /** Una arista de {@code requires}: el módulo que requiere y el id requerido. */
    private record Requirement(String module, String required) {
    }

    /** Las aristas que cumplen {@code test(idDelMódulo, idRequerido)}, con su ruta /modules/i/requires/j. */
    private static Stream<Located<Requirement>> prerequisites(CourseDocument course, BiPredicate<String, String> test) {
        return Locations.modules(course).stream().flatMap(module ->
                Locations.indexed(module.value().requires(), module.path().key("requires")).stream()
                        .filter(required -> test.test(module.value().id(), required.value()))
                        .map(required -> new Located<>(required.path(),
                                new Requirement(module.value().id(), required.value()))));
    }
}
