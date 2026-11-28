package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseContentTest {

    @Test
    void theHashIsTheLowercaseHexSha256OfTheUtf8Bytes() {
        // Valor de referencia de SHA-256("abc") (FIPS 180-2).
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                new CourseContent("abc").sha256());
    }

    @Test
    void theSizeCountsUtf8BytesNotCharacters() {
        assertEquals(3, new CourseContent("abc").sizeBytes());
        assertEquals(2, new CourseContent("é").sizeBytes());
        assertEquals(4, new CourseContent("📚").sizeBytes());
    }

    @Test
    void contentIsBetween1ByteAnd2MiB() {
        assertDoesNotThrow(() -> new CourseContent("x".repeat(CourseContent.MAX_SIZE_BYTES)));
        assertThrows(IllegalArgumentException.class, () -> new CourseContent(""));
        assertThrows(IllegalArgumentException.class, () -> new CourseContent("x".repeat(CourseContent.MAX_SIZE_BYTES + 1)));
        assertThrows(NullPointerException.class, () -> new CourseContent(null));
    }
}
