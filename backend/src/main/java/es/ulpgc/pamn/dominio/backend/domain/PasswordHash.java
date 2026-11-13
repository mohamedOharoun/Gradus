package es.ulpgc.pamn.dominio.backend.domain;

import java.util.Objects;

/**
 * Hash de una contraseña, en formato autodescriptivo {@code algoritmo$iteraciones$sal$hash}.
 * Su formato concreto lo decide el hasher (paso 4); aquí solo se garantiza que no esté vacío.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("El hash de la contraseña no puede estar vacío");
        }
    }

    /** Aunque no sea la contraseña, el hash tampoco debe acabar en los logs. */
    @Override
    public String toString() {
        return "PasswordHash[****]";
    }
}
