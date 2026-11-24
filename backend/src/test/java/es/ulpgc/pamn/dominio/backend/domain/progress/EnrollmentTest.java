package es.ulpgc.pamn.dominio.backend.domain.progress;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnrollmentTest {

    private static final UUID COURSE = UUID.fromString("0b8f6d2e-0000-4000-8000-000000000001");
    private static final Instant AT = Instant.parse("2026-11-24T10:00:00Z");

    @Test
    void theListCannotBeModifiedFromOutside() {
        Enrollment enrollment = new Enrollment(COURSE, AT, List.of(new MasteredModule("bfs", AT)));

        assertThrows(UnsupportedOperationException.class,
                () -> enrollment.mastered().add(new MasteredModule("dfs", AT)));
    }

    @Test
    void changingTheOriginalListAfterwardsDoesNotAffectTheRecord() {
        List<MasteredModule> original = new ArrayList<>(List.of(new MasteredModule("bfs", AT)));
        Enrollment enrollment = new Enrollment(COURSE, AT, original);

        original.add(new MasteredModule("dfs", AT));

        assertEquals(1, enrollment.mastered().size());
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(NullPointerException.class, () -> new Enrollment(null, AT, List.of()));
        assertThrows(NullPointerException.class, () -> new Enrollment(COURSE, null, List.of()));
        assertThrows(NullPointerException.class, () -> new Enrollment(COURSE, AT, null));
    }
}
