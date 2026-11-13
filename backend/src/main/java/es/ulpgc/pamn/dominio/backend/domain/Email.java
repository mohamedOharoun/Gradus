package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.Rule;
import es.ulpgc.pamn.dominio.backend.domain.validation.Validation;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static es.ulpgc.pamn.dominio.backend.domain.validation.Validation.lengthOf;

/** Correo electrónico normalizado (sin espacios alrededor y en minúsculas). */
public record Email(String value) {

    public static final int MAX_LENGTH = 254;

    private static final String FIELD = "email";
    // Pragmático, no RFC completo: algo@dominio.ext, sin espacios ni etiquetas de dominio vacías.
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");
    private static final List<Rule> RULES = List.of(
            Rule.of(v -> FORMAT.matcher(v).matches(), "No tiene un formato de correo válido."),
            Rule.of(v -> lengthOf(v) <= MAX_LENGTH, "No puede superar los " + MAX_LENGTH + " caracteres."));

    public Email {
        value = normalize(value);
        Validation.requireValid(FIELD, value, RULES);
    }

    public static Result<Email> of(String raw) {
        String normalized = normalize(raw);
        return Result.validate(Validation.check(FIELD, normalized, RULES), () -> new Email(normalized));
    }

    private static String normalize(String raw) {
        return raw == null ? null : raw.strip().toLowerCase(Locale.ROOT);
    }
}
