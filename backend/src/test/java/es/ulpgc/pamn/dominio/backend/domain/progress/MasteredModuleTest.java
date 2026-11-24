package es.ulpgc.pamn.dominio.backend.domain.progress;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MasteredModuleTest {

    private static final Instant AT = Instant.parse("2026-11-24T10:20:00Z");

    @Test
    void acceptsAValidModule() {
        assertDoesNotThrow(() -> new MasteredModule("bfs", AT));
    }

    @Test
    void rejectsEmptyOrBlankModuleId() {
        assertThrows(IllegalArgumentException.class, () -> new MasteredModule("", AT));
        assertThrows(IllegalArgumentException.class, () -> new MasteredModule("   ", AT));
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(NullPointerException.class, () -> new MasteredModule(null, AT));
        assertThrows(NullPointerException.class, () -> new MasteredModule("bfs", null));
    }
}
