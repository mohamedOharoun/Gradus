package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;

/** Los desenlaces de eliminar la cuenta. La capa web traduce cada uno a un código HTTP. */
public sealed interface DeleteAccountResult {

    /** La cuenta y sus sesiones ya no existen. */
    record Deleted() implements DeleteAccountResult {
    }

    /** Falta la contraseña; no se ha borrado nada. */
    record Rejected(List<ValidationError> errors) implements DeleteAccountResult {
        public Rejected {
            errors = List.copyOf(errors);
        }
    }

    /** La contraseña no es la de la cuenta; no se ha borrado nada. */
    record WrongPassword() implements DeleteAccountResult {
    }

    /** La cuenta es autora de cursos publicados: no se borra para no dejar a nadie sin su curso (decisión I). */
    record AuthorOfCourses() implements DeleteAccountResult {
    }
}
