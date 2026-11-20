package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueTest {

    static final JsonPointer PATH = JsonPointer.root().key("modules").index(0);
    static final String MESSAGE = "Los prerrequisitos forman un ciclo: a requiere b, que requiere a.";

    @Test
    void severityComesFromTheCode() {
        Issue error = Issue.at(PATH, CourseIssueCode.CYCLE, MESSAGE);
        Issue warning = Issue.at(PATH, CourseIssueCode.REDUNDANT_PREREQUISITE, MESSAGE);

        assertEquals(Severity.ERROR, error.severity());
        assertTrue(error.isError());
        assertEquals(Severity.WARNING, warning.severity());
        assertFalse(warning.isError());
    }

    @Test
    void keepsPathCodeAndMessage() {
        Issue issue = Issue.at(PATH, CourseIssueCode.CYCLE, MESSAGE);

        assertEquals(PATH, issue.path());
        assertEquals(CourseIssueCode.CYCLE, issue.code());
        assertEquals(MESSAGE, issue.message());
    }

    @Test
    void requiresEveryField() {
        assertThrows(NullPointerException.class, () -> Issue.at(null, CourseIssueCode.CYCLE, MESSAGE));
        assertThrows(NullPointerException.class, () -> Issue.at(PATH, null, MESSAGE));
        assertThrows(NullPointerException.class, () -> Issue.at(PATH, CourseIssueCode.CYCLE, null));
    }

    @Test
    void rejectsABlankMessage() {
        // Un problema sin explicación no le sirve ni al autor ni a su LLM.
        assertThrows(IllegalArgumentException.class, () -> Issue.at(PATH, CourseIssueCode.CYCLE, "  "));
    }
}
