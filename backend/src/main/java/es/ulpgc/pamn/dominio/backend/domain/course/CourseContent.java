package es.ulpgc.pamn.dominio.backend.domain.course;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * El JSON de un curso tal como lo envió el autor (sin el BOM, si lo había). Se guarda y se sirve
 * sin modificar (§4.1). El hash y el tamaño se calculan del propio texto: como viene de UTF-8 estricto,
 * volver a codificarlo da los mismos bytes, así que nunca pueden contradecirse.
 */
public record CourseContent(String text) {

    /** 2 MiB, el límite del cuerpo de la petición (§4.7) y de la restricción de la base de datos. */
    public static final int MAX_SIZE_BYTES = 2_097_152;

    public CourseContent {
        Objects.requireNonNull(text, "text");
        int size = text.getBytes(StandardCharsets.UTF_8).length;
        if (size < 1 || size > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "El contenido debe ocupar entre 1 y " + MAX_SIZE_BYTES + " bytes; ocupa " + size);
        }
    }

    public byte[] bytes() {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    public int sizeBytes() {
        return bytes().length;
    }

    /** SHA-256 de los bytes UTF-8, en hexadecimal en minúsculas. */
    public String sha256() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en este JDK", e);
        }
    }
}
