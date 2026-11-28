package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Objects;

/**
 * Un curso tal como lo escribe su autor (formato v1, §2). No lleva id, versión, autor ni fechas: los
 * asigna el servidor al publicar. Las listas conservan el orden del documento, así que sus índices
 * son los de las rutas JSON Pointer de los problemas ({@code /modules/1/review/3}).
 *
 * @param defaultMastery {@code defaults.mastery} del fichero: la prueba de los módulos sin prueba propia
 */
public record CourseDocument(int formatVersion, String title, String description, String language,
                             List<String> tags, Mastery defaultMastery, List<Module> modules) {

    /** Única versión del formato que existe. Es la del fichero, no la del curso. */
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MIN_TAGS = 1;
    public static final int MAX_TAGS = 10;
    public static final int MAX_TAG_LENGTH = 32;
    public static final int MIN_MODULES = 1;
    public static final int MAX_MODULES = 100;

    public CourseDocument {
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Versión de formato no soportada: " + formatVersion);
        }
        Require.plainText(title, "title", MAX_TITLE_LENGTH);
        Require.plainText(description, "description", MAX_DESCRIPTION_LENGTH);
        Require.language(language, "language");
        tags = Require.distinct(Require.size(tags, "tags", MIN_TAGS, MAX_TAGS), "tags");
        tags.forEach(tag -> Require.plainText(tag, "tags", MAX_TAG_LENGTH));
        Objects.requireNonNull(defaultMastery, "defaultMastery");
        modules = Require.size(modules, "modules", MIN_MODULES, MAX_MODULES);
    }
}
