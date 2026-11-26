package es.ulpgc.pamn.dominio.backend.web.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.List;

/**
 * {@code {"submissions": [...]}}: las entregas propias en un curso, en el orden que da
 * {@code ListMySubmissions} (la más reciente primero). Un objeto y no una lista suelta, para poder añadir
 * campos más adelante sin romper la app.
 */
public record SubmissionListResponse(List<SubmissionResponse> submissions) {

    public SubmissionListResponse {
        submissions = List.copyOf(submissions);
    }

    public static SubmissionListResponse from(List<Submission> submissions) {
        return new SubmissionListResponse(submissions.stream().map(SubmissionResponse::from).toList());
    }
}
