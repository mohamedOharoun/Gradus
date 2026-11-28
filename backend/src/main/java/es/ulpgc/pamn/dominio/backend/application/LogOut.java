package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.session.Session;

import java.util.Objects;

/** Caso de uso: cerrar la sesión de un dispositivo. Las de otros dispositivos siguen abiertas. */
public final class LogOut {

    private final SessionRepository sessions;

    public LogOut(SessionRepository sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    /**
     * Cierra la sesión ya autenticada (ver {@link Authenticate}). Es idempotente: cerrar una sesión
     * que ya no existe no es un error.
     */
    public void logOut(Session session) {
        sessions.remove(session.tokenHash());
    }
}
