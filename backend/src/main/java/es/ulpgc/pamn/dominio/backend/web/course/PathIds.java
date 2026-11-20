package es.ulpgc.pamn.dominio.backend.web.course;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lee los parámetros de las rutas de cursos. Un {@code {id}} que no es un UUID, o una {@code {version}} que no
 * es un entero positivo, no identifican ningún recurso: vacío, y el controlador responde 404 (§4.3).
 */
final class PathIds {

    /** Forma canónica: {@code UUID.fromString} sola aceptaría cosas como «1-2-3-4-5». */
    private static final Pattern UUID_FORMAT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    /** Sin ceros a la izquierda ni signos, y sin desbordar un {@code int}. */
    private static final Pattern POSITIVE = Pattern.compile("[1-9][0-9]{0,8}");

    private PathIds() {
    }

    static Optional<UUID> courseId(String raw) {
        return Optional.ofNullable(raw).filter(UUID_FORMAT.asMatchPredicate()).map(UUID::fromString);
    }

    static Optional<Integer> version(String raw) {
        return Optional.ofNullable(raw).filter(POSITIVE.asMatchPredicate()).map(Integer::valueOf);
    }
}
