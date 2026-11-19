package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Utf8TextTest {

    static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @Test
    void decodesValidUtf8() {
        assertEquals(new Utf8Text.Decoded("Introducción"),
                Utf8Text.decode("Introducción".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aLeadingBomIsRemovedBeforeAnything() {
        byte[] withBom = concat(BOM, "{}".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), Utf8Text.withoutBom(withBom));
        assertEquals(new Utf8Text.Decoded("{}"), Utf8Text.decode(withBom));
    }

    @Test
    void anInvalidSequenceIsReportedWithItsPositionInsteadOfBeingReplaced() {
        // 0xFF nunca aparece en UTF-8. Sin REPORT se cambiaría en silencio por U+FFFD.
        byte[] body = {'{', '"', 'a', (byte) 0xFF, '"', '}'};

        assertEquals(new Utf8Text.Malformed(3), Utf8Text.decode(body));
    }

    @Test
    void thePositionCountsTheBomIfThereWasOne() {
        byte[] body = concat(BOM, new byte[]{'{', (byte) 0xC3, '}'});   // 0xC3 empieza una secuencia de dos bytes

        assertEquals(new Utf8Text.Malformed(4), Utf8Text.decode(body));
    }
}
