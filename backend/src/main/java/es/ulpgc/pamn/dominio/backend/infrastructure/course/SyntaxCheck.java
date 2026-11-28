package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.JsonNode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;

/**
 * Resultado de la capa 1: el texto y su árbol JSON, o el problema que impide leerlo. El analizador se
 * detiene en el primer fallo, así que un fallo de sintaxis es siempre un único problema.
 */
sealed interface SyntaxCheck permits SyntaxCheck.Passed, SyntaxCheck.Failed {

    record Passed(CourseContent content, JsonNode tree) implements SyntaxCheck {
    }

    record Failed(Issue issue) implements SyntaxCheck {
    }
}
