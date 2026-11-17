package es.ulpgc.pamn.dominio.backend.domain.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Token que recibe la app al iniciar sesión y envía en cada petición. Es opaco: no lleva datos
 * dentro, solo azar. El servidor nunca lo guarda; guarda su {@link #hash()}.
 */
public record SessionToken(String value) {

    private static final int RANDOM_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_SAFE = Base64.getUrlEncoder().withoutPadding();

    public SessionToken {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("El token no puede estar vacío");
        }
    }

    /** Factory method: 256 bits de azar criptográfico, imposibles de adivinar probando. */
    public static SessionToken generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return new SessionToken(URL_SAFE.encodeToString(bytes));
    }

    /**
     * Huella SHA-256 del token. Basta un hash rápido (no PBKDF2) porque el token es aleatorio y
     * largo: no hay diccionario con el que probar, a diferencia de las contraseñas.
     */
    public SessionTokenHash hash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return new SessionTokenHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en este JDK", e);
        }
    }

    /** Quien tenga el token tiene la sesión: no debe acabar en los logs. */
    @Override
    public String toString() {
        return "SessionToken[****]";
    }
}
