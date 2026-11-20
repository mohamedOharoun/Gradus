package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.domain.course.Issue;

/** Un problema del informe (§3.7). La gravedad no va aquí: la indica la lista en la que está. */
public record IssueResponse(String code, String path, String message) {

    public static IssueResponse from(Issue issue) {
        return new IssueResponse(issue.code().name(), issue.path().toString(), issue.message());
    }
}
