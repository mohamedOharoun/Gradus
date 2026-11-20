package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersionDraft;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.course.PageRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio falso para tests. Los métodos {@code synchronized} hacen el papel del {@code FOR UPDATE}:
 * comprobar e insertar ocurren de una vez, sin que otra publicación se cuele en medio.
 */
final class InMemoryCourseRepository implements CourseRepository {

    /** Las versiones de cada curso, en orden: la de la posición i es la versión i + 1. */
    private final Map<UUID, List<CourseVersion>> versionsByCourse = new HashMap<>();

    @Override
    public synchronized CourseSummary create(UUID courseId, CourseAuthor author, CourseVersionDraft draft) {
        if (versionsByCourse.containsKey(courseId)) {
            throw new IllegalStateException("Ya existe un curso con el id " + courseId);
        }
        CourseVersion first = CourseVersion.of(courseId, CourseSummary.FIRST_VERSION, author, draft);
        versionsByCourse.put(courseId, new ArrayList<>(List.of(first)));
        return first.summary();
    }

    @Override
    public synchronized AppendVersionResult appendVersion(UUID courseId, UUID requesterId, CourseVersionDraft draft) {
        List<CourseVersion> versions = versionsByCourse.get(courseId);
        if (versions == null) {
            return new AppendVersionResult.CourseNotFound();
        }
        CourseVersion latest = versions.getLast();
        if (!latest.summary().author().id().equals(requesterId)) {
            return new AppendVersionResult.NotAuthor();
        }
        if (latest.content().sha256().equals(draft.content().sha256())) {
            return new AppendVersionResult.IdenticalToLatest();
        }
        CourseVersion next = CourseVersion.of(courseId, versions.size() + 1, latest.summary().author(), draft);
        versions.add(next);
        return new AppendVersionResult.Appended(next.summary());
    }

    @Override
    public synchronized Optional<CourseVersion> findLatest(UUID courseId) {
        return Optional.ofNullable(versionsByCourse.get(courseId)).map(List::getLast);
    }

    @Override
    public synchronized Optional<CourseVersion> findVersion(UUID courseId, int version) {
        return Optional.ofNullable(versionsByCourse.get(courseId))
                .filter(versions -> version >= 1 && version <= versions.size())
                .map(versions -> versions.get(version - 1));
    }

    /**
     * Solo últimas versiones, filtradas con la Specification del dominio y ordenadas por fecha descendente y
     * después por id. El id se compara como texto: así coincide con el orden de los UUID en PostgreSQL
     * (byte a byte), que no es el de {@code UUID.compareTo} (que compara números con signo).
     */
    @Override
    public synchronized Page<CourseSummary> search(CourseSearch search, PageRequest page) {
        List<CourseSummary> matching = versionsByCourse.values().stream()
                .map(versions -> versions.getLast().summary())
                .filter(summary -> search.matches(summary.metadata()))
                .sorted(Comparator.comparing(CourseSummary::publishedAt).reversed()
                        .thenComparing(summary -> summary.id().toString()))
                .toList();
        List<CourseSummary> items = matching.stream().skip(page.offset()).limit(page.size()).toList();
        return new Page<>(items, page, matching.size());
    }

    /** Cuántas versiones hay guardadas en total, para comprobar que un rechazo no guarda nada. */
    synchronized int versionCount() {
        return versionsByCourse.values().stream().mapToInt(List::size).sum();
    }
}
