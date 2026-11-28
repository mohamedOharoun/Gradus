package es.ulpgc.pamn.dominio.backend.domain.session;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sesión iniciada en un dispositivo. Inmutable: renovarla produce una {@code Session} nueva
 * (ver {@link SessionPolicy#renew}). Se crea y se renueva siempre a través de la política.
 */
public record Session(UUID id, UUID userId, SessionTokenHash tokenHash, Instant createdAt, Instant lastUsedAt,
                      Instant expiresAt) {

    public Session {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(lastUsedAt, "lastUsedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (lastUsedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("No se puede usar una sesión antes de crearla");
        }
        if (!expiresAt.isAfter(lastUsedAt)) {
            throw new IllegalArgumentException("La sesión debe caducar después de su último uso");
        }
    }

    /** Activa hasta el instante anterior a {@code expiresAt}; en ese instante ya ha caducado. */
    public boolean isActiveAt(Instant now) {
        return now.isBefore(expiresAt);
    }
}
