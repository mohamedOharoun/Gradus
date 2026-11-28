package es.ulpgc.pamn.dominio.backend.domain.progress;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkippedEntryTest {

    @Test
    void anEnrollmentEntryHasNoModuleId() {
        SkippedEntry entry = SkippedEntry.enrollment("abc", ErrorCode.COURSE_NOT_FOUND);

        assertEquals("abc", entry.courseId());
        assertEquals(Optional.empty(), entry.moduleId());
        assertEquals(ErrorCode.COURSE_NOT_FOUND, entry.code());
    }

    @Test
    void aModuleEntryKeepsTheModuleIdAsItArrived() {
        SkippedEntry entry = SkippedEntry.module("abc", " ", ErrorCode.REQUIRED);

        assertEquals(Optional.of(" "), entry.moduleId());
        assertEquals(ErrorCode.REQUIRED, entry.code());
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(NullPointerException.class, () -> SkippedEntry.enrollment(null, ErrorCode.REQUIRED));
        assertThrows(NullPointerException.class, () -> SkippedEntry.enrollment("abc", null));
        assertThrows(NullPointerException.class, () -> SkippedEntry.module("abc", null, ErrorCode.REQUIRED));
    }
}
