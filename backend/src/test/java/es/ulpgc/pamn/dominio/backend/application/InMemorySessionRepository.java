package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionTokenHash;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Repositorio de sesiones falso para tests, indexado por el hash del token (como la tabla). */
final class InMemorySessionRepository implements SessionRepository {

    private final Map<SessionTokenHash, Session> sessionsByTokenHash = new ConcurrentHashMap<>();

    @Override
    public void add(Session session) {
        sessionsByTokenHash.put(session.tokenHash(), session);
    }

    @Override
    public Optional<Session> findByTokenHash(SessionTokenHash tokenHash) {
        return Optional.ofNullable(sessionsByTokenHash.get(tokenHash));
    }

    @Override
    public void update(Session session) {
        sessionsByTokenHash.replace(session.tokenHash(), session);
    }

    @Override
    public void removeOthersOf(UUID userId, SessionTokenHash keep) {
        sessionsByTokenHash.values().removeIf(s -> s.userId().equals(userId) && !s.tokenHash().equals(keep));
    }

    @Override
    public void remove(SessionTokenHash tokenHash) {
        sessionsByTokenHash.remove(tokenHash);
    }

    List<Session> all() {
        return List.copyOf(sessionsByTokenHash.values());
    }
}
