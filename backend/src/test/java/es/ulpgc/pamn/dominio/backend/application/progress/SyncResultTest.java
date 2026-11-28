package es.ulpgc.pamn.dominio.backend.application.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.SkippedEntry;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.EARLY;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.GRAPHS;
import static es.ulpgc.pamn.dominio.backend.application.progress.ProgressTestData.enrollment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncResultTest {

    private static final SkippedEntry SKIPPED = SkippedEntry.enrollment("x", ErrorCode.COURSE_NOT_FOUND);

    @Test
    void rejectsNullLists() {
        assertThrows(NullPointerException.class, () -> new SyncResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new SyncResult(List.of(), null));
    }

    @Test
    void copiesItsListsSoLaterChangesToTheOriginalsDoNotLeakIn() {
        List<Enrollment> enrollments = new ArrayList<>(List.of(enrollment(GRAPHS, EARLY)));
        List<SkippedEntry> skipped = new ArrayList<>(List.of(SKIPPED));

        SyncResult result = new SyncResult(enrollments, skipped);
        enrollments.clear();
        skipped.clear();

        assertEquals(List.of(enrollment(GRAPHS, EARLY)), result.enrollments());
        assertEquals(List.of(SKIPPED), result.skipped());
    }

    @Test
    void itsListsCannotBeModifiedFromOutside() {
        SyncResult result = new SyncResult(List.of(enrollment(GRAPHS, EARLY)), List.of(SKIPPED));

        assertThrows(UnsupportedOperationException.class, () -> result.enrollments().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.skipped().clear());
    }
}
