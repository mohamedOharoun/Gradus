package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.JsonNode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Lesson;
import es.ulpgc.pamn.dominio.backend.domain.course.Mastery;
import es.ulpgc.pamn.dominio.backend.domain.course.Matching;
import es.ulpgc.pamn.dominio.backend.domain.course.MatchingPair;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;
import es.ulpgc.pamn.dominio.backend.domain.course.MultipleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.Option;
import es.ulpgc.pamn.dominio.backend.domain.course.PhotoExercise;
import es.ulpgc.pamn.dominio.backend.domain.course.SingleChoice;
import es.ulpgc.pamn.dominio.backend.domain.course.Step;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Árbol JSON → records del dominio, a mano y sin anotaciones de Jackson en el dominio (§3.1). Solo se
 * llama tras pasar el schema, así que la forma está garantizada y no hace falta comprobar nada.
 */
final class CourseNodeMapper {

    private static final String LESSON = "lesson";

    /** Factory por tipo: cada {@code type} del fichero sabe construir su record. */
    private static final Map<String, Function<JsonNode, Exercise>> EXERCISES = Map.of(
            "singleChoice", node -> new SingleChoice(text(node, "id"), text(node, "prompt"),
                    list(node.get("options"), CourseNodeMapper::option), text(node, "answer"), text(node, "explanation")),
            "multipleChoice", node -> new MultipleChoice(text(node, "id"), text(node, "prompt"),
                    list(node.get("options"), CourseNodeMapper::option), strings(node.get("correct")),
                    text(node, "explanation")),
            "matching", node -> new Matching(text(node, "id"), text(node, "prompt"),
                    list(node.get("pairs"), CourseNodeMapper::pair), text(node, "explanation")),
            "photo", node -> new PhotoExercise(text(node, "id"), text(node, "prompt"), text(node, "explanation")));

    private CourseNodeMapper() {
    }

    static CourseDocument course(JsonNode node) {
        return new CourseDocument(
                node.get("formatVersion").asInt(),
                text(node, "title"),
                text(node, "description"),
                text(node, "language"),
                strings(node.get("tags")),
                mastery(node.get("defaults").get("mastery")),
                list(node.get("modules"), CourseNodeMapper::module));
    }

    private static Module module(JsonNode node) {
        return new Module(
                text(node, "id"),
                text(node, "title"),
                strings(node.get("requires")),
                Optional.ofNullable(node.get("mastery")).map(CourseNodeMapper::mastery),
                list(node.get("steps"), CourseNodeMapper::step),
                list(node.get("review"), CourseNodeMapper::exercise));
    }

    private static Mastery mastery(JsonNode node) {
        return new Mastery(node.get("questions").asInt(), node.get("minCorrect").asInt());
    }

    private static Step step(JsonNode node) {
        return LESSON.equals(text(node, "type")) ? new Lesson(text(node, "id"), text(node, "text")) : exercise(node);
    }

    private static Exercise exercise(JsonNode node) {
        String type = text(node, "type");
        return Optional.ofNullable(EXERCISES.get(type))
                .orElseThrow(() -> new IllegalStateException("Tipo de ejercicio desconocido tras pasar el schema: " + type))
                .apply(node);
    }

    private static Option option(JsonNode node) {
        return new Option(text(node, "id"), text(node, "text"));
    }

    private static MatchingPair pair(JsonNode node) {
        return new MatchingPair(text(node, "left"), text(node, "right"));
    }

    private static String text(JsonNode node, String field) {
        return node.get(field).asText();
    }

    private static List<String> strings(JsonNode array) {
        return list(array, JsonNode::asText);
    }

    private static <T> List<T> list(JsonNode array, Function<JsonNode, T> mapper) {
        return StreamSupport.stream(array.spliterator(), false).map(mapper).toList();
    }
}
