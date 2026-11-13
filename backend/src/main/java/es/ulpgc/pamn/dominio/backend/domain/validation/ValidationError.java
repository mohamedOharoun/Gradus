package es.ulpgc.pamn.dominio.backend.domain.validation;

import java.util.Objects;

/** Un error de validación asociado a un campo concreto (por ejemplo, "email"). */
public record ValidationError(String field, String message) {

    public ValidationError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(message, "message");
    }
}
