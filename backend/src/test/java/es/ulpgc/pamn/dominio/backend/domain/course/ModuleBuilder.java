package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.List;
import java.util.Optional;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.REVIEW_SIZE;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.reviewPool;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.singleChoice;

/**
 * Builder de {@link Module} para los tests. Por defecto: sin prerrequisitos ni prueba propia, una
 * lección y un ejercicio formativos, y la reserva de {@link TestCourses#reviewPool}.
 */
public final class ModuleBuilder {

    private final String id;
    private String title;
    private List<String> requires = List.of();
    private Optional<Mastery> mastery = Optional.empty();
    private List<Step> steps;
    private List<Exercise> review;

    ModuleBuilder(String id) {
        this.id = id;
        this.title = "Módulo " + id;
        this.steps = List.of(lesson(id + "-l1"), singleChoice(id + "-f1", 3));
        this.review = reviewPool(id, REVIEW_SIZE);
    }

    /** Parte de un módulo existente, para cambiarle solo una pieza. */
    public static ModuleBuilder from(Module module) {
        return new ModuleBuilder(module.id())
                .title(module.title())
                .requires(module.requires().toArray(String[]::new))
                .mastery(module.mastery())
                .steps(module.steps())
                .review(module.review());
    }

    public ModuleBuilder title(String title) {
        this.title = title;
        return this;
    }

    public ModuleBuilder requires(String... requires) {
        this.requires = List.of(requires);
        return this;
    }

    public ModuleBuilder mastery(int questions, int minCorrect) {
        return mastery(Optional.of(new Mastery(questions, minCorrect)));
    }

    public ModuleBuilder mastery(Optional<Mastery> mastery) {
        this.mastery = mastery;
        return this;
    }

    public ModuleBuilder steps(Step... steps) {
        return steps(List.of(steps));
    }

    public ModuleBuilder steps(List<Step> steps) {
        this.steps = steps;
        return this;
    }

    public ModuleBuilder review(Exercise... review) {
        return review(List.of(review));
    }

    public ModuleBuilder review(List<Exercise> review) {
        this.review = review;
        return this;
    }

    public Module build() {
        return new Module(id, title, requires, mastery, steps, review);
    }
}
