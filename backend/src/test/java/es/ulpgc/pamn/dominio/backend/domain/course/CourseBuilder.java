package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Builder de {@link CourseDocument} para los tests. Por defecto es un curso válido y sin avisos con
 * dos módulos: «representacion» y «bfs», que requiere el primero.
 */
public final class CourseBuilder {

    private int formatVersion = CourseDocument.FORMAT_VERSION;
    private String title = "Introducción a los grafos";
    private String description = "Cómo se guarda un grafo en memoria y cómo se recorre en anchura.";
    private String language = "es";
    private List<String> tags = List.of("grafos", "algoritmos");
    private Mastery defaultMastery = new Mastery(4, 3);
    // TestCourses.module explícito: el método module(int, …) de esta clase oculta el import estático.
    private List<Module> modules = List.of(
            TestCourses.module("representacion").build(),
            TestCourses.module("bfs").requires("representacion").build());

    CourseBuilder() {
    }

    public CourseBuilder formatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
        return this;
    }

    public CourseBuilder title(String title) {
        this.title = title;
        return this;
    }

    public CourseBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CourseBuilder language(String language) {
        this.language = language;
        return this;
    }

    public CourseBuilder tags(String... tags) {
        this.tags = List.of(tags);
        return this;
    }

    public CourseBuilder defaultMastery(int questions, int minCorrect) {
        this.defaultMastery = new Mastery(questions, minCorrect);
        return this;
    }

    public CourseBuilder modules(Module... modules) {
        return modules(List.of(modules));
    }

    public CourseBuilder modules(List<Module> modules) {
        this.modules = modules;
        return this;
    }

    /** Cambia solo el módulo {@code index}: {@code module(1, m -> m.requires("otro"))}. */
    public CourseBuilder module(int index, UnaryOperator<ModuleBuilder> change) {
        List<Module> changed = new ArrayList<>(modules);
        changed.set(index, change.apply(ModuleBuilder.from(modules.get(index))).build());
        return modules(changed);
    }

    public CourseDocument build() {
        return new CourseDocument(formatVersion, title, description, language, tags, defaultMastery, modules);
    }
}
