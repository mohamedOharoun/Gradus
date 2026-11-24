package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionTokenHash;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan las sesiones iniciadas. Se buscan por el hash del token. */
public interface SessionRepository {

    void add(Session session);

    Optional<Session> findByTokenHash(SessionTokenHash tokenHash);

    /** Guarda el último uso y la caducidad de una sesión renovada. Si ya no existe, no hace nada. */
    void update(Session session);

    /** Borra la sesión de ese token. Si no existe, no hace nada. */
    void remove(SessionTokenHash tokenHash);

    /** Cierra todas las sesiones de ese usuario menos la del token {@code keep} (la de este dispositivo). */
    void removeOthersOf(UUID userId, SessionTokenHash keep);
}
