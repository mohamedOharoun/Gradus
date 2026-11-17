package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.Validation;

import java.util.List;

/**
 * Contraseña que escribe el usuario al iniciar sesión. A diferencia de {@link RawPassword}, no
 * aplica las reglas de fortaleza (solo «no vacía»): una cuenta creada con reglas anteriores debe
 * poder seguir entrando. Tampoco se normaliza.
 */
public record LoginPassword(String value) {

    private static final String FIELD = "password";

    public LoginPassword {
        Validation.requireValid(FIELD, value, List.of());
    }

    public static Result<LoginPassword> of(String raw) {
        return Result.validate(Validation.check(FIELD, raw, List.of()), () -> new LoginPassword(raw));
    }

    /** Sustituye al toString generado por el record, que mostraría la contraseña en los logs. */
    @Override
    public String toString() {
        return "LoginPassword[****]";
    }
}
