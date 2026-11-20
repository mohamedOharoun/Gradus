package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.DocumentOrderTest.pointer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseReportTest {

    static final Issue CYCLE = Issue.at(pointer("modules", 1), CourseIssueCode.CYCLE, "Ciclo.");
    static final Issue TAG = Issue.at(pointer("tags", 1), CourseIssueCode.DUPLICATE_TAG, "Etiqueta repetida.");
    static final Issue REMOVED = Issue.at(JsonPointer.root(), CourseIssueCode.REMOVED_IDS, "Ids eliminados.");

    @Test
    void aReportWithoutProblemsIsValid() {
        assertTrue(CourseReport.EMPTY.isValid());
        assertEquals(List.of(), CourseReport.EMPTY.issues());
    }

    @Test
    void itIsValidIfAndOnlyIfThereAreNoErrors() {
        assertTrue(new CourseReport(List.of(TAG)).isValid());
        assertFalse(new CourseReport(List.of(TAG, CYCLE)).isValid());
    }

    @Test
    void separatesErrorsFromWarnings() {
        CourseReport report = new CourseReport(List.of(CYCLE, TAG));

        assertEquals(List.of(CYCLE), report.errors());
        assertEquals(List.of(TAG), report.warnings());
    }

    @Test
    void keepsTheIssuesInDocumentOrder() {
        assertEquals(List.of(TAG, CYCLE), new CourseReport(List.of(CYCLE, TAG)).issues());
    }

    @Test
    void addingIssuesGivesANewOrderedReport() {
        CourseReport original = new CourseReport(List.of(TAG));

        CourseReport extended = original.with(List.of(REMOVED));

        assertEquals(List.of(REMOVED, TAG), extended.issues());
        assertEquals(List.of(TAG), original.issues());
    }
}
