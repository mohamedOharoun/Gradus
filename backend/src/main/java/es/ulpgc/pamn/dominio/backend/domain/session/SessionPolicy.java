package es.ulpgc.pamn.dominio.backend.domain.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Cuánto dura una sesión (Factory de {@link Session}): caduca tras {@code idleTimeout} sin usarse
 * y, aunque se use, nunca después de {@code maxLifetime} desde que se inició.
 */
public record SessionPolicy(Duration idleTimeout, Duration maxLifetime) {

    /** 30 días deslizantes con un tope de 90 (aprobado el 2026-11-17). */
    public static final SessionPolicy STANDARD = new SessionPolicy(Duration.ofDays(30), Duration.ofDays(90));

    public SessionPolicy {
        Objects.requireNonNull(idleTimeout, "idleTimeout");
        Objects.requireNonNull(maxLifetime, "maxLifetime");
        if (!idleTimeout.isPositive() || !maxLifetime.isPositive()) {
            throw new IllegalArgumentException("Las duraciones deben ser positivas");
        }
        if (idleTimeout.compareTo(maxLifetime) > 0) {
            throw new IllegalArgumentException("La inactividad no puede superar la duración máxima");
        }
    }

    /** Sesión recién iniciada en el instante {@code now}. */
    public Session start(UUID id, UUID userId, SessionTokenHash tokenHash, Instant now) {
        return new Session(id, userId, tokenHash, now, now, expiryFor(now, now));
    }

    /**
     * La misma sesión, usada en {@code now}: su caducidad avanza, sin pasar del tope.
     * Si el reloj retrocede, se conserva el último uso para que la caducidad no vaya hacia atrás.
     *
     * @throws IllegalStateException si la sesión ya ha caducado: no se resucita.
     */
    public Session renew(Session session, Instant now) {
        if (!session.isActiveAt(now)) {
            throw new IllegalStateException("No se puede renovar una sesión caducada");
        }
        Instant usedAt = now.isAfter(session.lastUsedAt()) ? now : session.lastUsedAt();
        return new Session(session.id(), session.userId(), session.tokenHash(), session.createdAt(), usedAt,
                expiryFor(session.createdAt(), usedAt));
    }

    private Instant expiryFor(Instant createdAt, Instant lastUsedAt) {
        Instant idleExpiry = lastUsedAt.plus(idleTimeout);
        Instant hardExpiry = createdAt.plus(maxLifetime);
        return idleExpiry.isBefore(hardExpiry) ? idleExpiry : hardExpiry;
    }
}
