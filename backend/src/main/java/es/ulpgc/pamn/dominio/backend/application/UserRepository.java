package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.User;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan los usuarios. La aplicación no sabe si es PostgreSQL o memoria. */
public interface UserRepository {

    /**
     * Guarda el usuario si su correo no está registrado, de forma atómica (como {@code Set.add}).
     *
     * @return {@code true} si se ha guardado; {@code false} si ya existía un usuario con ese correo.
     */
    boolean add(User user);

    /** Busca el usuario con ese correo (ya normalizado en {@link Email}). */
    Optional<User> findByEmail(Email email);

    Optional<User> findById(UUID id);

    /**
     * Guarda los cambios de un usuario que ya existe (se busca por su id). El correo no cambia nunca.
     * Si ya no existe, no hace nada.
     */
    void update(User user);

    /**
     * Borra el usuario (y, en cascada, sus sesiones) <b>salvo que sea autor de algún curso</b>: sus
     * cursos los estudian otros (decisión I de docs/tareas/ajustes.md). Comprobar y borrar es atómico.
     *
     * @return {@code true} si se ha borrado; {@code false} si es autor (o ya no existía).
     */
    boolean removeUnlessAuthor(UUID id);
}
