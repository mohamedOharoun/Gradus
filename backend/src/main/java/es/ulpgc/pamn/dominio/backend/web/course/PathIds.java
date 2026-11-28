package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.web.CanonicalUuid;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lee los parámetros de las rutas de cursos. Un {@code {id}} que no es un UUID, o una {@code {version}} que no
 * es un entero positivo, no identifican ningún recurso: vacío, y el controlador responde 404 (§4.3).
 */
final class PathIds {

    /** Sin ceros a la izquierda ni signos, y sin desbordar un {@code int}. */
    private static final Pattern POSITIVE = Pattern.compile("[1-9][0-9]{0,8}");

    private PathIds() {
    }

    static Optional<UUID> courseId(String raw) {
        return CanonicalUuid.parse(raw);
    }

    static Optional<Integer> version(String raw) {
        return Optional.ofNullable(raw).filter(POSITIVE.asMatchPredicate()).map(Integer::valueOf);
    }
}
