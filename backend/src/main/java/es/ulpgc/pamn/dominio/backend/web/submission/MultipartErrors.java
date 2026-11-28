package es.ulpgc.pamn.dominio.backend.web.submission;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.web.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.servlet.ServletException;

/**
 * Decisión O: Jetty lanza {@link ServletException} («400: bad multipart») tanto si el multipart está roto
 * como si pasa del límite sin haber enviado {@code Content-Length} (comprobado en Javalin 7.2.4). Las dos son
 * culpa del cliente: 400, no 500. Va aquí y no en {@code ExceptionHandlers} para no tocar ese fichero.
 */
public final class MultipartErrors {

    private MultipartErrors() {
    }

    public static void unreadable(ServletException exception, Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of("body", ErrorCode.MALFORMED_BODY,
                "El cuerpo no es un multipart válido o pasa de " + UploadLimit.MAX_REQUEST_BYTES + " bytes."));
    }
}
