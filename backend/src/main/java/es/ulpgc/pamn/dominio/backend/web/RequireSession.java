package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.Authenticate;
import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import java.util.Objects;

/**
 * Decorator de rutas protegidas: antes de llamar al handler comprueba el token de la petición.
 * Si no identifica a nadie, responde 401 y el handler no llega a ejecutarse.
 */
public final class RequireSession {

    private final Authenticate authenticate;

    public RequireSession(Authenticate authenticate) {
        this.authenticate = Objects.requireNonNull(authenticate, "authenticate");
    }

    /** Envuelve {@code handler} para que solo lo alcancen peticiones con una sesión activa. */
    public Handler around(AuthenticatedHandler handler) {
        return ctx -> {
            AuthenticationResult result = BearerToken.from(ctx.header("Authorization"))
                    .map(authenticate::authenticate)
                    .orElseGet(AuthenticationResult.Unauthenticated::new);
            switch (result) {
                case AuthenticationResult.Authenticated authenticated -> handler.handle(ctx, authenticated);
                case AuthenticationResult.Unauthenticated ignored -> rejectAsUnauthenticated(ctx);
            }
        };
    }

    private static void rejectAsUnauthenticated(Context ctx) {
        ctx.status(HttpStatus.UNAUTHORIZED)
                // RFC 6750: un 401 indica con qué esquema hay que autenticarse.
                .header("WWW-Authenticate", "Bearer")
                .json(ErrorResponse.of("authorization", ErrorCode.UNAUTHENTICATED,
                        "La sesión no es válida o ha caducado. Inicia sesión de nuevo."));
    }
}
