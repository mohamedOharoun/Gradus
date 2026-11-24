package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;

/** Los tres desenlaces de cambiar la contraseña. La capa web traduce cada uno a un código HTTP. */
public sealed interface ChangePasswordResult {

    /** Contraseña cambiada; las sesiones de los otros dispositivos se han cerrado. */
    record Changed() implements ChangePasswordResult {
    }

    /** Falta alguna contraseña o la nueva no cumple las reglas; no se ha cambiado nada. */
    record Rejected(List<ValidationError> errors) implements ChangePasswordResult {
        public Rejected {
            errors = List.copyOf(errors);
        }
    }

    /** La contraseña actual no es la de la cuenta; no se ha cambiado nada. */
    record WrongPassword() implements ChangePasswordResult {
    }
}
