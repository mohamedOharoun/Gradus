package es.ulpgc.pamn.dominio.backend.domain.progress;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MasteredModuleDraftTest {

    private static final Instant AT = Instant.parse("2026-11-24T10:20:00Z");

    @Test
    void acceptsABlankModuleIdBecauseValidatingIsAnotherStep() {
        assertDoesNotThrow(() -> new MasteredModuleDraft(" ", AT));
        assertDoesNotThrow(() -> new MasteredModuleDraft("", AT));
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(NullPointerException.class, () -> new MasteredModuleDraft(null, AT));
        assertThrows(NullPointerException.class, () -> new MasteredModuleDraft("bfs", null));
    }
}
