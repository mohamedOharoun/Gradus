package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Repositorio falso para tests: guarda los usuarios en un mapa indexado por correo. */
final class InMemoryUserRepository implements UserRepository {

    private final Map<Email, User> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public boolean add(User user) {
        // putIfAbsent es atómico, igual que la restricción UNIQUE de la base de datos.
        return usersByEmail.putIfAbsent(user.email(), user) == null;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return usersByEmail.values().stream().filter(user -> user.id().equals(id)).findFirst();
    }

    List<User> all() {
        return List.copyOf(usersByEmail.values());
    }
}
