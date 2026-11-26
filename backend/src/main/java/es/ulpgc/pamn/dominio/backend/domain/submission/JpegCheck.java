package es.ulpgc.pamn.dominio.backend.domain.submission;

/**
 * Lo que sale de comprobar los bytes de una foto ({@link Jpeg#check}). Es un valor, no una excepción:
 * quien lo recibe decide qué responder en cada caso (el caso de uso, y al final la capa web).
 */
public sealed interface JpegCheck {

    /** Es un JPEG y cabe. */
    record Valid(Jpeg jpeg) implements JpegCheck {
    }

    /** No ha llegado ninguna foto, o ha llegado vacía. */
    record Empty() implements JpegCheck {
    }

    /** Ha llegado algo que no empieza como un JPEG (otro formato, texto…). */
    record NotJpeg() implements JpegCheck {
    }

    /** Pasa de {@link Jpeg#MAX_BYTES}; {@code size} es lo que ocupa, para el mensaje. */
    record TooLarge(int size) implements JpegCheck {
    }
}
