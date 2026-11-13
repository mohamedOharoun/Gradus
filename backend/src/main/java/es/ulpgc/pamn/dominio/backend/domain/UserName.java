package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.Rule;
import es.ulpgc.pamn.dominio.backend.domain.validation.Validation;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.validation.Validation.lengthOf;

/** Nombre visible del usuario, sin espacios alrededor y de 2 a 100 caracteres. */
public record UserName(String value) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 100;

    private static final String FIELD = "name";
    private static final List<Rule> RULES = List.of(
            Rule.of(v -> lengthOf(v) >= MIN_LENGTH, "Debe tener al menos " + MIN_LENGTH + " caracteres."),
            Rule.of(v -> lengthOf(v) <= MAX_LENGTH, "No puede superar los " + MAX_LENGTH + " caracteres."));

    public UserName {
        value = normalize(value);
        Validation.requireValid(FIELD, value, RULES);
    }

    public static Result<UserName> of(String raw) {
        String normalized = normalize(raw);
        return Result.validate(Validation.check(FIELD, normalized, RULES), () -> new UserName(normalized));
    }

    private static String normalize(String raw) {
        return raw == null ? null : raw.strip();
    }
}
