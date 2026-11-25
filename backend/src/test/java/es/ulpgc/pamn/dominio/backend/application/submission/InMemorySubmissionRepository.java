package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Override
    public synchronized List<Submission> mine(UUID studentId, UUID courseId) {
        return submissions.values().stream()
                .filter(s -> s.studentId().equals(studentId) && s.courseId().equals(courseId))
                .toList();
    }

    @Override
    public synchronized Optional<Submission> find(UUID submissionId) {
        return Optional.ofNullable(submissions.get(submissionId));
    }

    @Override
    public synchronized Optional<Jpeg> photo(UUID submissionId) {
        return Optional.ofNullable(photos.get(submissionId));
    }

    /** Solo para probar un fallo imposible en la base de datos real: una entrega sin su foto. */
    synchronized void addWithoutPhoto(Submission submission) {
        submissions.put(submission.id(), submission);
    }

    synchronized List<Submission> all() {
        return new ArrayList<>(submissions.values());
    }

    synchronized Jpeg photoOf(UUID submissionId) {
        return photos.get(submissionId);
    }
}
