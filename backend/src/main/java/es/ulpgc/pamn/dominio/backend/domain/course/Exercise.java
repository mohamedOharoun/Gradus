package es.ulpgc.pamn.dominio.backend.domain.course;

/**
 * Ejercicio: puede ser un paso formativo o estar en la reserva de repaso. Por eso {@code Module.review}
 * es una lista de {@code Exercise}: el compilador impide meter una lección en la reserva.
 */
public sealed interface Exercise extends Step permits SingleChoice, MultipleChoice, Matching {

    int MAX_PROMPT_LENGTH = 2_000;
    int MAX_EXPLANATION_LENGTH = 3_000;

    /** Enunciado en Markdown. */
    String prompt();

    /** Explicación en Markdown que se muestra tras responder; es obligatoria (§2.5). */
    String explanation();
}
