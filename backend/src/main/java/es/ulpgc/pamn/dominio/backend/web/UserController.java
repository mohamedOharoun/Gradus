package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.application.ChangePassword;
import es.ulpgc.pamn.dominio.backend.application.ChangePasswordResult;
import es.ulpgc.pamn.dominio.backend.application.DeleteAccount;
import es.ulpgc.pamn.dominio.backend.application.DeleteAccountResult;
import es.ulpgc.pamn.dominio.backend.application.RegisterUser;
import es.ulpgc.pamn.dominio.backend.application.RegistrationResult;
import es.ulpgc.pamn.dominio.backend.application.RenameResult;
import es.ulpgc.pamn.dominio.backend.application.RenameUser;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

/** Traduce HTTP ↔ caso de uso. No contiene reglas de negocio: solo lee el JSON y elige el código de respuesta. */
public final class UserController {

    private final RegisterUser registerUser;
    private final RenameUser renameUser;
    private final ChangePassword changePassword;
    private final DeleteAccount deleteAccount;

    public UserController(RegisterUser registerUser, RenameUser renameUser, ChangePassword changePassword,
                          DeleteAccount deleteAccount) {
        this.registerUser = Objects.requireNonNull(registerUser, "registerUser");
        this.renameUser = Objects.requireNonNull(renameUser, "renameUser");
        this.changePassword = Objects.requireNonNull(changePassword, "changePassword");
        this.deleteAccount = Objects.requireNonNull(deleteAccount, "deleteAccount");
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

    /** {@code GET /api/users/me}: quién es el dueño del token. Protegida con {@link RequireSession}. */
    public void me(Context ctx, AuthenticationResult.Authenticated authenticated) {
        ctx.json(UserResponse.from(authenticated.user()));
    }

    /** {@code PATCH /api/users/me}: cambia el nombre del dueño del token. Protegida con {@link RequireSession}. */
    public void rename(Context ctx, AuthenticationResult.Authenticated authenticated) {
        RenameUserRequest request = ctx.bodyAsClass(RenameUserRequest.class);

        switch (renameUser.rename(authenticated.user(), request.name())) {
            case RenameResult.Renamed renamed -> ctx.json(UserResponse.from(renamed.user()));
            case RenameResult.Rejected rejected -> ctx
                    .status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse(rejected.errors()));
        }
    }

    /**
     * {@code PUT /api/users/me/password}: cambia la contraseña y cierra las sesiones de los demás
     * dispositivos (la de esta petición sigue). Protegida con {@link RequireSession}.
     */
    public void changePassword(Context ctx, AuthenticationResult.Authenticated authenticated) {
        ChangePasswordRequest request = ctx.bodyAsClass(ChangePasswordRequest.class);

        switch (changePassword.change(authenticated.user(), authenticated.session(), request.toCommand())) {
            case ChangePasswordResult.Changed ignored -> ctx.status(HttpStatus.NO_CONTENT);
            case ChangePasswordResult.Rejected rejected -> ctx
                    .status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse(rejected.errors()));
            case ChangePasswordResult.WrongPassword ignored -> ctx
                    .status(HttpStatus.FORBIDDEN)
                    .json(ErrorResponse.of("currentPassword", ErrorCode.WRONG_PASSWORD,
                            "La contraseña actual no es correcta."));
        }
    }

    /**
     * {@code DELETE /api/users/me} con {@code {"password": "..."}}: elimina la cuenta y sus sesiones,
     * salvo que sea autora de cursos. Protegida con {@link RequireSession}.
     */
    public void deleteAccount(Context ctx, AuthenticationResult.Authenticated authenticated) {
        DeleteAccountRequest request = ctx.bodyAsClass(DeleteAccountRequest.class);

        switch (deleteAccount.delete(authenticated.user(), request.toCommand())) {
            case DeleteAccountResult.Deleted ignored -> ctx.status(HttpStatus.NO_CONTENT);
            case DeleteAccountResult.Rejected rejected -> ctx
                    .status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse(rejected.errors()));
            case DeleteAccountResult.WrongPassword ignored -> ctx
                    .status(HttpStatus.FORBIDDEN)
                    .json(ErrorResponse.of("password", ErrorCode.WRONG_PASSWORD, "La contraseña no es correcta."));
            case DeleteAccountResult.AuthorOfCourses ignored -> ctx
                    .status(HttpStatus.CONFLICT)
                    .json(ErrorResponse.of("account", ErrorCode.AUTHOR_OF_COURSES,
                            "No se puede eliminar una cuenta que ha publicado cursos: otras personas los están estudiando."));
        }
    }
}
