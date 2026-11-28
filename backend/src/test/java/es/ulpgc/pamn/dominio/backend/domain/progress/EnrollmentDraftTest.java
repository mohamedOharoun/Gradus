package es.ulpgc.pamn.dominio.backend.domain.progress;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnrollmentDraftTest {

    private static final String COURSE = "0b8f6d2e-0000-4000-8000-000000000001";
    private static final Instant AT = Instant.parse("2026-11-24T10:00:00Z");

    @Test
    void acceptsACourseIdThatIsNotAUuidBecauseValidatingIsAnotherStep() {
        assertDoesNotThrow(() -> new EnrollmentDraft("not-a-uuid", AT, List.of()));
    }

    @Test
    void theListCannotBeModifiedFromOutside() {
        EnrollmentDraft draft = new EnrollmentDraft(COURSE, AT, List.of(new MasteredModuleDraft("bfs", AT)));

        assertThrows(UnsupportedOperationException.class,
                () -> draft.mastered().add(new MasteredModuleDraft("dfs", AT)));
    }

    @Test
    void changingTheOriginalListAfterwardsDoesNotAffectTheRecord() {
        List<MasteredModuleDraft> original = new ArrayList<>(List.of(new MasteredModuleDraft("bfs", AT)));
        EnrollmentDraft draft = new EnrollmentDraft(COURSE, AT, original);

        original.add(new MasteredModuleDraft("dfs", AT));

        assertEquals(1, draft.mastered().size());
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(NullPointerException.class, () -> new EnrollmentDraft(null, AT, List.of()));
        assertThrows(NullPointerException.class, () -> new EnrollmentDraft(COURSE, null, List.of()));
        assertThrows(NullPointerException.class, () -> new EnrollmentDraft(COURSE, AT, null));
    }
}
