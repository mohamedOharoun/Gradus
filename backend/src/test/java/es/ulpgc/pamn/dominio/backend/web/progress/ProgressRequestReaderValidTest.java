package es.ulpgc.pamn.dominio.backend.web.progress;

import es.ulpgc.pamn.dominio.backend.domain.progress.EnrollmentDraft;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModuleDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Cuerpos con la forma correcta: el lector entrega los borradores, en el mismo orden y con las fechas parseadas. */
class ProgressRequestReaderValidTest {

    private final ProgressRequestReader reader = new ProgressRequestReader();

    private ParsedRequest read(String body) {
        return reader.read(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void aValidBodyKeepsTheOrderOfEnrollmentsAndModules() {
        ParsedRequest parsed = read("""
                {"enrollments":[
                  {"courseId":"0b8f6d2e-0000-0000-0000-000000000002","enrolledAt":"2026-11-24T10:00:00Z",
                   "mastered":[{"moduleId":"colas","masteredAt":"2026-11-24T10:30:00Z"},
                               {"moduleId":"bfs","masteredAt":"2026-11-24T10:20:00Z"}]},
                  {"courseId":"0b8f6d2e-0000-0000-0000-000000000001","enrolledAt":"2026-11-23T09:00:00Z",
                   "mastered":[]}]}""");

        assertEquals(new ParsedRequest.Parsed(List.of(
                new EnrollmentDraft("0b8f6d2e-0000-0000-0000-000000000002",
                        Instant.parse("2026-11-24T10:00:00Z"),
                        List.of(new MasteredModuleDraft("colas", Instant.parse("2026-11-24T10:30:00Z")),
                                new MasteredModuleDraft("bfs", Instant.parse("2026-11-24T10:20:00Z")))),
                new EnrollmentDraft("0b8f6d2e-0000-0000-0000-000000000001",
                        Instant.parse("2026-11-23T09:00:00Z"), List.of()))), parsed);
    }

    @Test
    void anEmptyListOfEnrollmentsIsValid() {
        assertEquals(new ParsedRequest.Parsed(List.of()), read("{\"enrollments\":[]}"));
    }

    @Test
    void anEnrollmentWithoutModulesIsValid() {
        assertEquals(new ParsedRequest.Parsed(List.of(
                        new EnrollmentDraft("c", Instant.parse("2026-11-24T10:00:00Z"), List.of()))),
                read("{\"enrollments\":[{\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[]}]}"));
    }

    @Test
    void theReaderCanBeReusedBetweenRequests() {
        String body = "{\"enrollments\":[]}";

        assertEquals(read(body), read(body));
    }

    @Test
    void textWithNonAsciiCharactersSurvivesTheDecoding() {
        ParsedRequest parsed = read("""
                {"enrollments":[{"courseId":"c","enrolledAt":"2026-11-24T10:00:00Z",
                  "mastered":[{"moduleId":"árboles-ñandú","masteredAt":"2026-11-24T10:00:00Z"}]}]}""");

        assertEquals("árboles-ñandú",
                ((ParsedRequest.Parsed) parsed).enrollments().get(0).mastered().get(0).moduleId());
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "2026-11-24T10:00:00Z,          2026-11-24T10:00:00Z",
            "2026-11-24T10:00:00.123Z,      2026-11-24T10:00:00.123Z",
            "2026-11-24T10:00:00+01:00,     2026-11-24T09:00:00Z",
            "2026-11-24T10:00:00-05:00,     2026-11-24T15:00:00Z",
            "0001-01-01T00:00:00Z,          0001-01-01T00:00:00Z",
            "9999-12-31T23:59:59Z,          9999-12-31T23:59:59Z",
    })
    void datesAreIso8601AndAnOffsetIsConvertedToUtc(String text, String expected) {
        ParsedRequest parsed = read("""
                {"enrollments":[{"courseId":"c","enrolledAt":"%s","mastered":[]}]}""".formatted(text));

        assertEquals(Instant.parse(expected),
                ((ParsedRequest.Parsed) parsed).enrollments().get(0).enrolledAt());
    }
}
