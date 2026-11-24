package es.ulpgc.pamn.dominio.backend.application.progress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Fake de {@link CourseExistence}: conoce unos cursos y anota con qué ids se le ha preguntado. */
final class FakeCourseExistence implements CourseExistence {

    private final Set<UUID> existing;
    private final List<Set<UUID>> calls = new ArrayList<>();

    FakeCourseExistence(UUID... existing) {
        this.existing = Set.of(existing);
    }

    /** Un elemento por llamada: los ids que recibió. */
    List<Set<UUID>> calls() {
        return List.copyOf(calls);
    }

    @Override
    public Set<UUID> findExisting(Set<UUID> courseIds) {
        calls.add(Set.copyOf(courseIds));
        Set<UUID> found = new HashSet<>(courseIds);
        found.retainAll(existing);
        return found;
    }
}
