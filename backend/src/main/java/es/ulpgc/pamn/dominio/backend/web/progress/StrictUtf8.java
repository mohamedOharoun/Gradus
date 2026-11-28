package es.ulpgc.pamn.dominio.backend.web.progress;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Bytes → texto UTF-8 que <b>falla</b> con una secuencia incorrecta. {@code new String(bytes, UTF_8)} la
 * sustituiría en silencio por {@code U+FFFD}, y ese carácter acabaría dentro de un {@code courseId}. No quita
 * el BOM: si el cuerpo lo trae, el JSON no empieza por {@code '{'} y no será válido.
 */
final class StrictUtf8 {

    private StrictUtf8() {
    }

    static Optional<String> decode(byte[] bytes) {
        try {
            return Optional.of(StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString());
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }
}
