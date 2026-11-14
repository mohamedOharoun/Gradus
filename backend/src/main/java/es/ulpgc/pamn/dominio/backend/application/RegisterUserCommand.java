package es.ulpgc.pamn.dominio.backend.application;

/**
 * Petición de registro tal como llega del exterior: datos sin validar (pueden ser {@code null}).
 * La validación la hace {@link RegisterUser}, no este objeto.
 */
public record RegisterUserCommand(String name, String email, String password, Boolean acceptedTerms) {

    /** Evita que la contraseña en claro acabe en los logs. */
    @Override
    public String toString() {
        return "RegisterUserCommand[name=" + name + ", email=" + email
                + ", password=****, acceptedTerms=" + acceptedTerms + "]";
    }
}
