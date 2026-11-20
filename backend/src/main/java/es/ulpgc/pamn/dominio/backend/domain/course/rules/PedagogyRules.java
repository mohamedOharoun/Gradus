package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Lesson;
import es.ulpgc.pamn.dominio.backend.domain.course.Matching;
import es.ulpgc.pamn.dominio.backend.domain.course.Step;

import java.util.function.Predicate;
import java.util.stream.Stream;

/** Reglas de la capa 5 sobre pedagogía (§3.3): todas son avisos y no dependen de la prueba de dominio. */
public final class PedagogyRules {

    /** Más parejas que estas son incómodas de unir en la pantalla de un móvil. */
    public static final int MAX_COMFORTABLE_PAIRS = 6;

    private PedagogyRules() {
    }

    /** {@code MODULE_WITHOUT_LESSON}: la parte formativa no explica nada antes de preguntar. */
    public static Stream<Issue> modulesWithoutLesson(CourseDocument course) {
        return modulesWhoseStepsLack(course, step -> step instanceof Lesson, CourseIssueCode.MODULE_WITHOUT_LESSON,
                "no tiene ninguna lección en steps: el alumno solo verá preguntas. Añade una lección que "
                        + "explique el tema antes de los ejercicios.");
    }

    /** {@code MODULE_WITHOUT_EXERCISE}: la parte formativa no deja practicar antes de la prueba. */
    public static Stream<Issue> modulesWithoutExercise(CourseDocument course) {
        return modulesWhoseStepsLack(course, step -> step instanceof Exercise, CourseIssueCode.MODULE_WITHOUT_EXERCISE,
                "no tiene ningún ejercicio formativo en steps: el alumno llegará a la prueba de dominio sin "
                        + "haber practicado. Intercala algún ejercicio entre las lecciones.");
    }

    /** {@code MATCHING_TOO_MANY_PAIRS}: más de 6 parejas, esté el ejercicio en steps o en review. */
    public static Stream<Issue> matchingsWithTooManyPairs(CourseDocument course) {
        return Locations.exercises(course).flatMap(located ->
                located.value() instanceof Matching matching && matching.pairs().size() > MAX_COMFORTABLE_PAIRS
                        ? Stream.of(Issue.at(located.path().key("pairs"), CourseIssueCode.MATCHING_TOO_MANY_PAIRS,
                        "El ejercicio «" + matching.id() + "» tiene " + matching.pairs().size() + " parejas: "
                                + "más de " + MAX_COMFORTABLE_PAIRS + " son incómodas en la pantalla de un móvil. "
                                + "Divídelo en dos ejercicios."))
                        : Stream.empty());
    }

    private static Stream<Issue> modulesWhoseStepsLack(CourseDocument course, Predicate<Step> kind,
                                                       CourseIssueCode code, String problem) {
        return Locations.modules(course).stream()
                .filter(module -> module.value().steps().stream().noneMatch(kind))
                .map(module -> Issue.at(module.path().key("steps"), code,
                        "El módulo «" + module.value().id() + "» " + problem));
    }
}
