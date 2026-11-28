package es.ulpgc.pamn.dominio.backend.web;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validación estricta de UUID en su forma canónica ({@code 8-4-4-4-12} dígitos hexadecimales,
 * mayúsculas o minúsculas). Función pura compartida entre las rutas que reciben un UUID en la URL:
 * {@link UUID#fromString(String)} sola aceptaría formatos abreviados como {@code 1-2-3-4-5}.
 */
public final class CanonicalUuid {

    private static final Pattern FORMAT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private CanonicalUuid() {
    }

    /** El {@link UUID} si {@code raw} cumple exactamente el formato canónico; vacío en caso contrario. */
    public static Optional<UUID> parse(String raw) {
        return Optional.ofNullable(raw).filter(FORMAT.asMatchPredicate()).map(UUID::fromString);
    }
}
