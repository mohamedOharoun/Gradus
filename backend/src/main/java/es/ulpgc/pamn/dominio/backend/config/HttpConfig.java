package es.ulpgc.pamn.dominio.backend.config;

import java.util.Map;

/** Configuración del servidor HTTP. */
public record HttpConfig(int port) {

    private static final int DEFAULT_PORT = 7070;

    public static HttpConfig fromEnvironment(Map<String, String> env) {
        return new HttpConfig(Environment.port(env, "GRADUS_HTTP_PORT", DEFAULT_PORT));
    }
}
