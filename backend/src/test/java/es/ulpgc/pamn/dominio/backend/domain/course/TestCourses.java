package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Piezas de curso para los tests (factory methods) y punto de entrada de los builders.
 * {@code course().build()} es un curso válido y <b>sin avisos</b>: cada test cambia solo la pieza que
 * le interesa, como los fixtures de la especificación (§6) son mutaciones mínimas del ejemplo.
 * Es pública para que la usen también los tests de {@code domain.course.rules}.
 */
public final class TestCourses {

    /** Tamaño de la reserva por defecto: 2 · 4 preguntas, para que no salte MASTERY_POOL_SMALL_FOR_RETRY. */
    public static final int REVIEW_SIZE = 8;
    /** Con 5 opciones, p = 1/5 y la cota de 4 preguntas y 3 aciertos es 2,72 %: sin MASTERY_GUESSABLE. */
    public static final int REVIEW_OPTIONS = 5;

    private TestCourses() {
    }

    public static CourseBuilder course() {
        return new CourseBuilder();
    }

    public static ModuleBuilder module(String id) {
        return new ModuleBuilder(id);
    }

    public static Lesson lesson(String id) {
        return new Lesson(id, "Texto de la lección " + id + ".");
    }

    /** Respuesta única con {@code optionCount} opciones de ids a, b, c…; la correcta es «a». */
    public static SingleChoice singleChoice(String id, int optionCount) {
        return new SingleChoice(id, "Enunciado de " + id, options(optionCount), "a", "Explicación de " + id);
    }

    public static MultipleChoice multipleChoice(String id, int optionCount, String... correct) {
        return new MultipleChoice(id, "Enunciado de " + id, options(optionCount), List.of(correct),
                "Explicación de " + id);
    }

    public static Matching matching(String id, int pairCount) {
        List<MatchingPair> pairs = IntStream.rangeClosed(1, pairCount)
                .mapToObj(i -> new MatchingPair("Izquierda " + i, "Derecha " + i))
                .toList();
        return new Matching(id, "Enunciado de " + id, pairs, "Explicación de " + id);
    }

    public static PhotoExercise photo(String id) {
        return new PhotoExercise(id, "Enunciado de " + id, "Explicación de " + id);
    }

    /** Opciones con ids a, b, c… y textos «Opción A», «Opción B»… */
    public static List<Option> options(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> String.valueOf((char) ('a' + i)))
                .map(letter -> new Option(letter, "Opción " + letter.toUpperCase()))
                .toList();
    }

    /** Reserva por defecto de un módulo: {@code moduleId-r1}, {@code moduleId-r2}… */
    public static List<Exercise> reviewPool(String moduleId, int size) {
        return IntStream.rangeClosed(1, size)
                .<Exercise>mapToObj(i -> singleChoice(moduleId + "-r" + i, REVIEW_OPTIONS))
                .toList();
    }
}
