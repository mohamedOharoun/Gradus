package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.LoginPassword;
import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;

/** Puerto de salida (Strategy): cómo se hashean y verifican las contraseñas. */
public interface PasswordHasher {

    /** Devuelve un hash con sal aleatoria: dos llamadas con la misma contraseña dan hashes distintos. */
    PasswordHash hash(RawPassword password);

    /** Comprueba si lo escrito al iniciar sesión corresponde al hash guardado. */
    boolean matches(LoginPassword password, PasswordHash hash);

    /**
     * Hash señuelo con el que no coincide ninguna contraseña, pero que cuesta lo mismo comprobar
     * que uno real. Se usa cuando el correo no existe, para que el tiempo de respuesta no lo delate.
     */
    PasswordHash decoyHash();
}
