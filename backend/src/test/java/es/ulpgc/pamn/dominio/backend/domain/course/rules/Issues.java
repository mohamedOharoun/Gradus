package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;

import java.util.List;
import java.util.stream.Stream;

/** Ayudas para comprobar los problemas que devuelve una regla. */
final class Issues {

    private Issues() {
    }

    static List<String> paths(Stream<Issue> issues) {
        return issues.map(issue -> issue.path().toString()).toList();
    }

    static List<CourseIssueCode> codes(Stream<Issue> issues) {
        return issues.map(Issue::code).toList();
    }

    /** El único problema de la lista; falla si hay cero o más de uno. */
    static Issue single(Stream<Issue> issues) {
        List<Issue> list = issues.toList();
        if (list.size() != 1) {
            throw new AssertionError("Se esperaba exactamente un problema y hay " + list.size() + ": " + list);
        }
        return list.getFirst();
    }
}
