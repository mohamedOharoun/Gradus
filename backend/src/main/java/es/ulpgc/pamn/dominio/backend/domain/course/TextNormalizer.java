package es.ulpgc.pamn.dominio.backend.domain.course;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Normalización de texto para <b>comparar</b> (especificación §3.4): opciones, lados de un par,
 * etiquetas y búsqueda. Los textos se guardan y se sirven tal como los escribió el autor.
 */
public final class TextNormalizer {

    /** Cualquier espacio en blanco Unicode (también el de no separación, U+00A0). */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private TextNormalizer() {
    }

    /**
     * NFC, espacios en blanco recortados y agrupados en uno solo, y minúsculas con {@link Locale#ROOT}.
     * Agrupar antes de recortar da el mismo resultado y así ambos pasos usan la misma idea de «espacio».
     */
    public static String normalize(String text) {
        Objects.requireNonNull(text, "text");
        String composed = Normalizer.normalize(text, Normalizer.Form.NFC);
        return WHITESPACE.matcher(composed).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }
}
