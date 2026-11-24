package es.ulpgc.pamn.dominio.backend.web.progress;

import com.fasterxml.jackson.databind.JsonNode;
import es.ulpgc.pamn.dominio.backend.domain.progress.EnrollmentDraft;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModuleDraft;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Funciones puras: del árbol JSON a los borradores en una sola pasada. Cada paso devuelve un
 * {@link Optional}: vacío = «no tiene la forma del contrato». Solo se llama a un constructor de borrador
 * cuando ya se ha comprobado que todo está presente, así que nunca hay un {@code NullPointerException}.
 * Los campos que no se piden (como {@code userId}) ni se miran: Tolerant Reader.
 */
final class SyncRequestJson {

    private SyncRequestJson() {
    }

    static Optional<List<EnrollmentDraft>> drafts(JsonNode root) {
        if (!root.isObject()) {
            return Optional.empty();
        }
        return array(root, "enrollments").flatMap(nodes -> allOrNothing(nodes, SyncRequestJson::enrollment));
    }

    private static Optional<EnrollmentDraft> enrollment(JsonNode node) {
        if (!node.isObject()) {
            return Optional.empty();
        }
        return text(node, "courseId").flatMap(courseId ->
                instant(node, "enrolledAt").flatMap(enrolledAt ->
                        array(node, "mastered")
                                .flatMap(nodes -> allOrNothing(nodes, SyncRequestJson::module))
                                .map(mastered -> new EnrollmentDraft(courseId, enrolledAt, mastered))));
    }

    private static Optional<MasteredModuleDraft> module(JsonNode node) {
        if (!node.isObject()) {
            return Optional.empty();
        }
        return text(node, "moduleId").flatMap(moduleId ->
                instant(node, "masteredAt").map(masteredAt -> new MasteredModuleDraft(moduleId, masteredAt)));
    }

    /** El campo existe y es una lista (no {@code null}, ni objeto, ni texto). */
    private static Optional<JsonNode> array(JsonNode parent, String field) {
        return Optional.ofNullable(parent.get(field)).filter(JsonNode::isArray);
    }

    /** El campo existe y es un texto de JSON: un número o un {@code null} no se convierten en texto. */
    private static Optional<String> text(JsonNode parent, String field) {
        return Optional.ofNullable(parent.get(field)).filter(JsonNode::isTextual).map(JsonNode::textValue);
    }

    private static Optional<Instant> instant(JsonNode parent, String field) {
        return text(parent, field).flatMap(IsoInstants::parse);
    }

    /** Aplica {@code mapper} a cada elemento, en orden; si uno falla, falla todo. */
    private static <T> Optional<List<T>> allOrNothing(JsonNode array, Function<JsonNode, Optional<T>> mapper) {
        List<T> result = new ArrayList<>(array.size());
        for (JsonNode element : array) {
            Optional<T> mapped = mapper.apply(element);
            if (mapped.isEmpty()) {
                return Optional.empty();
            }
            result.add(mapped.get());
        }
        return Optional.of(result);
    }
}
