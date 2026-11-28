package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;
import java.util.Objects;

/**
 * Los desenlaces posibles de un inicio de sesión. La capa web traduce cada uno a un código HTTP.
 * Si se añade un segundo factor, bastará un caso nuevo (por ejemplo {@code SecondFactorRequired}).
 */
public sealed interface LogInResult {

    /** Sesión iniciada. El token solo existe aquí: se entrega a la app y no se guarda. */
    record LoggedIn(User user, SessionToken token, Session session) implements LogInResult {
        public LoggedIn {
            Objects.requireNonNull(user, "user");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(session, "session");
        }
    }

    /** Faltan datos o el correo no tiene formato válido. */
    record Rejected(List<ValidationError> errors) implements LogInResult {
        public Rejected {
            errors = List.copyOf(errors);
        }
    }

    /** Correo o contraseña incorrectos. No dice cuál de los dos, a propósito. */
    record InvalidCredentials() implements LogInResult {
    }
}
