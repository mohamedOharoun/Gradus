package es.ulpgc.pamn.dominio.backend.web.course;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathIdsTest {

    @Test
    void aWellFormedUuidIsACourseId() {
        assertEquals(Optional.of(UUID.fromString("0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b")),
                PathIds.courseId("0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1-2-3-4-5", "0b8f1e2a3c4d4e5f8a9b0c1d2e3f4a5b",
            "0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b-extra"})
    void anythingElseIsNoCourse(String raw) {
        // §4.3: un {id} que no es un UUID se responde como 404, no como 400.
        assertEquals(Optional.empty(), PathIds.courseId(raw));
    }

    @Test
    void versionsArePositiveWholeNumbers() {
        assertEquals(Optional.of(1), PathIds.version("1"));
        assertEquals(Optional.of(12), PathIds.version("12"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "1.5", "+2", "abc", "99999999999"})
    void anythingElseIsNoVersion(String raw) {
        assertEquals(Optional.empty(), PathIds.version(raw));
    }
}
