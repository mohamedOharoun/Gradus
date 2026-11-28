package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.Objects;
import java.util.UUID;

/**
 * El autor de un curso tal como se muestra (§4.4). El nombre no se vuelve a validar como
 * {@code UserName}: si sus reglas se endurecen, los cursos de autores antiguos se siguen pudiendo leer.
 */
public record CourseAuthor(UUID id, String name) {

    public CourseAuthor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El autor necesita un nombre");
        }
    }
}
