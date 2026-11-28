package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.LogInResult;

/**
 * Respuesta de un inicio de sesión correcto. La app guarda {@code token} y lo envía en
 * {@code Authorization: Bearer <token>}; {@code expiresAt} le dice hasta cuándo vale si no lo usa.
 */
public record SessionResponse(String token, String expiresAt, UserResponse user) {

    public static SessionResponse from(LogInResult.LoggedIn loggedIn) {
        return new SessionResponse(
                loggedIn.token().value(),
                loggedIn.session().expiresAt().toString(), // ISO-8601, como createdAt
                UserResponse.from(loggedIn.user()));
    }

    /** Quien tenga el token tiene la sesión: no debe acabar en los logs. */
    @Override
    public String toString() {
        return "SessionResponse[token=****, expiresAt=" + expiresAt + ", user=" + user + "]";
    }
}
