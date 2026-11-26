package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;
import java.util.Objects;

/** Los dos desenlaces de cambiar el nombre. La capa web traduce cada uno a un código HTTP. */
public sealed interface RenameResult {

    /** El nombre se ha cambiado y guardado; {@code user} es el usuario ya renombrado. */
    record Renamed(User user) implements RenameResult {
        public Renamed {
            Objects.requireNonNull(user, "user");
        }
    }

    /** El nombre no es válido; no se ha guardado nada. */
    record Rejected(List<ValidationError> errors) implements RenameResult {
        public Rejected {
            errors = List.copyOf(errors);
        }
    }
}
