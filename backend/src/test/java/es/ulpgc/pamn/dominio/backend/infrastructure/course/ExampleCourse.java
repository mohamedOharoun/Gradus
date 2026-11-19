package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** El curso de ejemplo de la especificación ({@code ejemplo-grafos.json}), base de los tests del lector. */
final class ExampleCourse {

    static final String RESOURCE = "/courses/valid/ejemplo-grafos.json";

    private ExampleCourse() {
    }

    static byte[] bytes() {
        try (InputStream in = ExampleCourse.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encuentra " + RESOURCE);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String text() {
        return new String(bytes(), StandardCharsets.UTF_8);
    }

    /** Un árbol nuevo en cada llamada: los tests pueden modificarlo sin afectar a los demás. */
    static ObjectNode tree() {
        try {
            return (ObjectNode) new ObjectMapper().readTree(bytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
