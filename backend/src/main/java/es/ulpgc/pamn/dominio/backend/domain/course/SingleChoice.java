package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;

/**
 * Respuesta única: es correcto elegir exactamente la opción {@code answer}. Que {@code answer} sea el
 * id de una de las opciones lo comprueba la regla {@code UNKNOWN_OPTION}, no el constructor.
 */
public record SingleChoice(String id, String prompt, List<Option> options, String answer, String explanation)
        implements Exercise {

    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 6;

    public SingleChoice {
        Require.exerciseFields(id, prompt, explanation);
        options = Require.size(options, "options", MIN_OPTIONS, MAX_OPTIONS);
        Require.id(answer, "answer");
    }
}
