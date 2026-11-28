package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.LoginPassword;
import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;

import java.util.concurrent.atomic.AtomicInteger;

/** Hasher falso y rápido para tests. Cuenta cuántas veces se ha hasheado y comparado. */
final class FakePasswordHasher implements PasswordHasher {

    private static final String PREFIX = "fake$";
    /** No empieza por {@link #PREFIX}, así que ninguna contraseña coincide con él. */
    static final PasswordHash DECOY = new PasswordHash("decoy$");

    private final AtomicInteger hashCalls = new AtomicInteger();
    private final AtomicInteger matchesCalls = new AtomicInteger();

    /** El hash que guardaría este hasher para {@code password}, sin aplicar reglas de fortaleza. */
    static PasswordHash hashOf(String password) {
        return new PasswordHash(PREFIX + password);
    }

    @Override
    public PasswordHash hash(RawPassword password) {
        hashCalls.incrementAndGet();
        return hashOf(password.value());
    }

    @Override
    public boolean matches(LoginPassword password, PasswordHash hash) {
        matchesCalls.incrementAndGet();
        return hash.equals(hashOf(password.value()));
    }

    @Override
    public PasswordHash decoyHash() {
        return DECOY;
    }

    int hashCalls() {
        return hashCalls.get();
    }

    int matchesCalls() {
        return matchesCalls.get();
    }
}
