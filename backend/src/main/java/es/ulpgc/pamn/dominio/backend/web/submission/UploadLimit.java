package es.ulpgc.pamn.dominio.backend.web.submission;

/**
 * Tamaño máximo de una petición con foto (docs/tareas/ejercicio-foto.md, decisiones N y O).
 *
 * <p>{@code maxRequestSize} de Javalin solo limita {@code ctx.bodyAsBytes()}, no el multipart, que por
 * defecto no tiene límite (comprobado en Javalin 7.2.4). Por eso hay dos barreras: {@link #exceededBy}
 * mira el {@code Content-Length} antes de leer nada (413), y {@code App.java} configura el multipart de
 * Jetty con {@link #MAX_REQUEST_BYTES} para quien no envíe esa cabecera.
 */
public final class UploadLimit {

    /** 2 MiB, el mismo que el de un curso. La foto ({@code Jpeg.MAX_BYTES}) cabe con margen. */
    public static final int MAX_REQUEST_BYTES = 2 * 1024 * 1024;

    private UploadLimit() {
    }

    /**
     * Si una petición de {@code contentLength} bytes pasa del límite. Javalin da {@code -1} cuando no hay
     * {@code Content-Length}: eso no se juzga aquí, lo frena el límite del multipart.
     */
    static boolean exceededBy(long contentLength) {
        return contentLength > MAX_REQUEST_BYTES;
    }
}
