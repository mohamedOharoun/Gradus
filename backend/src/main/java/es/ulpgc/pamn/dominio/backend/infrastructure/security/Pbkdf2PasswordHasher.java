package es.ulpgc.pamn.dominio.backend.infrastructure.security;

import es.ulpgc.pamn.dominio.backend.application.PasswordHasher;
import es.ulpgc.pamn.dominio.backend.domain.LoginPassword;
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
    private final PasswordHash decoy;

    private Pbkdf2PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas");
        }
        this.iterations = iterations;
        // Sal y hash al azar: tiene el formato y las iteraciones de uno real, pero no procede de
        // ninguna contraseña, así que nada coincide con él (salvo con probabilidad 1 entre 2^256).
        this.decoy = new PasswordHash(new Pbkdf2Hash(iterations, randomBytes(SALT_BYTES), randomBytes(HASH_BITS / 8)).encode());
    }

    public static Pbkdf2PasswordHasher withDefaults() {
        return new Pbkdf2PasswordHasher(DEFAULT_ITERATIONS);
    }

    public static Pbkdf2PasswordHasher withIterations(int iterations) {
        return new Pbkdf2PasswordHasher(iterations);
    }

    @Override
    public PasswordHash hash(RawPassword password) {
        byte[] salt = randomBytes(SALT_BYTES);
        return new PasswordHash(new Pbkdf2Hash(iterations, salt, derive(password.value(), salt, iterations)).encode());
    }

    @Override
    public boolean matches(LoginPassword password, PasswordHash hash) {
        return Pbkdf2Hash.decode(hash.value())
                // Se usan la sal y las iteraciones guardadas, no las actuales.
                .map(stored -> MessageDigest.isEqual(stored.hash(), derive(password.value(), stored.salt(), stored.iterations())))
                .orElse(false);
    }

    @Override
    public PasswordHash decoyHash() {
        return decoy;
    }

    private byte[] randomBytes(int count) {
        byte[] bytes = new byte[count];
        random.nextBytes(bytes);
        return bytes;
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(JCA_ALGORITHM + " no está disponible en este JDK", e);
        } finally {
            spec.clearPassword();
        }
    }
}
