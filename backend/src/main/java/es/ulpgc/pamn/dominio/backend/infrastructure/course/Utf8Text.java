package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Primera parte de la capa 1 (§3.2): bytes → texto. Quita el BOM y decodifica UTF-8 en modo estricto:
 * una secuencia inválida es un error con su posición, nunca un carácter de sustitución silencioso.
 */
final class Utf8Text {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /** Resultado de decodificar: el texto, o la posición (en el cuerpo recibido) del primer byte inválido. */
    sealed interface Result permits Decoded, Malformed {
    }

    record Decoded(String text) implements Result {
    }

    record Malformed(int position) implements Result {
    }

    private Utf8Text() {
    }

    static byte[] withoutBom(byte[] body) {
        return hasBom(body) ? Arrays.copyOfRange(body, BOM.length, body.length) : body;
    }

    static Result decode(byte[] body) {
        byte[] bytes = withoutBom(body);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(bytes);
        // En UTF-8 cada byte da como mucho un char: el búfer nunca se queda corto.
        CharBuffer out = CharBuffer.allocate(bytes.length);
        CoderResult result = decoder.decode(in, out, true);
        if (result.isError()) {
            return new Malformed(in.position() + (body.length - bytes.length));
        }
        decoder.flush(out);
        return new Decoded(out.flip().toString());
    }

    private static boolean hasBom(byte[] body) {
        return body.length >= BOM.length && Arrays.equals(body, 0, BOM.length, BOM, 0, BOM.length);
    }
}
