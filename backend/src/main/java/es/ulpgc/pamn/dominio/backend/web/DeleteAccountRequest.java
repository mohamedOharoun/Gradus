package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.DeleteAccountCommand;

/** Cuerpo JSON de {@code DELETE /api/users/me}: {@code {"password": "..."}}. Es el contrato con la app. */
public record DeleteAccountRequest(String password) {

    public DeleteAccountCommand toCommand() {
        return new DeleteAccountCommand(password);
    }

    @Override
    public String toString() {
        return "DeleteAccountRequest[password=****]";
    }
}
