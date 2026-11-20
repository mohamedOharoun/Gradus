package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;

import java.util.Objects;

/**
 * Resultado de revisar un curso con las capas 1–5. Todos los casos llevan el informe completo de §3.7,
 * así que quien solo quiere el informe (validar sin guardar) puede pedirlo sin mirar el caso.
 */
public sealed interface Inspection permits Inspection.Accepted, Inspection.TooLarge, Inspection.Unreadable,
        Inspection.Rejected {

    CourseReport report();

    /** Se puede publicar: sin errores. El informe solo puede tener avisos. */
    record Accepted(CourseDocument course, CourseContent content, CourseReport report) implements Inspection {
        public Accepted {
            Objects.requireNonNull(course, "course");
            Objects.requireNonNull(content, "content");
            if (!report.isValid()) {
                throw new IllegalArgumentException("Un curso con errores no puede aceptarse");
            }
        }
    }

    /** El cuerpo supera 2 MiB. */
    record TooLarge(CourseReport report) implements Inspection {
    }

    /** Capa 1: no es UTF-8 o no es JSON válido. */
    record Unreadable(CourseReport report) implements Inspection {
    }

    /** Capas 2–5: es JSON, pero no es un curso válido. */
    record Rejected(CourseReport report) implements Inspection {
    }
}
