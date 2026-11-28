package es.ulpgc.pamn.dominio.backend.domain.course;

/** Lección: texto en Markdown que se lee antes o entre los ejercicios formativos. */
public record Lesson(String id, String text) implements Step {

    public static final int MAX_TEXT_LENGTH = 10_000;

    public Lesson {
        Require.id(id, "id");
        Require.markdown(text, "text", MAX_TEXT_LENGTH);
    }
}
