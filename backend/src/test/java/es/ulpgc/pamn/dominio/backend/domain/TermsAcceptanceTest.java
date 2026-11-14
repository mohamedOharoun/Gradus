package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertFailure;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertSuccess;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.codesFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TermsAcceptanceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    @Test
    void recordsWhenTheTermsWereAccepted() {
        assertEquals(NOW, assertSuccess(TermsAcceptance.of(true, NOW)).acceptedAt());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void rejectsTermsThatWereNotAccepted(Boolean accepted) {
        // null es el campo ausente en el JSON: cuenta igual que no haberlos aceptado.
        assertEquals(List.of(ErrorCode.TERMS_NOT_ACCEPTED),
                codesFor("acceptedTerms", assertFailure(TermsAcceptance.of(accepted, NOW))));
    }

    @Test
    void cannotExistWithoutTheMoment() {
        assertThrows(NullPointerException.class, () -> new TermsAcceptance(null));
    }
}
