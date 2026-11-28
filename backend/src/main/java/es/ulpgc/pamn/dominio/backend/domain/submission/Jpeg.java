package es.ulpgc.pamn.dominio.backend.domain.submission;

import java.util.Arrays;

/**
 * La foto de una entrega: bytes que empiezan como un JPEG y caben en el límite. Solo se crea con
 * {@link #check}, así que un {@code Jpeg} siempre es válido. Inmutable: copia los bytes al entrar y
 * al salir (un {@code byte[]} se puede cambiar desde fuera).
 *
 * <p>No es un {@code record} a propósito: en un record, {@code equals} compararía la referencia del
 * array y no su contenido, y {@code toString} lo imprimiría entero.
 */
public final class Jpeg {

    /**
     * Toda petición está limitada a {@code CourseContent.MAX_SIZE_BYTES} (2 MiB, {@code maxRequestSize} en
     * {@code App.java}). El multipart añade separadores y el campo {@code version}, así que la foto se queda
     * en 2 000 000 bytes para dejar margen. La app la reduce antes de subirla (≈ 0,5–1,5 MB).
     */
    public static final int MAX_BYTES = 2_000_000;

    /** FF D8 (inicio de imagen) seguido de FF (el primer marcador). Lo tiene todo JPEG, sea JFIF o EXIF. */
    private static final byte[] SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final byte[] bytes;

    private Jpeg(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    /**
     * Factory method: comprueba los bytes tal como llegaron. No mira el nombre del fichero ni el tipo que
     * diga el cliente, que se pueden falsear. El tamaño se mira antes que la firma: si algo es enorme, eso es
     * lo que hay que contar.
     */
    public static JpegCheck check(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new JpegCheck.Empty();
        }
        if (bytes.length > MAX_BYTES) {
            return new JpegCheck.TooLarge(bytes.length);
        }
        if (!startsWithSignature(bytes)) {
            return new JpegCheck.NotJpeg();
        }
        return new JpegCheck.Valid(new Jpeg(bytes));
    }

    private static boolean startsWithSignature(byte[] bytes) {
        return bytes.length >= SIGNATURE.length
                && Arrays.equals(bytes, 0, SIGNATURE.length, SIGNATURE, 0, SIGNATURE.length);
    }

    /** Una copia: cambiarla no cambia la foto. */
    public byte[] bytes() {
        return bytes.clone();
    }

    public int size() {
        return bytes.length;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Jpeg jpeg && Arrays.equals(bytes, jpeg.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    /** El tamaño, nunca los bytes: una foto de 2 MB no debe acabar en los logs. */
    @Override
    public String toString() {
        return "Jpeg[" + bytes.length + " bytes]";
    }
}
