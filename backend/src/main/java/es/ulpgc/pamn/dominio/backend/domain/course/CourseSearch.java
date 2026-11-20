package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Criterios de búsqueda de cursos (§4.5), ya normalizados (§3.4). Es una Specification: {@link #matches}
 * define qué significa buscar, y el repositorio JDBC lo traduce a SQL con el mismo significado.
 *
 * @param text     subcadena de título o descripción, literal (un «%» no es un comodín)
 * @param tags     etiquetas que deben estar <b>todas</b>, sin repetir
 * @param language idioma exacto
 */
public record CourseSearch(Optional<String> text, List<String> tags, Optional<String> language) {

    /** Sin filtros: todos los cursos. */
    public static final CourseSearch ANYTHING = new CourseSearch(Optional.empty(), List.of(), Optional.empty());

    public CourseSearch {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(language, "language");
        tags = List.copyOf(tags);
    }

    /** Factory method a partir de los parámetros tal como llegan: normaliza y descarta los vacíos. */
    public static CourseSearch of(String text, List<String> tags, String language) {
        return new CourseSearch(
                Optional.ofNullable(text).map(TextNormalizer::normalize).filter(normalized -> !normalized.isEmpty()),
                Stream.ofNullable(tags).flatMap(List::stream)
                        .filter(Objects::nonNull)
                        .map(TextNormalizer::normalize)
                        .filter(tag -> !tag.isEmpty())
                        .distinct()
                        .toList(),
                Optional.ofNullable(language).filter(code -> !code.isBlank()));
    }

    public boolean matches(CourseMetadata metadata) {
        return text.map(metadata.searchText()::contains).orElse(true)
                && metadata.searchTags().containsAll(tags)
                && language.map(metadata.language()::equals).orElse(true);
    }
}
