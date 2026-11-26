package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.ChangePasswordCommand;

/** Cuerpo JSON de {@code PUT /api/users/me/password}. Es el contrato con la app. */
public record ChangePasswordRequest(String currentPassword, String newPassword) {

    public ChangePasswordCommand toCommand() {
        return new ChangePasswordCommand(currentPassword, newPassword);
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=****, newPassword=****]";
    }
}
