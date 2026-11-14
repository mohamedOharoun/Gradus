package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.RegisterUser;
import es.ulpgc.pamn.dominio.backend.application.RegistrationResult;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

/** Traduce HTTP ↔ caso de uso. No contiene reglas de negocio: solo lee el JSON y elige el código de respuesta. */
public final class UserController {

    private final RegisterUser registerUser;

    public UserController(RegisterUser registerUser) {
        this.registerUser = Objects.requireNonNull(registerUser, "registerUser");
    }

    /** {@code POST /api/users}. */
    public void register(Context ctx) {
        RegisterUserRequest request = ctx.bodyAsClass(RegisterUserRequest.class);

        switch (registerUser.register(request.toCommand())) {
            case RegistrationResult.Registered registered -> ctx
                    .status(HttpStatus.CREATED)
                    .json(UserResponse.from(registered.user()));
            case RegistrationResult.Rejected rejected -> ctx
                    .status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse(rejected.errors()));
            case RegistrationResult.EmailAlreadyRegistered ignored -> ctx
                    .status(HttpStatus.CONFLICT)
                    .json(ErrorResponse.of("email", ErrorCode.EMAIL_TAKEN, "Ya existe una cuenta con este correo."));
        }
    }
}
