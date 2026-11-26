package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReadResult;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.PhotoExercise;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.ulpgc.pamn.dominio.backend.infrastructure.course.JacksonCourseReaderTest.READER;
import static es.ulpgc.pamn.dominio.backend.infrastructure.course.JacksonCourseReaderTest.parsed;
import static es.ulpgc.pamn.dominio.backend.infrastructure.course.JacksonCourseReaderTest.utf8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El paso {@code photo} (docs/tareas/ejercicio-foto.md, tarea 1): schema, lectura y validación. */
class PhotoStepReadingTest {

    /** El ejemplo con un paso {@code photo} añadido al final de los pasos del primer módulo (índice 4). */
    static ObjectNode exampleWithPhotoStep() {
        ObjectNode tree = ExampleCourse.tree();
        ((ArrayNode) tree.get("modules").get(0).get("steps")).addObject()
                .put("type", "photo")
                .put("id", "repr-a-mano")
                .put("prompt", "Dibuja la **lista de adyacencia** del grafo de la lección.")
                .put("explanation", "A → B, C; B → D; C → D.");
        return tree;
    }

    static ObjectNode photoStep(ObjectNode tree) {
        return (ObjectNode) tree.get("modules").get(0).get("steps").get(4);
    }

    static Issue single(CourseReadResult result) {
        List<Issue> issues = assertInstanceOf(CourseReadResult.Invalid.class, result).report().issues();
        assertEquals(1, issues.size(), issues.toString());
        return issues.getFirst();
    }

    @Test
    void aPhotoStepBecomesAPhotoExerciseWithItsFields() {
        assertEquals(
                new PhotoExercise("repr-a-mano", "Dibuja la **lista de adyacencia** del grafo de la lección.",
                        "A → B, C; B → D; C → D."),
                parsed(utf8(exampleWithPhotoStep())).modules().getFirst().steps().get(4));
    }

    @Test
    void aCourseWithAPhotoStepIsValidAndHasNoWarnings() {
        // Es lo que decide si se publica (201): ni errores ni avisos nuevos por el paso con foto.
        assertEquals(List.of(), CourseValidator.standard().validate(parsed(utf8(exampleWithPhotoStep()))).issues());
    }

    @Test
    void aPhotoStepWithoutExplanationIsRejectedOnItsObject() {
        // La explicación es la solución que ve el alumno al entregar: obligatoria, como en los demás (§2.5).
        ObjectNode tree = exampleWithPhotoStep();
        photoStep(tree).remove("explanation");

        Issue issue = single(READER.read(utf8(tree)));

        assertEquals(CourseIssueCode.SCHEMA_VIOLATION, issue.code());
        assertEquals("/modules/0/steps/4", issue.path().toString());
        assertTrue(issue.message().contains("explanation"), issue.message());
    }

    @Test
    void aPhotoStepWithoutPromptIsRejected() {
        ObjectNode tree = exampleWithPhotoStep();
        photoStep(tree).remove("prompt");

        assertEquals("/modules/0/steps/4", single(READER.read(utf8(tree))).path().toString());
    }

    @Test
    void aPhotoStepDoesNotAcceptFieldsOfOtherExercises() {
        // additionalProperties: false también aquí: un «answer» suelto es una errata, no una opción.
        ObjectNode tree = exampleWithPhotoStep();
        photoStep(tree).put("answer", "a");

        Issue issue = single(READER.read(utf8(tree)));

        assertEquals("/modules/0/steps/4", issue.path().toString());
        assertTrue(issue.message().contains("'answer'"), issue.message());
    }

    @Test
    void aPhotoPromptLongerThan2000CharactersIsRejected() {
        ObjectNode tree = exampleWithPhotoStep();
        photoStep(tree).put("prompt", "x".repeat(2_001));

        assertEquals("/modules/0/steps/4/prompt", single(READER.read(utf8(tree))).path().toString());
    }

    @Test
    void anUnknownExerciseTypeIsStillRejected() {
        ObjectNode tree = exampleWithPhotoStep();
        photoStep(tree).put("type", "video");

        assertEquals(CourseIssueCode.SCHEMA_VIOLATION, single(READER.read(utf8(tree))).code());
    }

    @Test
    void storedContentWithAPhotoStepReadsBackToTheSameCourse() {
        CourseReadResult.Parsed first = assertInstanceOf(CourseReadResult.Parsed.class,
                READER.read(utf8(exampleWithPhotoStep())));

        assertEquals(first.course(), parsed(first.content().bytes()));
    }
}
