package es.ulpgc.pamn.dominio.backend.domain.validation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorCodeTest {

    @Test
    void namesAreTheContractWithTheApp() {
        // La app traduce estos nombres a sus textos. Si este test falla por un renombrado,
        // hay que cambiar también la app (y docs/tareas/registro-cuentas-reales.md).
        Set<String> names = Arrays.stream(ErrorCode.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(Set.of(
                "REQUIRED", "TOO_SHORT", "TOO_LONG", "INVALID_EMAIL",
                "MISSING_LETTER", "MISSING_DIGIT", "MISSING_SPECIAL_CHARACTER",
                "TERMS_NOT_ACCEPTED", "EMAIL_TAKEN", "MALFORMED_BODY", "INVALID_CREDENTIALS",
                "UNAUTHENTICATED", "INVALID_PARAMETER", "COURSE_NOT_FOUND", "NOT_COURSE_AUTHOR",
                "IDENTICAL_TO_LATEST"), names);
    }
}
