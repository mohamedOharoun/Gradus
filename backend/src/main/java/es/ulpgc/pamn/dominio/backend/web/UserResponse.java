package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.domain.User;

/** Usuario tal como se devuelve en JSON. No incluye la contraseña ni su hash. */
public record UserResponse(String id, String name, String email, String createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().toString(),
                user.name().value(),
                user.email().value(),
                user.createdAt().toString()); // ISO-8601, por ejemplo 2026-11-13T10:00:00.123456Z
    }
}
