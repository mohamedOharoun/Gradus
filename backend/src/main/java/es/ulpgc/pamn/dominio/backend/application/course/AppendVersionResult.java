package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;

import java.util.Objects;

/**
 * Lo que responde el repositorio al añadir una versión (§4.2). Las tres comprobaciones se hacen con la
 * fila del curso bloqueada, igual que {@code UserRepository.add} comprueba e inserta de una vez.
 */
public sealed interface AppendVersionResult {

    /** Guardada como la versión siguiente a la última. */
    record Appended(CourseSummary course) implements AppendVersionResult {
        public Appended {
            Objects.requireNonNull(course, "course");
        }
    }

    record CourseNotFound() implements AppendVersionResult {
    }

    /** Quien publica no es el autor del curso. */
    record NotAuthor() implements AppendVersionResult {
    }

    /** El contenido es idéntico (mismo SHA-256) al de la última versión: no se crea ninguna. */
    record IdenticalToLatest() implements AppendVersionResult {
    }
}
