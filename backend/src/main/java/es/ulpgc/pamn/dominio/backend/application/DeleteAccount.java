package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.LoginPassword;
import es.ulpgc.pamn.dominio.backend.domain.User;

import java.util.Objects;

/**
 * Caso de uso: eliminar la cuenta desde Ajustes (decisiones H e I de docs/tareas/ajustes.md).
 * <ol>
 *   <li>La contraseña debe venir y ser la de la cuenta (confirma que es su dueño quien lo pide).</li>
 *   <li>Si es autora de cursos no se borra: otros los están estudiando.</li>
 *   <li>Si no, se borra el usuario; en PostgreSQL sus sesiones caen con él ({@code ON DELETE CASCADE}).</li>
 * </ol>
 * La contraseña se comprueba antes que la autoría: a quien no la sabe no se le cuenta nada de la cuenta.
 */
public final class DeleteAccount {

    private final UserRepository users;
    private final PasswordHasher hasher;

    public DeleteAccount(UserRepository users, PasswordHasher hasher) {
        this.users = Objects.requireNonNull(users, "users");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
    }

    public DeleteAccountResult delete(User user, DeleteAccountCommand command) {
        return LoginPassword.of(command.password())
                .fold(DeleteAccountResult.Rejected::new, password -> deleteIfItIsTheirs(user, password));
    }

    private DeleteAccountResult deleteIfItIsTheirs(User user, LoginPassword password) {
        if (!hasher.matches(password, user.passwordHash())) {
            return new DeleteAccountResult.WrongPassword();
        }
        return users.removeUnlessAuthor(user.id())
                ? new DeleteAccountResult.Deleted()
                : new DeleteAccountResult.AuthorOfCourses();
    }
}
