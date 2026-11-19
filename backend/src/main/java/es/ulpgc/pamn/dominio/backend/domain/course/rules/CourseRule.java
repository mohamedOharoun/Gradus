package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;

import java.util.List;
import java.util.stream.Stream;

/**
 * Una regla de validación (Strategy): función pura de un curso a los problemas que encuentra, en
 * orden de documento. Cualquier método {@code static Stream<Issue> x(CourseDocument)} sirve como regla
 * con una referencia a método: {@code IdentityRules::duplicateIds}.
 */
@FunctionalInterface
public interface CourseRule {

    Stream<Issue> check(CourseDocument course);

    /** Composite: varias reglas se comportan como una sola, que devuelve sus problemas uno tras otro. */
    static CourseRule all(CourseRule... rules) {
        List<CourseRule> list = List.of(rules);
        return course -> list.stream().flatMap(rule -> rule.check(course));
    }
}
