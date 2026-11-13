package es.ulpgc.pamn.dominio.backend.config;

import java.util.Map;
import java.util.Optional;

/** Lectura de variables de entorno con mensajes claros. Recibe el mapa por parámetro para poder probarlo. */
final class Environment {

    private Environment() {
    }

    static String required(Map<String, String> env, String name) {
        return value(env, name)
                .orElseThrow(() -> new IllegalStateException("Falta la variable de entorno " + name));
    }

    static String optional(Map<String, String> env, String name, String defaultValue) {
        return value(env, name).orElse(defaultValue);
    }

    static int port(Map<String, String> env, String name, int defaultValue) {
        String raw = optional(env, name, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("La variable " + name + " debe ser un número de puerto: " + raw, e);
        }
    }

    private static Optional<String> value(Map<String, String> env, String name) {
        return Optional.ofNullable(env.get(name)).map(String::strip).filter(v -> !v.isEmpty());
    }
}
