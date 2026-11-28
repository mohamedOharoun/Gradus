package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Una página de resultados: sus elementos, qué página es y cuántos resultados hay en total (§4.5).
 *
 * @param total resultados de todas las páginas juntas, para que la app sepa cuántas hay
 */
public record Page<T>(List<T> items, PageRequest request, long total) {

    public Page {
        items = List.copyOf(items);
        Objects.requireNonNull(request, "request");
        if (items.size() > request.size()) {
            throw new IllegalArgumentException("Una página de " + request.size() + " no puede tener "
                    + items.size() + " elementos");
        }
        if (total < items.size()) {
            throw new IllegalArgumentException("El total (" + total + ") no puede ser menor que los elementos de "
                    + "la página (" + items.size() + ")");
        }
    }

    /** Functor: la misma página con cada elemento transformado (por ejemplo, a su DTO). */
    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(items.stream().<R>map(mapper).toList(), request, total);
    }
}
