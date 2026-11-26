package es.ulpgc.pamn.dominio.backend.application;

/** Lo que llega para cambiar la contraseña, aún sin validar. */
public record ChangePasswordCommand(String currentPassword, String newPassword) {

    /** Sustituye al toString generado por el record, que mostraría las contraseñas en los logs. */
    @Override
    public String toString() {
        return "ChangePasswordCommand[currentPassword=****, newPassword=****]";
    }
}
