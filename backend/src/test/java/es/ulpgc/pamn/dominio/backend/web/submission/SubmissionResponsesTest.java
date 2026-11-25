package es.ulpgc.pamn.dominio.backend.web.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El JSON de una entrega y de la lista de entregas (docs/tareas/ejercicio-foto.md, decisión Q). */
class SubmissionResponsesTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID GRAFOS = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    static Submission submission(int n, int photoSize) {
        byte[] bytes = new byte[photoSize];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        Jpeg photo = ((JpegCheck.Valid) Jpeg.check(bytes)).jpeg();
        return Submission.pending(UUID.fromString("00000000-0000-0000-0000-0000000000a" + n), ANA, GRAFOS, 3,
                "repr-foto", Instant.parse("2026-11-24T09:00:00.123456Z"), photo);
    }

    @Test
    void aSubmissionIsSentWithEveryFieldTheAppNeeds() throws Exception {
        JsonNode json = JSON.readTree(JSON.writeValueAsString(SubmissionResponse.from(submission(1, 300))));

        assertEquals("00000000-0000-0000-0000-0000000000a1", json.get("id").asText());
        assertEquals(GRAFOS.toString(), json.get("courseId").asText());
        assertEquals(3, json.get("courseVersion").asInt());
        assertEquals("repr-foto", json.get("exerciseId").asText());
        assertEquals("PENDING", json.get("status").asText());
        assertEquals(300, json.get("photoBytes").asInt());
    }

    // ISO-8601, como publishedAt en los cursos, y con los microsegundos que guarda PostgreSQL.
    @Test
    void theDateIsSentInIso8601() throws Exception {
        JsonNode json = JSON.readTree(JSON.writeValueAsString(SubmissionResponse.from(submission(1, 300))));

        assertEquals("2026-11-24T09:00:00.123456Z", json.get("submittedAt").asText());
    }

    // El alumno es siempre el de la sesión: no hace falta enviarlo. Y la foto se descarga aparte.
    @Test
    void neitherTheStudentNorThePhotoAreSent() throws Exception {
        JsonNode json = JSON.readTree(JSON.writeValueAsString(SubmissionResponse.from(submission(1, 300))));

        assertEquals(7, json.size());
        assertTrue(json.path("studentId").isMissingNode());
        assertTrue(json.path("photo").isMissingNode());
    }

    @Test
    void theListKeepsTheOrderItIsGiven() throws Exception {
        JsonNode json = JSON.readTree(JSON.writeValueAsString(
                SubmissionListResponse.from(List.of(submission(2, 400), submission(1, 300)))));

        assertEquals(2, json.get("submissions").size());
        assertEquals("00000000-0000-0000-0000-0000000000a2", json.get("submissions").get(0).get("id").asText());
        assertEquals("00000000-0000-0000-0000-0000000000a1", json.get("submissions").get(1).get("id").asText());
    }

    // Sin entregas: lista vacía, no null ni un 404 (la app pinta «aún no has entregado»).
    @Test
    void withoutSubmissionsTheListIsEmpty() throws Exception {
        assertEquals("{\"submissions\":[]}", JSON.writeValueAsString(SubmissionListResponse.from(List.of())));
    }
}
