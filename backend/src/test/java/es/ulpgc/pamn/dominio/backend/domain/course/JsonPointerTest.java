package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonPointerTest {

    @Test
    void rootIsTheEmptyString() {
        assertEquals("", JsonPointer.root().toString());
    }

    @Test
    void buildsAPathFromKeysAndIndexes() {
        JsonPointer pointer = JsonPointer.root().key("modules").index(1).key("review").index(3);

        assertEquals("/modules/1/review/3", pointer.toString());
    }

    @Test
    void escapesTildeAndSlashAsRfc6901Requires() {
        // Primero «~» → «~0» y después «/» → «~1»; al revés, «/» acabaría como «~01».
        assertEquals("/a~1b~0c", JsonPointer.root().key("a/b~c").toString());
    }

    @Test
    void addingATokenLeavesTheOriginalUnchanged() {
        JsonPointer modules = JsonPointer.root().key("modules");

        modules.index(0);

        assertEquals("/modules", modules.toString());
    }

    @Test
    void pointersWithTheSameTokensAreEqual() {
        assertEquals(JsonPointer.root().key("defaults").key("mastery"),
                JsonPointer.root().key("defaults").key("mastery"));
    }

    @Test
    void rejectsNegativeIndexesAndNullKeys() {
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.root().index(-1));
        assertThrows(NullPointerException.class, () -> JsonPointer.root().key(null));
    }
}
