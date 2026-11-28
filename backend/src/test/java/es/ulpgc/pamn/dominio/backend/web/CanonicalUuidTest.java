package es.ulpgc.pamn.dominio.backend.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanonicalUuidTest {

    private static final UUID EXPECTED = UUID.fromString("0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b");

    @Test
    void parsesLowercaseCanonicalUuid() {
        assertEquals(Optional.of(EXPECTED), CanonicalUuid.parse("0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b"));
    }

    @Test
    void parsesUppercaseAndMixedCaseCanonicalUuid() {
        assertEquals(Optional.of(EXPECTED), CanonicalUuid.parse("0B8F1E2A-3C4D-4E5F-8A9B-0C1D2E3F4A5B"));
        assertEquals(Optional.of(EXPECTED), CanonicalUuid.parse("0b8f1e2a-3C4D-4e5f-8A9B-0c1d2e3f4a5b"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "not-a-uuid",
            "1-2-3-4-5",
            "0b8f1e2a3c4d4e5f8a9b0c1d2e3f4a5b",
            " 0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b",
            "0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b ",
            "0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b-extra",
            "0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5g"
    })
    void rejectsAnythingOutsideCanonicalFormat(String raw) {
        assertEquals(Optional.empty(), CanonicalUuid.parse(raw));
    }
}
