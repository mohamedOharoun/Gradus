package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;

import java.util.Objects;

/** Si el token de una petición identifica a alguien. La capa web traduce cada caso a un código HTTP. */
public sealed interface AuthenticationResult {

    /** Token válido: quién es y su sesión ya renovada. */
    record Authenticated(User user, Session session) implements AuthenticationResult {
        public Authenticated {
            Objects.requireNonNull(user, "user");
            Objects.requireNonNull(session, "session");
        }
    }

    /** Token desconocido, caducado o de un usuario que ya no existe. No se distingue, a propósito. */
    record Unauthenticated() implements AuthenticationResult {
    }
}
