package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.LogInCommand;

/** Cuerpo JSON de {@code POST /api/sessions}. Es el contrato con la app. */
public record LogInRequest(String email, String password) {

    public LogInCommand toCommand() {
        return new LogInCommand(email, password);
    }

    @Override
    public String toString() {
        return "LogInRequest[email=" + email + ", password=****]";
    }
}
