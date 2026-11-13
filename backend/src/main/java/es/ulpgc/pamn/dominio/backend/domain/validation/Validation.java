package es.ulpgc.pamn.dominio.backend.domain.validation;

import java.util.List;

/** Funciones puras para aplicar una lista de {@link Rule} a un campo. */
public final class Validation {

    static final String REQUIRED_MESSAGE = "Es obligatorio.";

    private Validation() {
    }

    /** Devuelve todos los errores del campo (lista vacía si es válido). Un {@code null} solo da el error de obligatorio. */
    public static List<ValidationError> check(String field, String value, List<Rule> rules) {
        if (value == null) {
            return List.of(new ValidationError(field, REQUIRED_MESSAGE));
        }
        return rules.stream()
                .filter(rule -> rule.isBrokenBy(value))
                .map(rule -> new ValidationError(field, rule.message()))
                .toList();
    }

    /** Igual que {@link #check}, pero lanza una excepción: protege los constructores de los objetos de valor. */
    public static void requireValid(String field, String value, List<Rule> rules) {
        List<ValidationError> errors = check(field, value, rules);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.toString());
        }
    }

    /** Longitud en caracteres reales (code points), igual que {@code char_length} de PostgreSQL. */
    public static int lengthOf(String value) {
        return value.codePointCount(0, value.length());
    }
}
