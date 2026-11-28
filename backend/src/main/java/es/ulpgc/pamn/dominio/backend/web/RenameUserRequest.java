package es.ulpgc.pamn.dominio.backend.web;

/** Cuerpo JSON de {@code PATCH /api/users/me}: {@code {"name": "..."}}. Es el contrato con la app. */
public record RenameUserRequest(String name) {
}
