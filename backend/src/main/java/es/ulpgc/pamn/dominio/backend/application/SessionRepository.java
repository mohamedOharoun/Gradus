package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionTokenHash;

import java.util.Optional;

/** Puerto de salida: dónde se guardan las sesiones iniciadas. Se buscan por el hash del token. */
public interface SessionRepository {

    void add(Session session);

    Optional<Session> findByTokenHash(SessionTokenHash tokenHash);

    /** Guarda el último uso y la caducidad de una sesión renovada. Si ya no existe, no hace nada. */
    void update(Session session);

    /** Borra la sesión de ese token. Si no existe, no hace nada. */
    void remove(SessionTokenHash tokenHash);
}
