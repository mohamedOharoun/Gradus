package es.ulpgc.pamn.dominio.backend.application;

/** Lo que llega para eliminar la cuenta, aún sin validar: la contraseña, como confirmación. */
public record DeleteAccountCommand(String password) {

    /** Sustituye al toString generado por el record, que mostraría la contraseña en los logs. */
    @Override
    public String toString() {
        return "DeleteAccountCommand[password=****]";
    }
}
