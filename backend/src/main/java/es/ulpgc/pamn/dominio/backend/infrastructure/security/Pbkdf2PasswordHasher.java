package es.ulpgc.pamn.dominio.backend.infrastructure.security;

import es.ulpgc.pamn.dominio.backend.application.PasswordHasher;
import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** {@link PasswordHasher} con PBKDF2-HMAC-SHA256 del propio JDK y una sal aleatoria por contraseña. */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    /** Recomendación de OWASP para PBKDF2-HMAC-SHA256. */
    public static final int DEFAULT_ITERATIONS = 600_000;

    private static final String JCA_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    private Pbkdf2PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas");
        }
        this.iterations = iterations;
    }

    public static Pbkdf2PasswordHasher withDefaults() {
        return new Pbkdf2PasswordHasher(DEFAULT_ITERATIONS);
    }

    public static Pbkdf2PasswordHasher withIterations(int iterations) {
        return new Pbkdf2PasswordHasher(iterations);
    }

    @Override
    public PasswordHash hash(RawPassword password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return new PasswordHash(new Pbkdf2Hash(iterations, salt, derive(password, salt, iterations)).encode());
    }

    @Override
    public boolean matches(RawPassword password, PasswordHash hash) {
        return Pbkdf2Hash.decode(hash.value())
                // Se usan la sal y las iteraciones guardadas, no las actuales.
                .map(stored -> MessageDigest.isEqual(stored.hash(), derive(password, stored.salt(), stored.iterations())))
                .orElse(false);
    }

    private static byte[] derive(RawPassword password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.value().toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(JCA_ALGORITHM + " no está disponible en este JDK", e);
        } finally {
            spec.clearPassword();
        }
    }
}
