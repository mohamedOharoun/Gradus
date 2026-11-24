package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Matching;
import es.ulpgc.pamn.dominio.backend.domain.course.MatchingPair;
import es.ulpgc.pamn.dominio.backend.domain.course.MultipleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.Option;
import es.ulpgc.pamn.dominio.backend.domain.course.PhotoExercise;
import es.ulpgc.pamn.dominio.backend.domain.course.SingleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.TextNormalizer;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reglas de la capa 3 sobre la coherencia interna de cada ejercicio, esté en {@code steps} o en
 * {@code review}: opciones distintas, respuestas que existen y parejas que forman una biyección.
 */
public final class ExerciseRules {

    private ExerciseRules() {
    }

    /** {@code DUPLICATE_OPTION_ID}: dos opciones del mismo ejercicio con el mismo id. */
    public static Stream<Issue> duplicateOptionIds(CourseDocument course) {
        return repeatedOptions(course, Option::id, CourseIssueCode.DUPLICATE_OPTION_ID,
                (exercise, repeat) -> "El ejercicio «" + exercise.id() + "» tiene dos opciones con el id «"
                        + repeat.repeated().value().id() + "» (la primera es " + repeat.first().path()
                        + "). Los ids de las opciones deben ser distintos dentro de cada ejercicio.");
    }

    /** {@code DUPLICATE_OPTION_TEXT}: dos opciones del mismo ejercicio con el mismo texto normalizado. */
    public static Stream<Issue> duplicateOptionTexts(CourseDocument course) {
        return repeatedOptions(course, option -> TextNormalizer.normalize(option.text()),
                CourseIssueCode.DUPLICATE_OPTION_TEXT,
                (exercise, repeat) -> "El ejercicio «" + exercise.id() + "» tiene dos opciones con el mismo "
                        + "texto: «" + repeat.repeated().value().text() + "» repite «"
                        + repeat.first().value().text() + "» (" + repeat.first().path() + "). "
                        + "Cambia o quita una de las dos.");
    }

    /** {@code UNKNOWN_OPTION}: {@code answer}, o un elemento de {@code correct}, que no es ninguna opción. */
    public static Stream<Issue> unknownOptions(CourseDocument course) {
        return Locations.exercises(course).flatMap(located -> {
            Exercise exercise = located.value();
            Set<String> ids = optionIds(exercise);
            Stream<Located<String>> references = switch (exercise) {
                case SingleChoice single -> Stream.of(new Located<>(located.path().key("answer"), single.answer()));
                case MultipleChoice multiple -> Locations.indexed(multiple.correct(), located.path().key("correct")).stream();
                case Matching matching -> Stream.empty();
                case PhotoExercise photo -> Stream.empty();
            };
            return references
                    .filter(reference -> !ids.contains(reference.value()))
                    .map(reference -> Issue.at(reference.path(), CourseIssueCode.UNKNOWN_OPTION,
                            "La respuesta «" + reference.value() + "» del ejercicio «" + exercise.id()
                                    + "» no es el id de ninguna de sus opciones. Opciones existentes: "
                                    + String.join(", ", optionIdsInOrder(exercise)) + "."));
        });
    }

    /** {@code ALL_OPTIONS_CORRECT}: en {@code multipleChoice}, marcarlo todo siempre acertaría. */
    public static Stream<Issue> allOptionsCorrect(CourseDocument course) {
        return Locations.exercises(course)
                .filter(located -> located.value() instanceof MultipleChoice multiple
                        && Set.copyOf(multiple.correct()).containsAll(optionIds(multiple)))
                .map(located -> Issue.at(located.path().key("correct"), CourseIssueCode.ALL_OPTIONS_CORRECT,
                        "En el ejercicio «" + located.value().id() + "» todas las opciones ("
                                + String.join(", ", optionIdsInOrder(located.value())) + ") son correctas: "
                                + "marcarlo todo siempre acertaría. Deja al menos una opción incorrecta."));
    }

    /** {@code DUPLICATE_MATCHING_SIDE}: dos izquierdas o dos derechas iguales rompen la biyección. */
    public static Stream<Issue> duplicateMatchingSides(CourseDocument course) {
        return Locations.exercises(course).flatMap(located -> {
            if (!(located.value() instanceof Matching matching)) {
                return Stream.empty();
            }
            List<Located<MatchingPair>> pairs = Locations.indexed(matching.pairs(), located.path().key("pairs"));
            return Stream.concat(
                    repeatedSides(matching, pairs, MatchingPair::left, "izquierdo"),
                    repeatedSides(matching, pairs, MatchingPair::right, "derecho"));
        });
    }

    private static Stream<Issue> repeatedSides(Matching matching, List<Located<MatchingPair>> pairs,
                                               Function<MatchingPair, String> side, String sideName) {
        return Duplicates.by(pairs, pair -> TextNormalizer.normalize(side.apply(pair)))
                .map(repeat -> Issue.at(repeat.repeated().path(), CourseIssueCode.DUPLICATE_MATCHING_SIDE,
                        "En el ejercicio «" + matching.id() + "» el lado " + sideName + " «"
                                + side.apply(repeat.repeated().value()) + "» ya aparece en "
                                + repeat.first().path() + ". Cada lado debe aparecer una sola vez para que "
                                + "solo haya una forma de unir las parejas."));
    }

    /** Message factory de las reglas de opciones repetidas: recibe el ejercicio y la repetición. */
    @FunctionalInterface
    private interface RepeatMessage {
        String of(Exercise exercise, Duplicates.Repeat<Option> repeat);
    }

    private static Stream<Issue> repeatedOptions(CourseDocument course, Function<Option, String> key,
                                                 CourseIssueCode code, RepeatMessage message) {
        return Locations.exercises(course).flatMap(located -> Duplicates.by(
                        Locations.indexed(Locations.options(located.value()), located.path().key("options")), key)
                .map(repeat -> Issue.at(repeat.repeated().path(), code, message.of(located.value(), repeat))));
    }

    private static Set<String> optionIds(Exercise exercise) {
        return Locations.options(exercise).stream().map(Option::id).collect(Collectors.toSet());
    }

    /** Ids sin repetir y en el orden del documento, para los mensajes. */
    private static List<String> optionIdsInOrder(Exercise exercise) {
        return Locations.options(exercise).stream().map(Option::id).distinct().toList();
    }
}
