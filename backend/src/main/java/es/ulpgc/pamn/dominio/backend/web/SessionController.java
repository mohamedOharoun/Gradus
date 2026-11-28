package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.application.LogIn;
import es.ulpgc.pamn.dominio.backend.application.LogInResult;
import es.ulpgc.pamn.dominio.backend.application.LogOut;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

/** Traduce HTTP ↔ casos de uso de sesión. No contiene reglas de negocio. */
public final class SessionController {

    private final LogIn logIn;
    private final LogOut logOut;

    public SessionController(LogIn logIn, LogOut logOut) {
        this.logIn = Objects.requireNonNull(logIn, "logIn");
        this.logOut = Objects.requireNonNull(logOut, "logOut");
    }

    /** {@code POST /api/sessions}. */
    public void logIn(Context ctx) {
        LogInRequest request = ctx.bodyAsClass(LogInRequest.class);

        switch (logIn.logIn(request.toCommand())) {
            case LogInResult.LoggedIn loggedIn -> ctx
                    .status(HttpStatus.CREATED)
                    // Ningún proxy ni caché intermedia debe guardar una respuesta con un token.
                    .header("Cache-Control", "no-store")
                    .json(SessionResponse.from(loggedIn));
            case LogInResult.Rejected rejected -> ctx
                    .status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse(rejected.errors()));
            case LogInResult.InvalidCredentials ignored -> ctx
                    .status(HttpStatus.UNAUTHORIZED)
                    .json(ErrorResponse.of("credentials", ErrorCode.INVALID_CREDENTIALS,
                            "Correo o contraseña incorrectos."));
        }
    }

    /** {@code DELETE /api/sessions/current}: cierra la sesión del token. Protegida con {@link RequireSession}. */
    public void logOut(Context ctx, AuthenticationResult.Authenticated authenticated) {
        logOut.logOut(authenticated.session());
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
