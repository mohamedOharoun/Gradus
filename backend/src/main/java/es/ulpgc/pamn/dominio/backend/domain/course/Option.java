package es.ulpgc.pamn.dominio.backend.domain.course;

/** Opción de un ejercicio de elección. Su id solo tiene que ser único dentro de su ejercicio. */
public record Option(String id, String text) {

    public static final int MAX_TEXT_LENGTH = 300;

    public Option {
        Require.id(id, "id");
        Require.markdown(text, "text", MAX_TEXT_LENGTH);
    }
}
