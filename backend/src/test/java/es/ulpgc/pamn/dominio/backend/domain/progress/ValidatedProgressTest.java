package es.ulpgc.pamn.dominio.backend.domain.progress;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatedProgressTest {

    private static final Instant AT = Instant.parse("2026-11-24T10:00:00Z");

    private static Enrollment enrollment(String id) {
        return new Enrollment(UUID.fromString(id), AT, List.of());
    }

    private static final String COURSE_A = "0b8f6d2e-0000-4000-8000-00000000000a";
    private static final String COURSE_B = "0b8f6d2e-0000-4000-8000-00000000000b";

    @Test
    void theListsCannotBeModifiedFromOutside() {
        ValidatedProgress progress = new ValidatedProgress(
                List.of(enrollment(COURSE_A)), List.of(SkippedEntry.enrollment("x", ErrorCode.COURSE_NOT_FOUND)));

        assertThrows(UnsupportedOperationException.class, () -> progress.valid().add(enrollment(COURSE_B)));
        assertThrows(UnsupportedOperationException.class,
                () -> progress.skipped().add(SkippedEntry.enrollment("y", ErrorCode.COURSE_NOT_FOUND)));
    }

    @Test
    void changingTheOriginalListsAfterwardsDoesNotAffectTheRecord() {
        List<Enrollment> valid = new ArrayList<>(List.of(enrollment(COURSE_A)));
        List<SkippedEntry> skipped = new ArrayList<>(List.of(SkippedEntry.enrollment("x", ErrorCode.COURSE_NOT_FOUND)));
        ValidatedProgress progress = new ValidatedProgress(valid, skipped);

        valid.add(enrollment(COURSE_B));
        skipped.add(SkippedEntry.enrollment("y", ErrorCode.COURSE_NOT_FOUND));

        assertEquals(1, progress.valid().size());
        assertEquals(1, progress.skipped().size());
    }

    @Test
    void rejectsMissingLists() {
        assertThrows(NullPointerException.class, () -> new ValidatedProgress(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ValidatedProgress(List.of(), null));
    }
}
