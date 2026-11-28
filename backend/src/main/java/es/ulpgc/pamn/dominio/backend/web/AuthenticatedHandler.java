package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import io.javalin.http.Context;

/** Como {@link io.javalin.http.Handler}, pero solo se llama cuando ya se sabe quién hace la petición. */
@FunctionalInterface
public interface AuthenticatedHandler {

    void handle(Context ctx, AuthenticationResult.Authenticated authenticated) throws Exception;
}
