package es.ulpgc.pamn.dominio.backend.domain.validation;

import java.util.Objects;
import java.util.function.Predicate;

/** Una regla sobre un texto: la condición que debe cumplir, y el código y el mensaje si no la cumple. */
public record Rule(Predicate<String> condition, ErrorCode code, String message) {

    public Rule {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }

    public static Rule of(Predicate<String> condition, ErrorCode code, String message) {
        return new Rule(condition, code, message);
    }

    public boolean isBrokenBy(String value) {
        return !condition.test(value);
    }
}
