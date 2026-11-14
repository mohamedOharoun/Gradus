package es.ulpgc.pamn.dominio.backend.domain.validation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/** Utilidades de test para inspeccionar un {@link Result} sin repetir código en cada test. */
public final class ResultAssertions {

    private ResultAssertions() {
    }

    public static <T> T assertSuccess(Result<T> result) {
        return switch (result) {
            case Result.Success<T> success -> success.value();
            case Result.Failure<T> failure -> fail("Se esperaba éxito, pero hubo errores: " + failure.errors());
        };
    }

    public static List<ValidationError> assertFailure(Result<?> result) {
        return switch (result) {
            case Result.Success<?> success -> fail("Se esperaba fallo, pero fue éxito: " + success.value());
            case Result.Failure<?> failure -> failure.errors();
        };
    }

    /** Devuelve solo los mensajes de error de un campo concreto. */
    public static List<String> messagesFor(String field, List<ValidationError> errors) {
        return errors.stream()
                .filter(error -> error.field().equals(field))
                .map(ValidationError::message)
                .toList();
    }

    /** Devuelve solo los códigos de error de un campo concreto, en orden. */
    public static List<ErrorCode> codesFor(String field, List<ValidationError> errors) {
        return errors.stream()
                .filter(error -> error.field().equals(field))
                .map(ValidationError::code)
                .toList();
    }
}
