package es.ulpgc.pamn.dominio.backend.web.progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Todo lo que no tiene la forma del contrato es {@code Malformed} (400); ningún caso acaba en excepción. */
class ProgressRequestReaderMalformedTest {

    private static final ParsedRequest MALFORMED = new ParsedRequest.Malformed();

    private static final String OK_ENROLLMENT =
            "\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[]";

    private final ProgressRequestReader reader = new ProgressRequestReader();

    private static String enrollments(String list) {
        return "{\"enrollments\":" + list + "}";
    }

    private static String enrollment(String fields) {
        return enrollments("[{" + fields + "}]");
    }

    private static String module(String fields) {
        return enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[{" + fields + "}]");
    }

    private static String withEnrolledAt(String json) {
        return enrollment("\"courseId\":\"c\",\"enrolledAt\":" + json + ",\"mastered\":[]");
    }

    static Stream<Arguments> malformedBodies() {
        return Stream.of(
                // Raíz
                Arguments.of("empty body", ""),
                Arguments.of("only spaces", "   \n "),
                Arguments.of("root null", "null"),
                Arguments.of("root array", "[]"),
                Arguments.of("root string", "\"enrollments\""),
                Arguments.of("root number", "42"),
                Arguments.of("broken JSON", "{\"enrollments\":["),
                Arguments.of("not JSON", "enrollments"),
                Arguments.of("text after the JSON", "{\"enrollments\":[]} basura"),
                Arguments.of("second JSON after the first", "{\"enrollments\":[]} {}"),
                // Clave repetida
                Arguments.of("repeated key: enrollments", "{\"enrollments\":[],\"enrollments\":[]}"),
                Arguments.of("repeated key in an ignored field", "{\"userId\":\"a\",\"userId\":\"b\",\"enrollments\":[]}"),
                Arguments.of("repeated key inside an enrollment",
                        enrollment(OK_ENROLLMENT + ",\"courseId\":\"d\"")),
                // enrollments
                Arguments.of("enrollments missing", "{}"),
                Arguments.of("enrollments null", "{\"enrollments\":null}"),
                Arguments.of("enrollments is an object", enrollments("{}")),
                Arguments.of("enrollments is a string", enrollments("\"x\"")),
                Arguments.of("enrollments is a number", enrollments("1")),
                Arguments.of("enrollments has a null element", enrollments("[null]")),
                Arguments.of("enrollments has a null after a valid one",
                        enrollments("[{" + OK_ENROLLMENT + "},null]")),
                Arguments.of("enrollments has a number element", enrollments("[1]")),
                Arguments.of("enrollments has a string element", enrollments("[\"c\"]")),
                Arguments.of("enrollments has an array element", enrollments("[[]]")),
                // inscripción
                Arguments.of("courseId missing",
                        enrollment("\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[]")),
                Arguments.of("enrolledAt missing", enrollment("\"courseId\":\"c\",\"mastered\":[]")),
                Arguments.of("mastered missing",
                        enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\"")),
                Arguments.of("courseId null",
                        enrollment("\"courseId\":null,\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[]")),
                Arguments.of("enrolledAt null", withEnrolledAt("null")),
                Arguments.of("mastered null",
                        enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":null")),
                Arguments.of("mastered is an object",
                        enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":{}")),
                Arguments.of("mastered is a string",
                        enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":\"x\"")),
                Arguments.of("mastered has a null element",
                        enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[null]")),
                Arguments.of("mastered has a number element",
                        enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[1]")),
                // módulo
                Arguments.of("moduleId missing", module("\"masteredAt\":\"2026-11-24T10:00:00Z\"")),
                Arguments.of("masteredAt missing", module("\"moduleId\":\"bfs\"")),
                Arguments.of("moduleId null", module("\"moduleId\":null,\"masteredAt\":\"2026-11-24T10:00:00Z\"")),
                Arguments.of("masteredAt null", module("\"moduleId\":\"bfs\",\"masteredAt\":null")),
                Arguments.of("masteredAt invalid in the second module",
                        enrollment("\"courseId\":\"c\",\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":["
                                + "{\"moduleId\":\"a\",\"masteredAt\":\"2026-11-24T10:00:00Z\"},"
                                + "{\"moduleId\":\"b\",\"masteredAt\":\"ayer\"}]")),
                // Tipos equivocados
                Arguments.of("number as courseId",
                        enrollment("\"courseId\":7,\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[]")),
                Arguments.of("boolean as courseId",
                        enrollment("\"courseId\":true,\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[]")),
                Arguments.of("object as courseId",
                        enrollment("\"courseId\":{},\"enrolledAt\":\"2026-11-24T10:00:00Z\",\"mastered\":[]")),
                Arguments.of("number as moduleId", module("\"moduleId\":5,\"masteredAt\":\"2026-11-24T10:00:00Z\"")),
                Arguments.of("number as enrolledAt", withEnrolledAt("1732442400")),
                Arguments.of("number as masteredAt", module("\"moduleId\":\"bfs\",\"masteredAt\":1732442400")),
                Arguments.of("object as enrolledAt", withEnrolledAt("{}")),
                // Fechas
                Arguments.of("date dd/MM/yyyy", withEnrolledAt("\"24/11/2026\"")),
                Arguments.of("date without time", withEnrolledAt("\"2026-11-24\"")),
                Arguments.of("empty date", withEnrolledAt("\"\"")),
                Arguments.of("date without offset", withEnrolledAt("\"2026-11-24T10:00:00\"")),
                Arguments.of("date with spaces around", withEnrolledAt("\" 2026-11-24T10:00:00Z \"")),
                Arguments.of("masteredAt with spaces around",
                        module("\"moduleId\":\"bfs\",\"masteredAt\":\" 2026-11-24T10:00:00Z\"")),
                Arguments.of("date with an impossible day", withEnrolledAt("\"2026-02-30T10:00:00Z\"")),
                // Año fuera de 0001–9999 (en UTC)
                Arguments.of("year 10000", withEnrolledAt("\"+10000-01-01T00:00:00Z\"")),
                Arguments.of("year -1", withEnrolledAt("\"-0001-01-01T00:00:00Z\"")),
                Arguments.of("year -1000000000", withEnrolledAt("\"-1000000000-01-01T00:00:00Z\"")),
                Arguments.of("year 999999999", withEnrolledAt("\"+999999999-01-01T00:00:00Z\"")),
                Arguments.of("year 10 digits out of Instant range", withEnrolledAt("\"+9999999999-01-01T00:00:00Z\"")),
                Arguments.of("year 0", withEnrolledAt("\"0000-06-01T00:00:00Z\"")),
                Arguments.of("year 0 in UTC after applying the offset", withEnrolledAt("\"0001-01-01T00:30:00+01:00\"")),
                Arguments.of("year 10000 in UTC after applying the offset",
                        withEnrolledAt("\"9999-12-31T23:30:00-01:00\"")),
                Arguments.of("masteredAt with year 10000",
                        module("\"moduleId\":\"bfs\",\"masteredAt\":\"+10000-01-01T00:00:00Z\""))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedBodies")
    void aBodyWithoutTheExpectedShapeIsMalformed(String name, String body) {
        assertEquals(MALFORMED, reader.read(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void bytesThatAreNotValidUtf8AreMalformed() {
        byte[] invalid = {'{', '"', 'e', 'n', 'r', 'o', 'l', 'l', 'm', 'e', 'n', 't', 's', '"', ':', '[', ']',
                ',', '"', 'x', '"', ':', '"', (byte) 0xC3, (byte) 0x28, '"', '}'};

        assertEquals(MALFORMED, reader.read(invalid));
    }

    @Test
    void anEmptyArrayOfBytesIsMalformed() {
        assertEquals(MALFORMED, reader.read(new byte[0]));
    }

    @Test
    void aNullArrayOfBytesIsMalformedAndNotANullPointerException() {
        assertEquals(MALFORMED, reader.read(null));
    }

    @Test
    void aUtf8ByteOrderMarkAtTheStartIsMalformed() {
        // Decisión fijada: al contrario que el lector de cursos (un fichero que sube un autor), el cuerpo lo
        // genera la app, que nunca pone BOM. Se rechaza en vez de aceptarlo en silencio.
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] json = "{\"enrollments\":[]}".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + json.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(json, 0, withBom, bom.length, json.length);

        assertEquals(MALFORMED, reader.read(withBom));
    }

    @Test
    void aVeryDeeplyNestedBodyIsMalformedAndNotAnError() {
        String deep = "[".repeat(5000) + "]".repeat(5000);

        assertInstanceOf(ParsedRequest.Malformed.class,
                reader.read(("{\"enrollments\":" + deep + "}").getBytes(StandardCharsets.UTF_8)));
    }
}
