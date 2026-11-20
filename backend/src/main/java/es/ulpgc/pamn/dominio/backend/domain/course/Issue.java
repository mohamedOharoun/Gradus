package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.Objects;

/**
 * Un problema encontrado en un curso: dónde está ({@code path}), qué regla incumple ({@code code}) y
 * una explicación en español pensada para el autor y su LLM. La gravedad la decide el código.
 */
public record Issue(JsonPointer path, CourseIssueCode code, String message) {

    public Issue {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Un problema necesita un mensaje");
        }
    }

    /** Factory method: se lee como la ruta del informe, «en {@code path}, {@code code}: {@code message}». */
    public static Issue at(JsonPointer path, CourseIssueCode code, String message) {
        return new Issue(path, code, message);
    }

    public Severity severity() {
        return code.severity();
    }

    public boolean isError() {
        return severity() == Severity.ERROR;
    }
}
