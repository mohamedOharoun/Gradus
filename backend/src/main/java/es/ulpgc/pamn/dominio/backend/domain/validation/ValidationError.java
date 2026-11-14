package es.ulpgc.pamn.dominio.backend.domain.validation;

import java.util.Objects;

/**
 * Un error asociado a un campo concreto (por ejemplo, "email"). El {@code code} es para la app,
 * que decide el texto; el {@code message} es una explicación legible para quien prueba la API.
 */
public record ValidationError(String field, ErrorCode code, String message) {

    public ValidationError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
