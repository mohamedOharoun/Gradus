package es.ulpgc.pamn.dominio.backend.web.progress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.application.progress.SyncResult;
import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;
import es.ulpgc.pamn.dominio.backend.domain.progress.SkippedEntry;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** El cuerpo de la respuesta 200 del contrato (api-progreso.md). */
class SyncResponseTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final UUID COURSE_1 = UUID.fromString("0b8f6d2e-0000-0000-0000-000000000001");
    private static final UUID COURSE_2 = UUID.fromString("0b8f6d2e-0000-0000-0000-000000000002");
    private static final Instant T = Instant.parse("2026-11-24T10:00:00Z");

    private static String json(SyncResult result) throws JsonProcessingException {
        return JSON.writeValueAsString(SyncResponse.from(result));
    }

    private static JsonNode tree(SyncResult result) throws JsonProcessingException {
        return JSON.readTree(json(result));
    }

    @Test
    void theFieldsHaveTheExactNamesOfTheContract() throws JsonProcessingException {
        SyncResult result = new SyncResult(
                List.of(new Enrollment(COURSE_1, T, List.of(new MasteredModule("bfs", T.plusSeconds(1200))))),
                List.of(SkippedEntry.enrollment("9a1c", ErrorCode.COURSE_NOT_FOUND),
                        SkippedEntry.module("0b8f6d2e-0000-0000-0000-000000000001", " ", ErrorCode.REQUIRED)));

        assertEquals("""
                {"enrollments":[{"courseId":"0b8f6d2e-0000-0000-0000-000000000001",\
                "enrolledAt":"2026-11-24T10:00:00Z",\
                "mastered":[{"moduleId":"bfs","masteredAt":"2026-11-24T10:20:00Z"}]}],\
                "skipped":[{"courseId":"9a1c","code":"COURSE_NOT_FOUND"},\
                {"courseId":"0b8f6d2e-0000-0000-0000-000000000001","moduleId":" ","code":"REQUIRED"}]}""",
                json(result));
    }

    @Test
    void datesAreIsoWithZAndKeepTheFraction() throws JsonProcessingException {
        Instant withFraction = Instant.parse("2026-11-24T10:00:00.123Z");
        SyncResult result = new SyncResult(
                List.of(new Enrollment(COURSE_1, withFraction, List.of(new MasteredModule("m", withFraction)))),
                List.of());

        JsonNode enrollment = tree(result).get("enrollments").get(0);

        assertEquals("2026-11-24T10:00:00.123Z", enrollment.get("enrolledAt").asText());
        assertEquals("2026-11-24T10:00:00.123Z", enrollment.get("mastered").get(0).get("masteredAt").asText());
    }

    @Test
    void datesWithoutFractionDoNotInventOne() throws JsonProcessingException {
        SyncResult result = new SyncResult(List.of(new Enrollment(COURSE_1, T, List.of())), List.of());

        assertEquals("2026-11-24T10:00:00Z", tree(result).get("enrollments").get(0).get("enrolledAt").asText());
    }

    @Test
    void anEnrollmentSkippedHasNoModuleIdField() throws JsonProcessingException {
        SyncResult result = new SyncResult(List.of(),
                List.of(SkippedEntry.enrollment("no-uuid", ErrorCode.COURSE_NOT_FOUND)));

        JsonNode skipped = tree(result).get("skipped").get(0);

        assertFalse(skipped.has("moduleId"), "moduleId debe estar ausente, no null");
        assertEquals("no-uuid", skipped.get("courseId").asText());
    }

    @Test
    void aModuleSkippedHasItsModuleId() throws JsonProcessingException {
        SyncResult result = new SyncResult(List.of(),
                List.of(SkippedEntry.module("c", "bfs", ErrorCode.REQUIRED)));

        assertEquals("bfs", tree(result).get("skipped").get(0).get("moduleId").asText());
    }

    @Test
    void aBlankModuleIdIsWrittenAsItArrived() throws JsonProcessingException {
        SyncResult result = new SyncResult(List.of(),
                List.of(SkippedEntry.module("c", "", ErrorCode.REQUIRED)));

        assertEquals("", tree(result).get("skipped").get(0).get("moduleId").asText());
    }

    @Test
    void theCodeIsTheNameOfTheErrorCode() throws JsonProcessingException {
        SyncResult result = new SyncResult(List.of(),
                List.of(SkippedEntry.enrollment("a", ErrorCode.COURSE_NOT_FOUND),
                        SkippedEntry.module("a", "m", ErrorCode.REQUIRED)));

        JsonNode skipped = tree(result).get("skipped");

        assertEquals("COURSE_NOT_FOUND", skipped.get(0).get("code").asText());
        assertEquals("REQUIRED", skipped.get(1).get("code").asText());
    }

    @Test
    void emptyListsAreEmptyArrays() throws JsonProcessingException {
        assertEquals("{\"enrollments\":[],\"skipped\":[]}", json(new SyncResult(List.of(), List.of())));
    }

    @Test
    void anEnrollmentWithoutModulesHasAnEmptyMasteredArray() throws JsonProcessingException {
        SyncResult result = new SyncResult(List.of(new Enrollment(COURSE_1, T, List.of())), List.of());

        assertEquals("[]", tree(result).get("enrollments").get(0).get("mastered").toString());
    }

    @Test
    void theOrderOfTheSyncResultIsKept() throws JsonProcessingException {
        // A propósito en un orden que ninguna ordenación natural produciría.
        SyncResult result = new SyncResult(
                List.of(new Enrollment(COURSE_2, T.plusSeconds(60), List.of(
                                new MasteredModule("z", T), new MasteredModule("a", T))),
                        new Enrollment(COURSE_1, T, List.of())),
                List.of(SkippedEntry.enrollment("zzz", ErrorCode.COURSE_NOT_FOUND),
                        SkippedEntry.enrollment("aaa", ErrorCode.COURSE_NOT_FOUND),
                        SkippedEntry.module("m", "k", ErrorCode.REQUIRED)));

        JsonNode tree = tree(result);

        assertEquals(COURSE_2.toString(), tree.get("enrollments").get(0).get("courseId").asText());
        assertEquals(COURSE_1.toString(), tree.get("enrollments").get(1).get("courseId").asText());
        assertEquals("z", tree.get("enrollments").get(0).get("mastered").get(0).get("moduleId").asText());
        assertEquals("a", tree.get("enrollments").get(0).get("mastered").get(1).get("moduleId").asText());
        assertEquals("zzz", tree.get("skipped").get(0).get("courseId").asText());
        assertEquals("aaa", tree.get("skipped").get(1).get("courseId").asText());
        assertEquals("m", tree.get("skipped").get(2).get("courseId").asText());
    }

    @Test
    void theCourseIdIsInCanonicalLowercaseForm() throws JsonProcessingException {
        UUID upper = UUID.fromString("0B8F6D2E-AAAA-BBBB-CCCC-DDDDEEEEFFFF");
        SyncResult result = new SyncResult(List.of(new Enrollment(upper, T, List.of())), List.of());

        assertEquals("0b8f6d2e-aaaa-bbbb-cccc-ddddeeeeffff",
                tree(result).get("enrollments").get(0).get("courseId").asText());
    }
}
