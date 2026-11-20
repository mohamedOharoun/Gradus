package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;
import es.ulpgc.pamn.dominio.backend.domain.course.Page;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;
import java.util.Objects;

/** Los dos desenlaces de buscar cursos (§4.3): 200 con una página de resultados, o 400 {@code INVALID_PARAMETER}. */
public sealed interface SearchResult {

    record Found(Page<CourseSummary> page) implements SearchResult {
        public Found {
            Objects.requireNonNull(page, "page");
        }
    }

    /** Todos los parámetros no válidos, con el mismo formato que los errores del registro. */
    record Rejected(List<ValidationError> errors) implements SearchResult {
        public Rejected {
            errors = List.copyOf(errors);
        }
    }
}
