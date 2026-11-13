package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;

import java.util.concurrent.atomic.AtomicInteger;

/** Hasher falso y rápido para tests. Cuenta cuántas veces se ha usado. */
final class FakePasswordHasher implements PasswordHasher {

    private static final String PREFIX = "fake$";

    private final AtomicInteger hashCalls = new AtomicInteger();

    @Override
    public PasswordHash hash(RawPassword password) {
        hashCalls.incrementAndGet();
        return new PasswordHash(PREFIX + password.value());
    }

    @Override
    public boolean matches(RawPassword password, PasswordHash hash) {
        return hash.value().equals(PREFIX + password.value());
    }

    int hashCalls() {
        return hashCalls.get();
    }
}
