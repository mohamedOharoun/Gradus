package es.ulpgc.pamn.dominio.backend.application.course;

import java.util.Objects;

/**
 * Caso de uso: validar un curso sin guardarlo (§4.3), para que los autores, también con un LLM, puedan
 * corregir y volver a probar. No necesita repositorio.
 */
public final class ValidateCourse {

    private final CourseInspector inspector;

    public ValidateCourse(CourseInspector inspector) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public CourseValidationResult validate(byte[] body) {
        Inspection inspection = inspector.inspect(body);
        return switch (inspection) {
            case Inspection.TooLarge tooLarge -> new CourseValidationResult.TooLarge(tooLarge.report());
            case Inspection.Accepted accepted -> new CourseValidationResult.Checked(accepted.report());
            case Inspection.Unreadable unreadable -> new CourseValidationResult.Checked(unreadable.report());
            case Inspection.Rejected rejected -> new CourseValidationResult.Checked(rejected.report());
        };
    }
}
