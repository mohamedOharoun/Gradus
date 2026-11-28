package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Busca repeticiones en una lista según una clave (el id, el texto normalizado…). Lo comparten todas
 * las reglas «DUPLICATE_*»: cada aparición posterior a la primera es un problema, en orden de documento.
 */
final class Duplicates {

    /** Una aparición repetida ({@code repeated}) y la primera con la misma clave ({@code first}). */
    record Repeat<T>(Located<T> first, Located<T> repeated) {
    }

    private Duplicates() {
    }

    static <T, K> Stream<Repeat<T>> by(List<Located<T>> items, Function<? super T, ? extends K> key) {
        Function<Located<T>, K> keyOf = item -> key.apply(item.value());
        Map<K, Located<T>> firsts = items.stream()
                .collect(Collectors.toMap(keyOf, Function.identity(), (first, later) -> first));
        return items.stream()
                .filter(item -> firsts.get(keyOf.apply(item)) != item)
                .map(item -> new Repeat<>(firsts.get(keyOf.apply(item)), item));
    }
}
