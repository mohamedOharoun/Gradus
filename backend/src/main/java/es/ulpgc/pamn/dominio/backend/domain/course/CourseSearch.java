package es.ulpgc.pamn.dominio.backend.domain.course;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Criterios de búsqueda de cursos (§4.5), ya normalizados (§3.4). Es una Specification: {@link #matches}
 * define qué significa buscar, y el repositorio JDBC lo traduce a SQL con el mismo significado.
 * Es inmutable: {@link #withAuthor} y {@link #publishedSince} devuelven una copia (<i>withers</i>).
 *
 * @param text           subcadena de título o descripción, literal (un «%» no es un comodín)
 * @param tags           etiquetas que deben estar <b>todas</b>, sin repetir
 * @param language       idioma exacto
 * @param author         subcadena del nombre del autor, sin distinguir mayúsculas y literal; no se quitan
 *                       las tildes («angel» no encuentra a «Ángel»)
 * @param publishedSince instante desde el que se publicó, <b>incluido</b>; se guarda el instante y no la
 *                       palabra {@code month}, para que {@link #matches} no dependa de la hora
 */
public record CourseSearch(Optional<String> text, List<String> tags, Optional<String> language,
                           Optional<String> author, Optional<Instant> publishedSince) {

    /** Sin filtros: todos los cursos. */
    public static final CourseSearch ANYTHING =
            new CourseSearch(Optional.empty(), List.of(), Optional.empty(), Optional.empty(), Optional.empty());

    public CourseSearch {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(publishedSince, "publishedSince");
        tags = List.copyOf(tags);
        // PostgreSQL guarda microsegundos: truncando aquí, Java y SQL comparan exactamente lo mismo.
        publishedSince = publishedSince.map(since -> since.truncatedTo(ChronoUnit.MICROS));
    }

    /**
     * Factory method a partir de los parámetros tal como llegan: normaliza y descarta los vacíos. El autor
     * y la fecha quedan vacíos; se añaden con {@link #withAuthor} y {@link #publishedSince}, porque tres
     * {@code String} seguidos en un solo método se podrían confundir sin que el compilador avisara.
     */
    public static CourseSearch of(String text, List<String> tags, String language) {
        return new CourseSearch(
                Optional.ofNullable(text).map(TextNormalizer::normalize).filter(normalized -> !normalized.isEmpty()),
                Stream.ofNullable(tags).flatMap(List::stream)
                        .filter(Objects::nonNull)
                        .map(TextNormalizer::normalize)
                        .filter(tag -> !tag.isEmpty())
                        .distinct()
                        .toList(),
                Optional.ofNullable(language).filter(code -> !code.isBlank()),
                Optional.empty(),
                Optional.empty());
    }

    /** Copia con el filtro de autor: se normaliza como el texto; nulo o en blanco lo quita. */
    public CourseSearch withAuthor(String raw) {
        Optional<String> normalized = Optional.ofNullable(raw)
                .map(TextNormalizer::normalize)
                .filter(name -> !name.isEmpty());
        return new CourseSearch(text, tags, language, normalized, publishedSince);
    }

    /** Copia que solo admite cursos publicados desde {@code since} (incluido). No admite nulo. */
    public CourseSearch publishedSince(Instant since) {
        Objects.requireNonNull(since, "since");
        return new CourseSearch(text, tags, language, author, Optional.of(since));
    }

    /**
     * Si el curso cumple <b>todos</b> los criterios. Recibe el resumen y no los metadatos, porque el
     * autor y la fecha de publicación están en el resumen.
     */
    public boolean matches(CourseSummary summary) {
        CourseMetadata metadata = summary.metadata();
        return text.map(metadata.searchText()::contains).orElse(true)
                && metadata.searchTags().containsAll(tags)
                && language.map(metadata.language()::equals).orElse(true)
                // Locale.ROOT, como TextNormalizer: no depende de la configuración regional del servidor.
                && author.map(summary.author().name().toLowerCase(Locale.ROOT)::contains).orElse(true)
                && publishedSince.map(since -> !summary.publishedAt().isBefore(since)).orElse(true);
    }
}
