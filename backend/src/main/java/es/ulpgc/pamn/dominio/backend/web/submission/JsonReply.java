package es.ulpgc.pamn.dominio.backend.web.submission;

import io.javalin.http.HttpStatus;

import java.util.Objects;

/**
 * Una respuesta ya decidida: el código HTTP y el cuerpo que irá en JSON. Separa el «qué responder»
 * (puro, con tests) del «enviarlo» ({@code ctx.status(...).json(...)}, en el controlador).
 */
record JsonReply(HttpStatus status, Object body) {

    JsonReply {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(body, "body");
    }
}
