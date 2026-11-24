package es.ulpgc.pamn.dominio.backend.domain.progress;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressValidationTest {

    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final Instant PAST = Instant.parse("2026-11-24T10:00:00Z");
    private static final Instant FUTURE = Instant.parse("2026-11-25T00:00:00Z");
    private static final String COURSE = "0b8f6d2e-0000-4000-8000-000000000001";
    private static final String OTHER_COURSE = "0b8f6d2e-0000-4000-8000-000000000002";

    private static EnrollmentDraft draft(String courseId, Instant enrolledAt, MasteredModuleDraft... modules) {
        return new EnrollmentDraft(courseId, enrolledAt, List.of(modules));
    }

    private static MasteredModuleDraft module(String id, Instant at) {
        return new MasteredModuleDraft(id, at);
    }

    @Test
    void emptyInputGivesAnEmptyResult() {
        ValidatedProgress result = ProgressValidation.validate(List.of(), NOW);

        assertTrue(result.valid().isEmpty());
        assertTrue(result.skipped().isEmpty());
    }

    @Test
    void aFullyValidEntryPassesUnchanged() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, PAST, module("bfs", PAST))), NOW);

        assertEquals(List.of(new Enrollment(UUID.fromString(COURSE), PAST,
                List.of(new MasteredModule("bfs", PAST)))), result.valid());
        assertTrue(result.skipped().isEmpty());
    }

    @Test
    void anInvalidUuidSkipsTheWholeEnrollmentEvenWithModules() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft("not-a-uuid", PAST, module("bfs", PAST), module(" ", PAST))), NOW);

        assertTrue(result.valid().isEmpty());
        assertEquals(List.of(SkippedEntry.enrollment("not-a-uuid", ErrorCode.COURSE_NOT_FOUND)), result.skipped());
    }

    @Test
    void onlyTheCanonicalUuidFormatIsAccepted() {
        // UUID.fromString("1-1-1-1-1") no falla, pero eso no es un UUID de verdad.
        ValidatedProgress result = ProgressValidation.validate(List.of(
                draft("1-1-1-1-1", PAST), draft("", PAST), draft(COURSE.toUpperCase(), PAST)), NOW);

        assertEquals(List.of(
                SkippedEntry.enrollment("1-1-1-1-1", ErrorCode.COURSE_NOT_FOUND),
                SkippedEntry.enrollment("", ErrorCode.COURSE_NOT_FOUND)), result.skipped());
        assertEquals(UUID.fromString(COURSE), result.valid().get(0).courseId());
    }

    @Test
    void anEmptyModuleIdSkipsOnlyThatModuleAndKeepsTheRest() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, PAST, module("bfs", PAST), module("", PAST), module("dfs", PAST))), NOW);

        assertEquals(List.of(new MasteredModule("bfs", PAST), new MasteredModule("dfs", PAST)),
                result.valid().get(0).mastered());
        assertEquals(List.of(SkippedEntry.module(COURSE, "", ErrorCode.REQUIRED)), result.skipped());
    }

    @Test
    void aBlankModuleIdIsSkippedAndReportedAsItArrived() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, PAST, module(" \t ", PAST))), NOW);

        assertEquals(List.of(SkippedEntry.module(COURSE, " \t ", ErrorCode.REQUIRED)), result.skipped());
    }

    @Test
    void anEnrollmentWhoseModulesAreAllInvalidIsStillKept() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, PAST, module(" ", PAST))), NOW);

        assertEquals(1, result.valid().size());
        assertTrue(result.valid().get(0).mastered().isEmpty());
    }

    @Test
    void aValidModuleIdIsNotTrimmed() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, PAST, module(" bfs ", PAST))), NOW);

        assertEquals(" bfs ", result.valid().get(0).mastered().get(0).moduleId());
    }

    @Test
    void aFutureEnrolledAtIsClippedToNow() {
        ValidatedProgress result = ProgressValidation.validate(List.of(draft(COURSE, FUTURE)), NOW);

        assertEquals(NOW, result.valid().get(0).enrolledAt());
    }

    @Test
    void aFutureMasteredAtIsClippedToNow() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, PAST, module("bfs", FUTURE))), NOW);

        assertEquals(NOW, result.valid().get(0).mastered().get(0).masteredAt());
    }

    @Test
    void aDateEqualToNowIsNotChanged() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, NOW, module("bfs", NOW))), NOW);

        assertEquals(NOW, result.valid().get(0).enrolledAt());
        assertEquals(NOW, result.valid().get(0).mastered().get(0).masteredAt());
    }

    @Test
    void aPastDateIsNotChanged() {
        ValidatedProgress result = ProgressValidation.validate(
                List.of(draft(COURSE, PAST, module("bfs", PAST))), NOW);

        assertEquals(PAST, result.valid().get(0).enrolledAt());
        assertEquals(PAST, result.valid().get(0).mastered().get(0).masteredAt());
    }

    @Test
    void skippedEntriesComeInRequestOrder() {
        ValidatedProgress result = ProgressValidation.validate(List.of(
                draft("zzz", PAST),
                draft(COURSE, PAST, module(" ", PAST), module("bfs", PAST), module("", PAST)),
                draft("aaa", PAST),
                draft(OTHER_COURSE, PAST, module("", PAST))), NOW);

        assertEquals(List.of(
                SkippedEntry.enrollment("zzz", ErrorCode.COURSE_NOT_FOUND),
                SkippedEntry.module(COURSE, " ", ErrorCode.REQUIRED),
                SkippedEntry.module(COURSE, "", ErrorCode.REQUIRED),
                SkippedEntry.enrollment("aaa", ErrorCode.COURSE_NOT_FOUND),
                SkippedEntry.module(OTHER_COURSE, "", ErrorCode.REQUIRED)), result.skipped());
    }

    @Test
    void validEnrollmentsKeepRequestOrderAndDuplicatesAreNotRemovedHere() {
        ValidatedProgress result = ProgressValidation.validate(List.of(
                draft(OTHER_COURSE, PAST), draft(COURSE, PAST), draft(OTHER_COURSE, PAST)), NOW);

        assertEquals(List.of(OTHER_COURSE, COURSE, OTHER_COURSE),
                result.valid().stream().map(e -> e.courseId().toString()).toList());
    }

    @Test
    void doesNotModifyTheInput() {
        List<EnrollmentDraft> input = new ArrayList<>(List.of(draft(COURSE, FUTURE, module("bfs", FUTURE))));
        List<EnrollmentDraft> copy = List.copyOf(input);

        ProgressValidation.validate(input, NOW);

        assertEquals(copy, input);
    }
}
