package es.ulpgc.pamn.dominio.backend.application;

/**
 * Petición de inicio de sesión tal como llega del exterior: datos sin validar (pueden ser {@code null}).
 * La validación la hace {@link LogIn}, no este objeto.
 */
public record LogInCommand(String email, String password) {

    /** Evita que la contraseña en claro acabe en los logs. */
    @Override
    public String toString() {
        return "LogInCommand[email=" + email + ", password=****]";
    }
}
