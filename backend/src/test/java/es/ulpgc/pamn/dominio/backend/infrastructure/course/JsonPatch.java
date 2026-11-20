package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON Patch (RFC 6902) mínimo para los fixtures: {@code add}, {@code replace}, {@code remove} y
 * {@code copy}. Solo para tests: devuelve un documento nuevo y nunca modifica el original.
 */
final class JsonPatch {

    private static final String END_OF_ARRAY = "-";

    private JsonPatch() {
    }

    static JsonNode apply(JsonNode document, JsonNode patch) {
        JsonNode result = document.deepCopy();
        for (JsonNode operation : patch) {
            applyOne(result, operation);
        }
        return result;
    }

    private static void applyOne(JsonNode document, JsonNode operation) {
        String op = operation.path("op").asText();
        JsonPointer path = JsonPointer.compile(operation.path("path").asText());
        switch (op) {
            case "add" -> add(document, path, operation.get("value").deepCopy());
            case "replace" -> {
                requireExisting(document, path);
                remove(document, path);
                add(document, path, operation.get("value").deepCopy());
            }
            case "remove" -> {
                requireExisting(document, path);
                remove(document, path);
            }
            case "copy" -> {
                JsonPointer from = JsonPointer.compile(operation.path("from").asText());
                requireExisting(document, from);
                add(document, path, document.at(from).deepCopy());
            }
            default -> throw new IllegalArgumentException("Operación no soportada en los fixtures: " + op);
        }
    }

    private static void add(JsonNode document, JsonPointer path, JsonNode value) {
        JsonNode parent = document.at(path.head());
        String last = path.last().getMatchingProperty();
        switch (parent) {
            case ObjectNode object -> object.set(last, value);
            case ArrayNode array when END_OF_ARRAY.equals(last) -> array.add(value);
            case ArrayNode array -> array.insert(Integer.parseInt(last), value);
            default -> throw new IllegalArgumentException("No se puede añadir en " + path);
        }
    }

    private static void remove(JsonNode document, JsonPointer path) {
        JsonNode parent = document.at(path.head());
        String last = path.last().getMatchingProperty();
        switch (parent) {
            case ObjectNode object -> object.remove(last);
            case ArrayNode array -> array.remove(Integer.parseInt(last));
            default -> throw new IllegalArgumentException("No se puede quitar " + path);
        }
    }

    private static void requireExisting(JsonNode document, JsonPointer path) {
        if (document.at(path).isMissingNode()) {
            throw new IllegalArgumentException("El parche apunta a algo que no existe: " + path);
        }
    }
}
