package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;

/** Puerto de salida (Strategy): cómo se hashean y verifican las contraseñas. */
public interface PasswordHasher {

    /** Devuelve un hash con sal aleatoria: dos llamadas con la misma contraseña dan hashes distintos. */
    PasswordHash hash(RawPassword password);

    /** Comprueba si la contraseña corresponde al hash (lo usará el inicio de sesión). */
    boolean matches(RawPassword password, PasswordHash hash);
}
