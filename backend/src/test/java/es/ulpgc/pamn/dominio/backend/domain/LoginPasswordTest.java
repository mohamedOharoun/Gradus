package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertFailure;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertSuccess;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.codesFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginPasswordTest {

    @ParameterizedTest
    @ValueSource(strings = {"Gradus2026!", "abc", " "})
    void acceptsAnyNonEmptyPasswordWithoutTheRegistrationRules(String attempt) {
        // Si las reglas de registro se endurecen, quien se registró antes debe poder seguir entrando.
        assertEquals(attempt, assertSuccess(LoginPassword.of(attempt)).value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isRequired(String missing) {
        assertEquals(List.of(ErrorCode.REQUIRED), codesFor("password", assertFailure(LoginPassword.of(missing))));
    }

    @Test
    void isNeverNormalized() {
        assertEquals(" Gradus2026! ", assertSuccess(LoginPassword.of(" Gradus2026! ")).value());
    }

    @Test
    void toStringNeverRevealsThePassword() {
        assertFalse(new LoginPassword("Gradus2026!").toString().contains("Gradus2026!"));
    }

    @Test
    void theConstructorRejectsAMissingPassword() {
        assertThrows(IllegalArgumentException.class, () -> new LoginPassword(""));
    }
}
