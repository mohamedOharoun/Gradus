package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.RegisterUserCommand;

/** Cuerpo JSON de {@code POST /api/users}. Es el contrato con la app; puede evolucionar sin tocar la aplicación. */
public record RegisterUserRequest(String name, String email, String password) {

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(name, email, password);
    }

    @Override
    public String toString() {
        return "RegisterUserRequest[name=" + name + ", email=" + email + ", password=****]";
    }
}
