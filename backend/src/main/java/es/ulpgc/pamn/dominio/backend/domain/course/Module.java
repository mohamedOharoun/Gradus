package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Módulo del curso: un nodo del grafo de prerrequisitos (§2.3). Que los prerrequisitos existan, que
 * no se requiera a sí mismo y que no haya ciclos lo comprueban las reglas, no el constructor.
 *
 * @param requires ids de los módulos prerrequisito; solo se guarda esta dirección
 * @param mastery  prueba propia; si está vacía, se usa la del curso (ver {@link #effectiveMastery})
 * @param steps    parte formativa: lecciones y ejercicios, en orden de presentación
 * @param review   reserva de ejercicios para la prueba de dominio y los repasos
 */
public record Module(String id, String title, List<String> requires, Optional<Mastery> mastery,
                     List<Step> steps, List<Exercise> review) {

    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_REQUIRES = 20;
    public static final int MIN_STEPS = 1;
    public static final int MAX_STEPS = 50;
    public static final int MIN_REVIEW = 1;
    public static final int MAX_REVIEW = 100;

    public Module {
        Require.id(id, "id");
        Require.plainText(title, "title", MAX_TITLE_LENGTH);
        requires = Require.ids(requires, "requires", 0, MAX_REQUIRES);
        Objects.requireNonNull(mastery, "mastery");
        steps = Require.size(steps, "steps", MIN_STEPS, MAX_STEPS);
        review = Require.size(review, "review", MIN_REVIEW, MAX_REVIEW);
    }

    /** Prueba efectiva (§2.6): la del módulo si la tiene; si no, la del curso. */
    public Mastery effectiveMastery(Mastery courseDefault) {
        return mastery.orElse(courseDefault);
    }
}
