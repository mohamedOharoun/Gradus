package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;

import java.util.Objects;

/**
 * Facade de las capas 1–5: lee el curso con el puerto {@link CourseReader} y, si se puede leer, le aplica
 * las reglas de {@link CourseValidator}. Lo comparten validar, publicar un curso y publicar una versión,
 * para que los tres revisen un curso exactamente igual.
 */
public final class CourseInspector {

    private final CourseReader reader;
    private final CourseValidator validator;

    public CourseInspector(CourseReader reader, CourseValidator validator) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public Inspection inspect(byte[] body) {
        return switch (reader.read(body)) {
            case CourseReadResult.Parsed parsed -> applyRules(parsed);
            case CourseReadResult.TooLarge tooLarge -> new Inspection.TooLarge(tooLarge.report());
            case CourseReadResult.Unreadable unreadable -> new Inspection.Unreadable(unreadable.report());
            case CourseReadResult.Invalid invalid -> new Inspection.Rejected(invalid.report());
        };
    }

    private Inspection applyRules(CourseReadResult.Parsed parsed) {
        CourseReport report = validator.validate(parsed.course());
        return report.isValid()
                ? new Inspection.Accepted(parsed.course(), parsed.content(), report)
                : new Inspection.Rejected(report);
    }
}
