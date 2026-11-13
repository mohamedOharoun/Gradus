package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.User;

/** Puerto de salida: dónde se guardan los usuarios. La aplicación no sabe si es PostgreSQL o memoria. */
public interface UserRepository {

    /**
     * Guarda el usuario si su correo no está registrado, de forma atómica (como {@code Set.add}).
     *
     * @return {@code true} si se ha guardado; {@code false} si ya existía un usuario con ese correo.
     */
    boolean add(User user);
}
