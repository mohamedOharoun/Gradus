package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseIssueCodeTest {

    @Test
    void namesAreTheContractWithAuthors() {
        // Los autores y sus LLM leen estos códigos (especificación §3.3). Si este test falla por un
        // renombrado, hay que actualizar también backend/docs/especificacion-cursos-v1.md.
        Set<String> names = Arrays.stream(CourseIssueCode.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(Set.of(
                "INVALID_JSON", "DUPLICATE_KEY",
                "UNSUPPORTED_FORMAT_VERSION", "SCHEMA_VIOLATION",
                "DUPLICATE_ID", "DUPLICATE_OPTION_ID", "DUPLICATE_OPTION_TEXT", "UNKNOWN_OPTION",
                "ALL_OPTIONS_CORRECT", "DUPLICATE_MATCHING_SIDE", "PHOTO_IN_REVIEW", "UNKNOWN_PREREQUISITE",
                "SELF_PREREQUISITE", "DUPLICATE_TAG",
                "CYCLE", "REDUNDANT_PREREQUISITE",
                "MASTERY_MIN_EXCEEDS_QUESTIONS", "MASTERY_POOL_TOO_SMALL", "MASTERY_POOL_SMALL_FOR_RETRY",
                "MASTERY_REQUIRES_PERFECT", "MASTERY_GUESSABLE", "MODULE_WITHOUT_LESSON",
                "MODULE_WITHOUT_EXERCISE", "MATCHING_TOO_MANY_PAIRS",
                "PAYLOAD_TOO_LARGE", "REMOVED_IDS"), names);
    }

    @Test
    void warningsAreExactlyThoseOfTheCatalog() {
        Set<CourseIssueCode> warnings = Arrays.stream(CourseIssueCode.values())
                .filter(code -> code.severity() == Severity.WARNING)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                CourseIssueCode.DUPLICATE_TAG, CourseIssueCode.REDUNDANT_PREREQUISITE,
                CourseIssueCode.MASTERY_POOL_SMALL_FOR_RETRY, CourseIssueCode.MASTERY_REQUIRES_PERFECT,
                CourseIssueCode.MASTERY_GUESSABLE, CourseIssueCode.MODULE_WITHOUT_LESSON,
                CourseIssueCode.MODULE_WITHOUT_EXERCISE, CourseIssueCode.MATCHING_TOO_MANY_PAIRS,
                CourseIssueCode.REMOVED_IDS), warnings);
    }
}
