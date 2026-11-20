package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;

/**
 * Selección múltiple: es correcto elegir exactamente el conjunto {@code correct}. Que esos ids existan
 * y que no estén todas las opciones lo comprueban las reglas {@code UNKNOWN_OPTION} y
 * {@code ALL_OPTIONS_CORRECT}, no el constructor.
 */
public record MultipleChoice(String id, String prompt, List<Option> options, List<String> correct,
                             String explanation) implements Exercise {

    public static final int MIN_OPTIONS = 3;
    public static final int MAX_OPTIONS = 6;
    public static final int MIN_CORRECT = 1;
    public static final int MAX_CORRECT = 5;

    public MultipleChoice {
        Require.exerciseFields(id, prompt, explanation);
        options = Require.size(options, "options", MIN_OPTIONS, MAX_OPTIONS);
        correct = Require.ids(correct, "correct", MIN_CORRECT, MAX_CORRECT);
    }
}
