package es.ulpgc.pamn.dominio.backend.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Usuario registrado. Inmutable: cualquier cambio futuro producirá un {@code User} nuevo. */
public record User(UUID id, UserName name, Email email, PasswordHash passwordHash, Instant createdAt,
                   TermsAcceptance termsAcceptance) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(termsAcceptance, "termsAcceptance");
    }

    /** Este usuario con otro nombre (Ajustes). El resto no cambia; el original tampoco (es inmutable). */
    public User withName(UserName newName) {
        return new User(id, newName, email, passwordHash, createdAt, termsAcceptance);
    }

    /** Este usuario con otra contraseña (ya hasheada). El resto no cambia. */
    public User withPasswordHash(PasswordHash newHash) {
        return new User(id, name, email, newHash, createdAt, termsAcceptance);
    }
}
