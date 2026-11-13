package es.ulpgc.pamn.dominio.backend.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Usuario registrado. Inmutable: cualquier cambio futuro producirá un {@code User} nuevo. */
public record User(UUID id, UserName name, Email email, PasswordHash passwordHash, Instant createdAt) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
