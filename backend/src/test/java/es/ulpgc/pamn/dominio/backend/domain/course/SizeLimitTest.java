package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SizeLimitTest {

    @Test
    void whenTheSizeIsKnownTheMessageSaysIt() {
        Issue issue = SizeLimit.exceeded(3_000_000);

        assertEquals(CourseIssueCode.PAYLOAD_TOO_LARGE, issue.code());
        assertEquals("", issue.path().toString());
        assertEquals("El curso ocupa 3000000 bytes y el máximo es 2 MiB (2097152 bytes).", issue.message());
    }

    @Test
    void whenTheSizeIsUnknownTheMessageStillGivesTheLimit() {
        // Javalin puede rechazar el cuerpo antes de saber cuánto ocupa.
        Issue issue = SizeLimit.exceeded();

        assertEquals(CourseIssueCode.PAYLOAD_TOO_LARGE, issue.code());
        assertTrue(issue.message().contains("2 MiB (2097152 bytes)"), issue.message());
    }
}
