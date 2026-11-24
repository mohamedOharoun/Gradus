package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.LoginPassword;
import es.ulpgc.pamn.dominio.backend.domain.RawPassword;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.Objects;

/**
 * Caso de uso: cambiar la contraseña desde Ajustes (decisión G de docs/tareas/ajustes.md).
 * <ol>
 *   <li>Las dos contraseñas deben venir, y la nueva cumplir las reglas del registro ({@link RawPassword}).</li>
 *   <li>La actual debe ser la de la cuenta: quien coja el móvil desbloqueado no puede cambiarla.</li>
 *   <li>Se guarda el hash nuevo y se cierran las sesiones de los <b>otros</b> dispositivos; la de este sigue.</li>
 * </ol>
 * Los pasos van en ese orden para no gastar un hash (PBKDF2, lento a propósito) en datos que no valen.
 */
public final class ChangePassword {

    private static final String CURRENT_FIELD = "currentPassword";
    private static final String NEW_FIELD = "newPassword";

    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordHasher hasher;

    public ChangePassword(UserRepository users, SessionRepository sessions, PasswordHasher hasher) {
        this.users = Objects.requireNonNull(users, "users");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
    }

    /** {@code current} es la sesión con la que se hace la petición: es la única que queda abierta. */
    public ChangePasswordResult change(User user, Session current, ChangePasswordCommand command) {
        return Result.combine(
                        asField(CURRENT_FIELD, LoginPassword.of(command.currentPassword())),
                        asField(NEW_FIELD, RawPassword.of(command.newPassword())),
                        Passwords::new)
                .fold(ChangePasswordResult.Rejected::new, passwords -> changeIfItIsTheirs(user, current, passwords));
    }

    private ChangePasswordResult changeIfItIsTheirs(User user, Session current, Passwords passwords) {
        if (!hasher.matches(passwords.current(), user.passwordHash())) {
            return new ChangePasswordResult.WrongPassword();
        }
        users.update(user.withPasswordHash(hasher.hash(passwords.next())));
        sessions.removeOthersOf(user.id(), current.tokenHash());
        return new ChangePasswordResult.Changed();
    }

    /** Las dos contraseñas ya validadas. */
    private record Passwords(LoginPassword current, RawPassword next) {
    }

    /**
     * {@link LoginPassword} y {@link RawPassword} informan sus errores con el campo «password» (el del
     * registro y el login). Aquí hay dos contraseñas: cada error se reetiqueta con su campo del formulario.
     */
    private static <T> Result<T> asField(String field, Result<T> result) {
        return result.fold(
                errors -> Result.failure(errors.stream()
                        .map(error -> new ValidationError(field, error.code(), error.message()))
                        .toList()),
                Result::success);
    }
}
