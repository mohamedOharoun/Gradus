package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.Rule;
import es.ulpgc.pamn.dominio.backend.domain.validation.Validation;

import java.util.List;
import java.util.function.IntPredicate;

import static es.ulpgc.pamn.dominio.backend.domain.validation.Validation.lengthOf;

/**
 * Contraseña en claro tal como la escribe el usuario. Solo vive en memoria el tiempo justo para
 * hashearla: nunca se guarda ni se muestra. No se normaliza (los espacios forman parte de ella).
 */
public record RawPassword(String value) {

    public static final int MIN_LENGTH = 8;

    private static final String FIELD = "password";
    private static final List<Rule> RULES = List.of(
            Rule.of(v -> lengthOf(v) >= MIN_LENGTH, "Debe tener al menos " + MIN_LENGTH + " caracteres."),
            Rule.of(v -> contains(v, Character::isLetter), "Debe contener al menos una letra."),
            Rule.of(v -> contains(v, Character::isDigit), "Debe contener al menos un número."),
            Rule.of(v -> contains(v, RawPassword::isSpecial), "Debe contener al menos un carácter especial."));

    public RawPassword {
        Validation.requireValid(FIELD, value, RULES);
    }

    public static Result<RawPassword> of(String raw) {
        return Result.validate(Validation.check(FIELD, raw, RULES), () -> new RawPassword(raw));
    }

    /** Sustituye al toString generado por el record, que mostraría la contraseña en los logs. */
    @Override
    public String toString() {
        return "RawPassword[****]";
    }

    private static boolean contains(String value, IntPredicate predicate) {
        return value.codePoints().anyMatch(predicate);
    }

    private static boolean isSpecial(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }
}
