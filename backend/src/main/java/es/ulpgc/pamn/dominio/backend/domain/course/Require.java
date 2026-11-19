package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static es.ulpgc.pamn.dominio.backend.domain.validation.Validation.lengthOf;

/**
 * Comprobaciones de los constructores del curso: repiten lo que el schema ya prohíbe, para que un
 * record del dominio nunca exista en un estado que el formato no admite. Lanzan excepciones porque,
 * tras pasar el schema, fallar aquí sería un error de programación, no del autor del curso.
 */
final class Require {

    static final int MAX_ID_LENGTH = 64;

    /** {@code matches} exige que coincida la cadena entera: un salto de línea final no pasa. */
    private static final Pattern ID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    /**
     * Mismo {@code \S} que el patrón del schema. Sin {@code UNICODE_CHARACTER_CLASS} a propósito: así
     * el dominio nunca es más estricto que el schema y no rechaza un curso que el schema ya aceptó.
     */
    private static final Pattern NON_WHITESPACE = Pattern.compile("\\S");
    /** Texto plano del schema: empieza y acaba sin espacio y no tiene saltos de línea. */
    private static final Pattern PLAIN_TEXT = Pattern.compile("\\S(?:[^\\r\\n]*\\S)?");
    /** ISO 639-1, opcionalmente con región: es, en, es-ES. */
    private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2}(-[A-Z]{2})?");

    private Require() {
    }

    /** Un id del curso (§2.4): minúsculas ASCII, dígitos y guiones sueltos, hasta 64 caracteres. */
    static String id(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.length() > MAX_ID_LENGTH || !ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " no es un id válido: «" + value + "»");
        }
        return value;
    }

    /** Lista de ids sin repetir, con entre {@code min} y {@code max} elementos. */
    static List<String> ids(List<String> values, String field, int min, int max) {
        List<String> copy = distinct(size(values, field, min, max), field);
        copy.forEach(value -> id(value, field));
        return copy;
    }

    /** Texto plano (§2.7): una sola línea, sin espacios al principio ni al final. */
    static String plainText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field);
        if (!PLAIN_TEXT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    field + " debe ser una sola línea sin espacios al principio ni al final: «" + value + "»");
        }
        return maxLength(value, field, maxLength);
    }

    /** Código de idioma (§2.2): {@code es}, {@code en}, {@code es-ES}. */
    static String language(String value, String field) {
        Objects.requireNonNull(value, field);
        if (!LANGUAGE.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " no es un código de idioma válido: «" + value + "»");
        }
        return value;
    }

    /** Un entero entre {@code min} y {@code max}, ambos incluidos. */
    static int range(int value, String field, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " debe estar entre " + min + " y " + max + "; es " + value);
        }
        return value;
    }

    /** Markdown (§2.7): algún carácter que no sea espacio y como mucho {@code maxLength} caracteres reales. */
    static String markdown(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field);
        if (!NON_WHITESPACE.matcher(value).find()) {
            throw new IllegalArgumentException(field + " no puede estar en blanco");
        }
        return maxLength(value, field, maxLength);
    }

    /** Campos comunes a los tres tipos de {@link Exercise}, para que cada record no los repita. */
    static void exerciseFields(String id, String prompt, String explanation) {
        id(id, "id");
        markdown(prompt, "prompt", Exercise.MAX_PROMPT_LENGTH);
        markdown(explanation, "explanation", Exercise.MAX_EXPLANATION_LENGTH);
    }

    /** Copia inmutable de la lista, con entre {@code min} y {@code max} elementos y ninguno nulo. */
    static <T> List<T> size(List<T> values, String field, int min, int max) {
        List<T> copy = List.copyOf(Objects.requireNonNull(values, field));
        if (copy.size() < min || copy.size() > max) {
            throw new IllegalArgumentException(
                    field + " debe tener entre " + min + " y " + max + " elementos; tiene " + copy.size());
        }
        return copy;
    }

    /** La misma lista, si no repite ningún elemento ({@code uniqueItems} del schema). */
    static <T> List<T> distinct(List<T> values, String field) {
        if (Set.copyOf(values).size() != values.size()) {
            throw new IllegalArgumentException(field + " no puede repetir elementos: " + values);
        }
        return values;
    }

    private static String maxLength(String value, String field, int maxLength) {
        int length = lengthOf(value);
        if (length > maxLength) {
            throw new IllegalArgumentException(
                    field + " no puede superar " + maxLength + " caracteres; tiene " + length);
        }
        return value;
    }
}
