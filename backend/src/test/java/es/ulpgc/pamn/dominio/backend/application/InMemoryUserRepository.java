package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Repositorio falso para tests: guarda los usuarios en un mapa indexado por correo. */
final class InMemoryUserRepository implements UserRepository {

    private final Map<Email, User> usersByEmail = new ConcurrentHashMap<>();
    private final Set<UUID> authors = ConcurrentHashMap.newKeySet();

    @Override
    public boolean add(User user) {
        // putIfAbsent es atómico, igual que la restricción UNIQUE de la base de datos.
        return usersByEmail.putIfAbsent(user.email(), user) == null;
    }

    @Override
    public void update(User user) {
        // Se indexa por correo, que no cambia nunca; si el usuario ya no está, no hace nada (como UPDATE).
        usersByEmail.computeIfPresent(user.email(), (email, old) -> old.id().equals(user.id()) ? user : old);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return usersByEmail.values().stream().filter(user -> user.id().equals(id)).findFirst();
    }

    @Override
    public synchronized boolean removeUnlessAuthor(UUID id) {
        return !authors.contains(id) && usersByEmail.values().removeIf(user -> user.id().equals(id));
    }

    /** Hace que el usuario cuente como autor de algún curso (en la app real lo dice la tabla courses). */
    void markAsAuthor(UUID id) {
        authors.add(id);
    }

    List<User> all() {
        return List.copyOf(usersByEmail.values());
    }
}
