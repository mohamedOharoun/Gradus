package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lee el token de la cabecera {@code Authorization: Bearer <token>} (RFC 6750). Función pura. */
final class BearerToken {

    // Esquema sin distinguir mayúsculas, uno o más espacios y un b64token (caracteres de la RFC 6750).
    private static final Pattern BEARER = Pattern.compile("(?i)^Bearer +([A-Za-z0-9\\-._~+/]+=*) *$");

    private BearerToken() {
    }

    /** El token, o vacío si la cabecera falta o no es de tipo Bearer. */
    static Optional<SessionToken> from(String authorizationHeader) {
        if (authorizationHeader == null) {
            return Optional.empty();
        }
        Matcher matcher = BEARER.matcher(authorizationHeader);
        return matcher.matches() ? Optional.of(new SessionToken(matcher.group(1))) : Optional.empty();
    }
}
