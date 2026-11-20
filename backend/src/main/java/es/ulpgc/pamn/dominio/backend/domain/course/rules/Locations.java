package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;
import es.ulpgc.pamn.dominio.backend.domain.course.Matching;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;
import es.ulpgc.pamn.dominio.backend.domain.course.MultipleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.Option;
import es.ulpgc.pamn.dominio.backend.domain.course.SingleChoice;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Recorridos del curso en orden de documento, con la ruta de cada elemento. Las reglas los usan para
 * no repetir en cada una cómo se calculan rutas como {@code /modules/1/review/3}.
 */
final class Locations {

    static final JsonPointer MODULES = JsonPointer.root().key("modules");
    static final JsonPointer TAGS = JsonPointer.root().key("tags");

    private Locations() {
    }

    /** Cada elemento de {@code values} con la ruta {@code list/índice}. */
    static <T> List<Located<T>> indexed(List<? extends T> values, JsonPointer list) {
        return IntStream.range(0, values.size())
                .mapToObj(i -> new Located<T>(list.index(i), values.get(i)))
                .toList();
    }

    static List<Located<Module>> modules(CourseDocument course) {
        return indexed(course.modules(), MODULES);
    }

    /**
     * Los ids de módulos, lecciones y ejercicios, que comparten un único espacio de nombres (§2.4).
     * La ruta de cada id es la del objeto que lo lleva: {@code /modules/1} o {@code /modules/1/steps/0}.
     */
    static Stream<Located<String>> ids(CourseDocument course) {
        return modules(course).stream().flatMap(module -> Stream.of(
                        Stream.of(new Located<>(module.path(), module.value().id())),
                        indexed(module.value().steps(), module.path().key("steps")).stream()
                                .map(step -> new Located<>(step.path(), step.value().id())),
                        indexed(module.value().review(), module.path().key("review")).stream()
                                .map(exercise -> new Located<>(exercise.path(), exercise.value().id())))
                .flatMap(stream -> stream));
    }

    /** Todos los ejercicios del curso: los formativos de {@code steps} y los de {@code review}. */
    static Stream<Located<Exercise>> exercises(CourseDocument course) {
        return modules(course).stream().flatMap(module -> Stream.concat(
                indexed(module.value().steps(), module.path().key("steps")).stream()
                        .flatMap(step -> step.value() instanceof Exercise exercise
                                ? Stream.of(new Located<>(step.path(), exercise))
                                : Stream.empty()),
                indexed(module.value().review(), module.path().key("review")).stream()));
    }

    /** Las opciones de un ejercicio de elección; un {@code matching} no tiene. */
    static List<Option> options(Exercise exercise) {
        return switch (exercise) {
            case SingleChoice single -> single.options();
            case MultipleChoice multiple -> multiple.options();
            case Matching matching -> List.of();
        };
    }
}
