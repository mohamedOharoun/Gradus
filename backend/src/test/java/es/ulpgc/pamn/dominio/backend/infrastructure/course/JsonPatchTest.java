package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonPatchTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static JsonNode json(String text) throws Exception {
        return MAPPER.readTree(text.replace('\'', '"'));
    }

    @Test
    void addsToObjectsAndArraysIncludingAtTheEnd() throws Exception {
        JsonNode result = JsonPatch.apply(json("{'a': [1, 3]}"), json("""
                [{'op': 'add', 'path': '/b', 'value': true},
                 {'op': 'add', 'path': '/a/1', 'value': 2},
                 {'op': 'add', 'path': '/a/-', 'value': 4}]"""));

        assertEquals(json("{'a': [1, 2, 3, 4], 'b': true}"), result);
    }

    @Test
    void replacesAndRemoves() throws Exception {
        JsonNode result = JsonPatch.apply(json("{'a': [1, 2, 3], 'b': 'x', 'c': 0}"), json("""
                [{'op': 'replace', 'path': '/b', 'value': 'y'},
                 {'op': 'replace', 'path': '/a/0', 'value': 9},
                 {'op': 'remove', 'path': '/a/2'},
                 {'op': 'remove', 'path': '/c'}]"""));

        assertEquals(json("{'a': [9, 2], 'b': 'y'}"), result);
    }

    @Test
    void copiesAnIndependentValue() throws Exception {
        JsonNode result = JsonPatch.apply(json("{'m': [{'id': 'a'}]}"), json("""
                [{'op': 'copy', 'from': '/m/0', 'path': '/m/-'},
                 {'op': 'replace', 'path': '/m/1/id', 'value': 'b'}]"""));

        assertEquals(json("{'m': [{'id': 'a'}, {'id': 'b'}]}"), result);
    }

    @Test
    void leavesTheOriginalDocumentUntouched() throws Exception {
        JsonNode original = json("{'a': 1}");

        JsonPatch.apply(original, json("[{'op': 'replace', 'path': '/a', 'value': 2}]"));

        assertEquals(json("{'a': 1}"), original);
    }

    @Test
    void aPatchThatDoesNotFitTheDocumentIsAnError() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> JsonPatch.apply(json("{'a': 1}"), json("[{'op': 'replace', 'path': '/nope', 'value': 2}]")));
        assertThrows(IllegalArgumentException.class,
                () -> JsonPatch.apply(json("{'a': 1}"), json("[{'op': 'move', 'from': '/a', 'path': '/b'}]")));
    }
}
