package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;
import java.util.Objects;

/** Los tres desenlaces posibles de un registro. La capa web traduce cada uno a un código HTTP. */
public sealed interface RegistrationResult {

    /** El usuario se ha creado y guardado. */
    record Registered(User user) implements RegistrationResult {
        public Registered {
            Objects.requireNonNull(user, "user");
        }
    }

    /** Los datos no son válidos; contiene todos los errores encontrados. */
    record Rejected(List<ValidationError> errors) implements RegistrationResult {
        public Rejected {
            errors = List.copyOf(errors);
        }
    }

    /** Ya existe una cuenta con ese correo. */
    record EmailAlreadyRegistered(Email email) implements RegistrationResult {
        public EmailAlreadyRegistered {
            Objects.requireNonNull(email, "email");
        }
    }
}
