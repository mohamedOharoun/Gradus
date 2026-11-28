package es.ulpgc.pamn.dominio.backend.web.submission;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lee los ids y la versión que llegan como texto en las rutas de entregas (docs/tareas/ejercicio-foto.md,
 * paso 3e-1). Lo que no tiene la forma esperada da vacío, y el controlador decide qué responder.
 * Mismas reglas que {@code PathIds} de los cursos; no se reutiliza porque es interno a su paquete.
 */
final class SubmissionParams {

    /** Forma canónica: {@code UUID.fromString} sola aceptaría cosas como «1-2-3-4-5». */
    private static final Pattern UUID_FORMAT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    /** Sin ceros a la izquierda, signos ni espacios, y sin desbordar un {@code int}. */
    private static final Pattern POSITIVE = Pattern.compile("[1-9][0-9]{0,8}");

    private SubmissionParams() {
    }

    /** El id de un curso o de una entrega. */
    static Optional<UUID> id(String raw) {
        return Optional.ofNullable(raw).filter(UUID_FORMAT.asMatchPredicate()).map(UUID::fromString);
    }

    /** La versión del curso que estudiaba el alumno (decisión M): un entero positivo. */
    static Optional<Integer> version(String raw) {
        return Optional.ofNullable(raw).filter(POSITIVE.asMatchPredicate()).map(Integer::valueOf);
    }
}
