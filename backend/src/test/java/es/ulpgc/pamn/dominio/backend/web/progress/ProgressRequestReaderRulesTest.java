package es.ulpgc.pamn.dominio.backend.web.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.EnrollmentDraft;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModuleDraft;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lo que el lector deja pasar a propósito: los problemas de valor no son de forma, los decide el dominio
 * (van a {@code skipped}). Y los campos que no conoce se ignoran (Tolerant Reader).
 */
class ProgressRequestReaderRulesTest {

    private static final Instant T = Instant.parse("2026-11-24T10:00:00Z");

    private final ProgressRequestReader reader = new ProgressRequestReader();

    private ParsedRequest read(String body) {
        return reader.read(body.getBytes(StandardCharsets.UTF_8));
    }

    private static String enrollment(String courseId, String moduleId) {
        return """
                {"enrollments":[{"courseId":"%s","enrolledAt":"2026-11-24T10:00:00Z",
                  "mastered":[{"moduleId":"%s","masteredAt":"2026-11-24T10:00:00Z"}]}]}"""
                .formatted(courseId, moduleId);
    }

    private static ParsedRequest parsedOf(String courseId, String moduleId) {
        return new ParsedRequest.Parsed(List.of(new EnrollmentDraft(courseId, T,
                List.of(new MasteredModuleDraft(moduleId, T)))));
    }

    @Test
    void aCourseIdThatIsNotAUuidPassesIntact() {
        assertEquals(parsedOf("no-es-un-uuid", "bfs"), read(enrollment("no-es-un-uuid", "bfs")));
    }

    @Test
    void aCourseIdIsNotNormalizedByTheReader() {
        assertEquals(parsedOf(" 0B8F6D2E ", "bfs"), read(enrollment(" 0B8F6D2E ", "bfs")));
    }

    @Test
    void anEmptyCourseIdPassesIntact() {
        assertEquals(parsedOf("", "bfs"), read(enrollment("", "bfs")));
    }

    @Test
    void anEmptyModuleIdPassesIntact() {
        assertEquals(parsedOf("c", ""), read(enrollment("c", "")));
    }

    @Test
    void aBlankModuleIdPassesIntact() {
        assertEquals(parsedOf("c", "   "), read(enrollment("c", "   ")));
    }

    @Test
    void unknownFieldsAreIgnoredAtEveryLevel() {
        ParsedRequest parsed = read("""
                {"extra":1,"userId":"otro-usuario","enrollments":[
                  {"courseId":"c","enrolledAt":"2026-11-24T10:00:00Z","note":{"a":[1,2]},
                   "mastered":[{"moduleId":"bfs","masteredAt":"2026-11-24T10:00:00Z","score":0.9}]}]}""");

        assertEquals(parsedOf("c", "bfs"), parsed);
    }

    @Test
    void aUserIdOfAnyTypeIsIgnored() {
        assertEquals(new ParsedRequest.Parsed(List.of()), read("{\"userId\":null,\"enrollments\":[]}"));
        assertEquals(new ParsedRequest.Parsed(List.of()), read("{\"userId\":42,\"enrollments\":[]}"));
        assertEquals(new ParsedRequest.Parsed(List.of()), read("{\"userId\":{\"x\":1},\"enrollments\":[]}"));
    }

    @Test
    void repeatedEntriesAreNotMergedByTheReader() {
        ParsedRequest parsed = read("""
                {"enrollments":[
                  {"courseId":"c","enrolledAt":"2026-11-24T10:00:00Z","mastered":[]},
                  {"courseId":"c","enrolledAt":"2026-11-25T10:00:00Z","mastered":[]}]}""");

        assertEquals(2, ((ParsedRequest.Parsed) parsed).enrollments().size());
    }
}
