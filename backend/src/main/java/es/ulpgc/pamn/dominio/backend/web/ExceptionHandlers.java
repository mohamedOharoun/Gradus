package es.ulpgc.pamn.dominio.backend.web;

import com.fasterxml.jackson.core.JacksonException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/** Respuestas para errores que no pasan por los casos de uso. */
public final class ExceptionHandlers {

    private ExceptionHandlers() {
    }

    /** Cuerpo que no es JSON válido o no encaja con lo esperado: es culpa del cliente (400), no del servidor (500). */
    public static void invalidJson(JacksonException exception, Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST)
                .json(ErrorResponse.of("body", "El cuerpo de la petición no es un JSON válido."));
    }
}
