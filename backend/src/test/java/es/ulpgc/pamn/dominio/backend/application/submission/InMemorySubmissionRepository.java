package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Repositorio de entregas en memoria para los tests: guarda cada entrega con su foto, en orden. */
final class InMemorySubmissionRepository implements SubmissionRepository {

    private final Map<UUID, Submission> submissions = new LinkedHashMap<>();
    private final Map<UUID, Jpeg> photos = new LinkedHashMap<>();

    @Override
    public synchronized void add(Submission submission, Jpeg photo) {
        submissions.put(submission.id(), submission);
        photos.put(submission.id(), photo);
    }

    synchronized List<Submission> all() {
        return new ArrayList<>(submissions.values());
    }

    synchronized Jpeg photoOf(UUID submissionId) {
        return photos.get(submissionId);
    }
}
