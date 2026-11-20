package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Metadatos de una versión publicada (§4.1). Son copias derivadas del JSON, que es la fuente de verdad:
 * se crean con {@link #of} al publicar y nunca se editan por separado. Los campos {@code search*} son las
 * versiones normalizadas (§3.4) para buscar; los demás se sirven tal como los escribió el autor.
 */
public record CourseMetadata(int formatVersion, String title, String description, String language,
                             List<String> tags, List<String> searchTags, String searchText, int moduleCount,
                             String sha256, int sizeBytes) {

    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    public CourseMetadata {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(searchText, "searchText");
        Objects.requireNonNull(sha256, "sha256");
        tags = List.copyOf(tags);
        searchTags = List.copyOf(searchTags);
        if (searchTags.size() != tags.size()) {
            throw new IllegalArgumentException("Cada etiqueta necesita su versión normalizada para buscar");
        }
        if (moduleCount < CourseDocument.MIN_MODULES) {
            throw new IllegalArgumentException("Un curso tiene al menos un módulo");
        }
        if (!SHA_256_HEX.matcher(sha256).matches()) {
            throw new IllegalArgumentException("No es un SHA-256 en hexadecimal en minúsculas: " + sha256);
        }
        if (sizeBytes < 1 || sizeBytes > CourseContent.MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Tamaño fuera de rango: " + sizeBytes);
        }
    }

    /** Factory method: los metadatos de un curso y del texto exacto que se guarda. */
    public static CourseMetadata of(CourseDocument document, CourseContent content) {
        return new CourseMetadata(
                document.formatVersion(),
                document.title(),
                document.description(),
                document.language(),
                document.tags(),
                document.tags().stream().map(TextNormalizer::normalize).toList(),
                TextNormalizer.normalize(document.title() + " " + document.description()),
                document.modules().size(),
                content.sha256(),
                content.sizeBytes());
    }
}
