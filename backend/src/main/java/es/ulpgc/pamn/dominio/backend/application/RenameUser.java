package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;

import java.util.Objects;

/**
 * Caso de uso: cambiar el nombre visible desde Ajustes. Las reglas son las del registro
 * ({@link UserName}: sin espacios alrededor, de 2 a 100 caracteres). El correo no se cambia.
 */
public final class RenameUser {

    private final UserRepository repository;

    public RenameUser(UserRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** Renombra al usuario ya autenticado. Si el nombre no es válido, no se guarda nada. */
    public RenameResult rename(User user, String name) {
        return UserName.of(name).fold(RenameResult.Rejected::new, valid -> store(user.withName(valid)));
    }

    private RenameResult store(User renamed) {
        repository.update(renamed);
        return new RenameResult.Renamed(renamed);
    }
}
