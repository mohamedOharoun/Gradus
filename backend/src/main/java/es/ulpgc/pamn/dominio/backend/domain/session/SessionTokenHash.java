package es.ulpgc.pamn.dominio.backend.domain.session;

import java.util.Objects;
import java.util.regex.Pattern;

/** Lo que se guarda en la base de datos en lugar del token: su SHA-256 en hexadecimal. */
public record SessionTokenHash(String value) {

    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    public SessionTokenHash {
        Objects.requireNonNull(value, "value");
        if (!SHA_256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("No es un SHA-256 en hexadecimal en minúsculas");
        }
    }
}
