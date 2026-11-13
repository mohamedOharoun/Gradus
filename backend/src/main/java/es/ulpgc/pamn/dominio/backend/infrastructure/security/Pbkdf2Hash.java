package es.ulpgc.pamn.dominio.backend.infrastructure.security;

import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Un hash PBKDF2 con todo lo necesario para verificarlo, y su forma en texto:
 * {@code pbkdf2_sha256$iteraciones$sal$hash} (sal y hash en Base64).
 */
record Pbkdf2Hash(int iterations, byte[] salt, byte[] hash) {

    static final String ALGORITHM_ID = "pbkdf2_sha256";

    private static final String SEPARATOR = "$";
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile(Pattern.quote(SEPARATOR));
    private static final int PARTS = 4;

    Pbkdf2Hash {
        if (iterations < 1) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas");
        }
        if (salt.length == 0 || hash.length == 0) {
            throw new IllegalArgumentException("La sal y el hash no pueden estar vacíos");
        }
        // Copias defensivas: un array es mutable y quien nos lo pasó podría cambiarlo después.
        salt = salt.clone();
        hash = hash.clone();
    }

    @Override
    public byte[] salt() {
        return salt.clone();
    }

    @Override
    public byte[] hash() {
        return hash.clone();
    }

    String encode() {
        Base64.Encoder base64 = Base64.getEncoder();
        return String.join(SEPARATOR, ALGORITHM_ID, Integer.toString(iterations),
                base64.encodeToString(salt), base64.encodeToString(hash));
    }

    /** Devuelve vacío si el texto no tiene el formato esperado, en lugar de lanzar una excepción. */
    static Optional<Pbkdf2Hash> decode(String encoded) {
        String[] parts = SEPARATOR_PATTERN.split(encoded);
        if (parts.length != PARTS || !ALGORITHM_ID.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            Base64.Decoder base64 = Base64.getDecoder();
            return Optional.of(new Pbkdf2Hash(Integer.parseInt(parts[1]), base64.decode(parts[2]), base64.decode(parts[3])));
        } catch (IllegalArgumentException malformed) {
            // Número inválido (NumberFormatException), Base64 inválido o valores fuera de rango.
            return Optional.empty();
        }
    }
}
