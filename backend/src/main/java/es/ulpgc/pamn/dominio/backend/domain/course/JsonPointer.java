package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ruta a una posición del documento en formato JSON Pointer (RFC 6901), por ejemplo
 * {@code /modules/1/review/3}. Inmutable: {@link #key} e {@link #index} devuelven una ruta nueva.
 */
public record JsonPointer(List<String> tokens) {

    private static final JsonPointer ROOT = new JsonPointer(List.of());

    public JsonPointer {
        tokens = List.copyOf(tokens);
    }

    /** La raíz del documento; su representación es la cadena vacía. */
    public static JsonPointer root() {
        return ROOT;
    }

    /** La ruta a la propiedad {@code name} del objeto al que apunta esta ruta. */
    public JsonPointer key(String name) {
        return append(Objects.requireNonNull(name, "name"));
    }

    /** La ruta al elemento {@code position} (desde 0) de la lista a la que apunta esta ruta. */
    public JsonPointer index(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Un índice no puede ser negativo: " + position);
        }
        return append(Integer.toString(position));
    }

    private JsonPointer append(String token) {
        return new JsonPointer(Stream.concat(tokens.stream(), Stream.of(token)).toList());
    }

    @Override
    public String toString() {
        return tokens.stream().map(token -> "/" + escape(token)).collect(Collectors.joining());
    }

    /** RFC 6901 §3: primero «~» y después «/», para no convertir el «~1» recién escrito en «~01». */
    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }
}
