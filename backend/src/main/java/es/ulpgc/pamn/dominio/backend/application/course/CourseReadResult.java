package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;

/**
 * Resultado de leer un curso (capas 1–2). Cada caso corresponde a una respuesta distinta (§4.4):
 * {@code TooLarge} → 413, {@code Unreadable} → 400, {@code Invalid} → 422; con {@code Parsed} se
 * sigue con las reglas de las capas 3–5.
 */
public sealed interface CourseReadResult permits CourseReadResult.Parsed, CourseReadResult.TooLarge,
        CourseReadResult.Unreadable, CourseReadResult.Invalid {

    /** El curso tipado y el texto exacto que se guardará. */
    record Parsed(CourseDocument course, CourseContent content) implements CourseReadResult {
    }

    /** El cuerpo supera 2 MiB ({@code PAYLOAD_TOO_LARGE}). */
    record TooLarge(CourseReport report) implements CourseReadResult {
    }

    /** Capa 1: no es UTF-8 válido o no es JSON válido ({@code INVALID_JSON}, {@code DUPLICATE_KEY}). */
    record Unreadable(CourseReport report) implements CourseReadResult {
    }

    /** Capa 2: es JSON, pero no es un curso v1 ({@code UNSUPPORTED_FORMAT_VERSION}, {@code SCHEMA_VIOLATION}). */
    record Invalid(CourseReport report) implements CourseReadResult {
    }
}
