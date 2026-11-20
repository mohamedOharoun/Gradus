package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de las capas 3–5 (Pipeline / Chain of Responsibility): ejecuta las etapas en orden y se
 * para tras la primera que encuentra errores. Los avisos nunca detienen nada (§3.2). Inmutable.
 */
public final class CourseValidator {

    /** Capa 3: identidad y referencias. */
    public static final CourseRule IDENTITY_AND_REFERENCES = CourseRule.all(
            IdentityRules::duplicateIds,
            ExerciseRules::duplicateOptionIds,
            ExerciseRules::duplicateOptionTexts,
            ExerciseRules::unknownOptions,
            ExerciseRules::allOptionsCorrect,
            ExerciseRules::duplicateMatchingSides,
            PrerequisiteRules::unknownPrerequisites,
            PrerequisiteRules::selfPrerequisites,
            IdentityRules::duplicateTags);

    /** Capas 4 y 5 juntas: son independientes entre sí, así que se ejecutan ambas (§3.2). */
    public static final CourseRule GRAPH_AND_MASTERY = CourseRule.all(
            GraphRules::cycles,
            GraphRules::redundantPrerequisites,
            MasteryRules::minExceedsQuestions,
            MasteryRules::poolTooSmall,
            MasteryRules::poolSmallForRetry,
            MasteryRules::requiresPerfect,
            MasteryRules::guessable,
            PedagogyRules::modulesWithoutLesson,
            PedagogyRules::modulesWithoutExercise,
            PedagogyRules::matchingsWithTooManyPairs);

    private final List<CourseRule> stages;

    public CourseValidator(List<CourseRule> stages) {
        this.stages = List.copyOf(stages);
    }

    /** Factory method: el validador del formato v1 (capa 3 y, si pasa, capas 4 y 5). */
    public static CourseValidator standard() {
        return new CourseValidator(List.of(IDENTITY_AND_REFERENCES, GRAPH_AND_MASTERY));
    }

    public CourseReport validate(CourseDocument course) {
        List<Issue> found = new ArrayList<>();
        for (CourseRule stage : stages) {
            List<Issue> issues = stage.check(course).toList();
            found.addAll(issues);
            if (issues.stream().anyMatch(Issue::isError)) {
                break;
            }
        }
        return new CourseReport(found);
    }
}
