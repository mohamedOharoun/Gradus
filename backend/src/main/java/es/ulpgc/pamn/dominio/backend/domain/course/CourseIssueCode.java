package es.ulpgc.pamn.dominio.backend.domain.course;

import static es.ulpgc.pamn.dominio.backend.domain.course.Severity.ERROR;
import static es.ulpgc.pamn.dominio.backend.domain.course.Severity.WARNING;

/**
 * Códigos estables de los problemas de un curso (especificación §3.3). Cada código fija su gravedad,
 * así que no se puede crear, por ejemplo, un {@code CYCLE} como aviso. Los nombres no se renombran
 * ni se borran: los leen los autores y sus LLM (lo vigila {@code CourseIssueCodeTest}).
 */
public enum CourseIssueCode {

    // Capa 1: sintaxis
    INVALID_JSON(ERROR),
    DUPLICATE_KEY(ERROR),

    // Capa 2: versión y estructura
    UNSUPPORTED_FORMAT_VERSION(ERROR),
    SCHEMA_VIOLATION(ERROR),

    // Capa 3: identidad y referencias
    DUPLICATE_ID(ERROR),
    DUPLICATE_OPTION_ID(ERROR),
    DUPLICATE_OPTION_TEXT(ERROR),
    UNKNOWN_OPTION(ERROR),
    ALL_OPTIONS_CORRECT(ERROR),
    DUPLICATE_MATCHING_SIDE(ERROR),
    UNKNOWN_PREREQUISITE(ERROR),
    SELF_PREREQUISITE(ERROR),
    DUPLICATE_TAG(WARNING),

    // Capa 4: grafo
    CYCLE(ERROR),
    REDUNDANT_PREREQUISITE(WARNING),

    // Capa 5: dominio y pedagogía
    MASTERY_MIN_EXCEEDS_QUESTIONS(ERROR),
    MASTERY_POOL_TOO_SMALL(ERROR),
    MASTERY_POOL_SMALL_FOR_RETRY(WARNING),
    MASTERY_REQUIRES_PERFECT(WARNING),
    MASTERY_GUESSABLE(WARNING),
    MODULE_WITHOUT_LESSON(WARNING),
    MODULE_WITHOUT_EXERCISE(WARNING),
    MATCHING_TOO_MANY_PAIRS(WARNING),

    // Fuera del validador: límite de tamaño (§4.7) y comparación de versiones (§4.6)
    PAYLOAD_TOO_LARGE(ERROR),
    REMOVED_IDS(WARNING);

    private final Severity severity;

    CourseIssueCode(Severity severity) {
        this.severity = severity;
    }

    public Severity severity() {
        return severity;
    }
}
