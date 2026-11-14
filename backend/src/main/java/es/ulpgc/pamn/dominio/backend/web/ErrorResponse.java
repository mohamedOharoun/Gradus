package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;

/** Respuesta de error en JSON: {@code {"errors": [{"field": "...", "code": "...", "message": "..."}]}}. */
public record ErrorResponse(List<ValidationError> errors) {

    public ErrorResponse {
        errors = List.copyOf(errors);
    }

    public static ErrorResponse of(String field, ErrorCode code, String message) {
        return new ErrorResponse(List.of(new ValidationError(field, code, message)));
    }
}
