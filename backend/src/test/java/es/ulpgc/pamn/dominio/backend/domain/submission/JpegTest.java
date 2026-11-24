package es.ulpgc.pamn.dominio.backend.domain.submission;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpegTest {

    /** Los tres primeros bytes de todo JPEG (la «firma») seguidos de relleno hasta {@code size}. */
    static byte[] jpegOfSize(int size) {
        byte[] bytes = new byte[size];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }

    static Jpeg valid(byte[] bytes) {
        return assertInstanceOf(JpegCheck.Valid.class, Jpeg.check(bytes)).jpeg();
    }

    @Test
    void bytesThatStartWithTheJpegSignatureAreAJpeg() {
        assertEquals(100, valid(jpegOfSize(100)).size());
    }

    @Test
    void theSignatureAloneIsEnough() {
        // Solo se comprueba que lo parece: decodificar la imagen entera no es cosa del servidor.
        assertEquals(3, valid(jpegOfSize(3)).size());
    }

    @Test
    void noBytesAtAllIsEmpty() {
        assertInstanceOf(JpegCheck.Empty.class, Jpeg.check(new byte[0]));
    }

    @Test
    void aMissingPhotoIsEmpty() {
        // El multipart sin el campo «photo»: el controlador pasa null.
        assertInstanceOf(JpegCheck.Empty.class, Jpeg.check(null));
    }

    @Test
    void aPngIsNotAJpeg() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

        assertInstanceOf(JpegCheck.NotJpeg.class, Jpeg.check(png));
    }

    @Test
    void textIsNotAJpegWhateverTheClientSays() {
        // No se mira el nombre del fichero ni el Content-Type que manda el cliente: solo los bytes.
        assertInstanceOf(JpegCheck.NotJpeg.class, Jpeg.check("foto.jpg".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void lessThanTheSignatureIsNotAJpeg() {
        assertInstanceOf(JpegCheck.NotJpeg.class, Jpeg.check(new byte[] {(byte) 0xFF, (byte) 0xD8}));
    }

    @Test
    void upToTheMaximumIsAccepted() {
        assertEquals(Jpeg.MAX_BYTES, valid(jpegOfSize(Jpeg.MAX_BYTES)).size());
    }

    @Test
    void oneByteOverTheMaximumIsTooLargeAndSaysHowBig() {
        JpegCheck.TooLarge tooLarge = assertInstanceOf(JpegCheck.TooLarge.class,
                Jpeg.check(jpegOfSize(Jpeg.MAX_BYTES + 1)));

        assertEquals(Jpeg.MAX_BYTES + 1, tooLarge.size());
    }

    @Test
    void theSizeIsCheckedBeforeTheSignature() {
        // Algo enorme se rechaza por tamaño aunque no sea un JPEG: es lo que de verdad hay que contarle.
        assertInstanceOf(JpegCheck.TooLarge.class, Jpeg.check(new byte[Jpeg.MAX_BYTES + 1]));
    }

    @Test
    void theMaximumLeavesRoomForTheRestOfTheRequest() {
        // Toda petición está limitada al tamaño de un curso (maxRequestSize en App.java). El multipart
        // añade separadores y el campo «version»: la foto tiene que caber con margen.
        assertTrue(Jpeg.MAX_BYTES + 64 * 1024 <= CourseContent.MAX_SIZE_BYTES);
    }

    @Test
    void changingTheOriginalArrayDoesNotChangeTheJpeg() {
        byte[] bytes = jpegOfSize(10);
        Jpeg jpeg = valid(bytes);

        bytes[5] = 42;

        assertEquals(0, jpeg.bytes()[5]);
    }

    @Test
    void changingTheReturnedBytesDoesNotChangeTheJpeg() {
        Jpeg jpeg = valid(jpegOfSize(10));

        jpeg.bytes()[5] = 42;

        assertEquals(0, jpeg.bytes()[5]);
    }

    @Test
    void twoJpegsWithTheSameBytesAreEqual() {
        assertEquals(valid(jpegOfSize(10)), valid(jpegOfSize(10)));
        assertEquals(valid(jpegOfSize(10)).hashCode(), valid(jpegOfSize(10)).hashCode());
        assertNotEquals(valid(jpegOfSize(10)), valid(jpegOfSize(11)));
    }

    @Test
    void theBytesAreKeptExactly() {
        byte[] bytes = jpegOfSize(10);
        Arrays.fill(bytes, 3, 10, (byte) 7);

        assertArrayEquals(bytes, valid(bytes).bytes());
    }

    @Test
    void toStringShowsTheSizeButNotTheBytes() {
        String text = valid(jpegOfSize(1234)).toString();

        assertTrue(text.contains("1234"), text);
        assertFalse(text.contains("[-1"), text);
    }
}
