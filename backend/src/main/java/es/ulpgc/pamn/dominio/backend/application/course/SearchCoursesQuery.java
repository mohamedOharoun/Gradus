package es.ulpgc.pamn.dominio.backend.application.course;

import java.util.List;

/**
 * Los parámetros de la búsqueda tal como llegan (§4.5), sin interpretar: cualquiera puede ser nulo. Como
 * en {@code RegisterUserCommand}, es el caso de uso quien decide si son válidos.
 *
 * @param text     {@code q}
 * @param tags     {@code tag}, que puede repetirse
 * @param page     número de página desde 0, como texto
 * @param size     resultados por página, como texto
 */
public record SearchCoursesQuery(String text, List<String> tags, String language, String page, String size) {
}
